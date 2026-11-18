package com.dji.sample.df.constructionDf;

import java.util.*;
import java.lang.reflect.*;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.Point;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.cloudapi.wayline.GetWaylineListResponse;
import org.springframework.jdbc.core.JdbcTemplate;

public class ConstructionLinePatrolChecks {
    static void check(boolean ok,String text) { if(!ok) throw new AssertionError(text); }
    static void set(Object object,String field,Object value) throws Exception { Field f=object.getClass().getDeclaredField(field); f.setAccessible(true); f.set(object,value); }
    static Object call(Object target,String name,Class<?>[] types,Object... args) throws Exception {
        Method method=target.getClass().getDeclaredMethod(name,types); method.setAccessible(true);
        try { return method.invoke(target,args); } catch(InvocationTargetException e) { throw (Exception)e.getCause(); }
    }
    interface Checked { void run() throws Exception; }
    static void reject(Checked task) throws Exception { try { task.run(); } catch(RuleException expected) { return; } throw new AssertionError("Expected rule rejection"); }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        String key=ConstructionLineTarget.key("shared-id");
        check(key.equals("line:shared-id"),"line namespace avoids area collision");
        check(!ConstructionLineTarget.isLine("shared-id"),"legacy IDs remain areas");
        reject(()->ConstructionLineTarget.key("line:shared-id"));
        List<Point> linePoints=List.of(new Point(120,36),new Point(120.001,36));
        Map<String,Object> line=new LinkedHashMap<>(Map.of("line_id","shared-id","name","test-line","type","power","points",linePoints,"enabled",true,"archived",false,"version",1));
        ConstructionLineService lines=new ConstructionLineService(null,null) {
            @Override public Map<String,Object> detail(String w,String id) { if(!"w".equals(w)||!"shared-id".equals(id)) throw new RuleException(404,"not found"); return line; }
        };
        List<String> queries=new ArrayList<>();
        JdbcTemplate db=new JdbcTemplate() {
            @Override public List<Map<String,Object>> queryForList(String sql,Object... a) {
                queries.add(sql);
                if(sql.contains("construction_line")) return "w".equals(a[0])?List.of(Map.of("id",a[1])):List.of();
                if(sql.contains("construction_analysis_config")) return List.of();
                throw new AssertionError(sql);
            }
        };
        ConstructionLineTarget targets=new ConstructionLineTarget(lines,db);
        Map<String,Object> lineView=targets.lock("w",key);
        check(lineView.get("id").equals(key),"snapshot carries typed grouping ID");
        check(((List<?>)lineView.get("boundary")).get(0) instanceof Map,"internal Point objects converted for shared coverage");
        check(queries.stream().noneMatch(s->s.contains("construction_area")),"line lock never touches area table");
        check(line.get("name").equals("test-line"),"adapter does not mutate original line");
        reject(()->targets.detail("other",key));
        Map<String,Object> polygon=new LinkedHashMap<>(Map.of("id","shared-id","name","area","version",1,"enabled",true,"archived",false,"boundary",List.of(Map.of("lng",120.0,"lat",36.0),Map.of("lng",120.002,"lat",36.0),Map.of("lng",120.0,"lat",36.002))));
        int[] areaCalls={0};
        ConstructionAreaService areas=new ConstructionAreaService(null,null) {
            @Override public Map<String,Object> detail(String w,String id) { areaCalls[0]++; check(id.equals("shared-id"),"line not passed to legacy area lookup"); return polygon; }
        };
        ConstructionWaylineService waylines=new ConstructionWaylineService(areas,db,null,null,null);
        set(waylines,"lineTargets",targets);
        ConstructionKmz kmz=new ConstructionKmz(); kmz.points.addAll(List.of(new Point(121,36),new Point(121.001,36)));
        call(waylines,"validateGeometry",new Class[]{ConstructionKmz.class,Map.class},kmz,lineView);
        reject(()->call(waylines,"validateGeometry",new Class[]{ConstructionKmz.class,Map.class},kmz,polygon));
        reject(()->ConstructionLineTarget.validateWaypoints(List.of(new Point(Double.NaN,36),new Point(121,36))));
        ConstructionOverallService overall=new ConstructionOverallService(db,areas,null,null,null);
        set(overall,"lineTargets",targets);
        Map<String,Object> binding=new LinkedHashMap<>(Map.of("area_id",key,"area_version",1,"waypoints",List.of(1)));
        Map<String,Object> center=Map.of("lng",120.0,"lat",36.0,"radius_m",500);
        call(overall,"checkArea",new Class[]{String.class,Map.class,Map.class},"w",binding,center);
        check(binding.get("target_type").equals("line"),"line type frozen");
        check(areaCalls[0]==0,"line coverage independent of legacy areas");
        Map<String,Object> oldBinding=new LinkedHashMap<>(Map.of("area_id","shared-id","area_version",1,"waypoints",List.of(2)));
        call(overall,"checkArea",new Class[]{String.class,Map.class,Map.class},"w",oldBinding,center);
        check(areaCalls[0]==1,"area still uses original service");
        reject(()->call(overall,"checkArea",new Class[]{String.class,Map.class,Map.class},"w",binding,Map.of("lng",121.0,"lat",36.0,"radius_m",500)));
        line.put("version",2); reject(()->call(overall,"checkArea",new Class[]{String.class,Map.class,Map.class},"w",binding,center)); line.put("version",1);
        line.put("archived",true); reject(()->call(overall,"checkArea",new Class[]{String.class,Map.class,Map.class},"w",binding,center)); line.put("archived",false);
        List<Map<String,Object>> points=List.of(Map.of("index",1,"lng",120.0,"lat",36.0,"photo_suffixes",List.of("p1")),Map.of("index",2,"lng",120.001,"lat",36.0,"photo_suffixes",List.of("p2")));
        List<Map<String,Object>> bindings=ConstructionOverallService.bindings(List.of(binding,oldBinding),points);
        check(bindings.size()==2,"same raw ID can be bound as distinct area and line");
        Map<String,Object> duplicate=new LinkedHashMap<>(oldBinding); duplicate.put("waypoints",List.of(1));
        reject(()->ConstructionOverallService.bindings(List.of(binding,duplicate),points));
        Map<String,Object> route=new LinkedHashMap<>(Map.of("overall_id","route","wayline_id","file","file_sign","sign","version",1,"name","mixed","bindings",List.of(binding,oldBinding),"points",points));
        ConstructionOverallService frozenRoutes=new ConstructionOverallService(db,areas,null,null,null) {
            @Override public Map<String,Object> stored(String w,String id) { return route; }
        };
        IWaylineFileService files=(IWaylineFileService)Proxy.newProxyInstance(IWaylineFileService.class.getClassLoader(),new Class[]{IWaylineFileService.class},(o,m,a)-> {
            if(m.getName().equals("getWaylineByWaylineId")) return Optional.of(new GetWaylineListResponse().setSign("sign"));
            throw new AssertionError(m.getName());
        });
        ConstructionAnalysisConfigService config=new ConstructionAnalysisConfigService(db,areas,waylines,frozenRoutes,files); set(config,"lineTargets",targets);
        Map<String,Object> state=config.get("w",key,null,null);
        check(state.get("area_id").equals(key),"line config uses distinct key");
        Map<String,Object> frozen=config.freeze("w",null,"file",route);
        List<Map<String,Object>> configured=(List<Map<String,Object>>)frozen.get("points");
        check(configured.get(0).get("area_id").equals(key),"line photo ownership frozen");
        check(configured.get(1).get("area_id").equals("shared-id"),"area photo ownership retained");
        check(configured.get(0).containsKey("effective"),"three-level effective policy included");
        check(((List<?>)binding.get("waypoints")).size()==1,"one waypoint one owner");
        System.out.println("Line patrol checks passed: namespace, workspace, polygon isolation, coverage/version/archive, mixed bindings and frozen analysis ownership.");
    }
}
