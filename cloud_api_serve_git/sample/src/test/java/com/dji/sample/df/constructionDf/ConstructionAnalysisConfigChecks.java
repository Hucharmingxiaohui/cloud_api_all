package com.dji.sample.df.constructionDf;

import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.cloudapi.wayline.GetWaylineListResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.DigestUtils;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;

@SuppressWarnings("unchecked")
public class ConstructionAnalysisConfigChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    interface Work{void run() throws Exception;}
    static void reject(Work work){try{work.run();}catch(RuleException e){return;}catch(Exception e){throw new RuntimeException(e);}throw new AssertionError("expected rejection");}
    static Map<String,Object> policy(boolean thermal,Object threshold,String...visible){Map<String,Object> ir=new LinkedHashMap<>();ir.put("enabled",thermal);ir.put("max_temperature_c",threshold);ir.put("min_hotspot_pixels",1);return Map.of("visible_checks",List.of(visible),"infrared",ir);}
    static Map<String,Object> body(Map<String,Object> view,String target,Object policy,int point){Map<String,Object> body=new LinkedHashMap<>();body.put("target",target);body.put("policy",policy);body.put("expected_revision",view.get("revision"));body.put("file_sign",view.get("file_sign"));body.put("route_version",view.get("route_version"));body.put("waypoint_index",point);return body;}
    static Map<String,Object> point(Map<String,Object> view,int i){return ((List<Map<String,Object>>)view.get("points")).get(i-1);}
    public static void main(String[] args)throws Exception{
        Map<String,Map<String,Object>> database=new HashMap<>();
        JdbcTemplate db=new JdbcTemplate(){
            @Override public List<Map<String,Object>> queryForList(String sql,Object...args){Map<String,Object> row=database.get(args[0]);return row==null?List.of():List.of(new LinkedHashMap<>(row));}
            @Override public int update(String sql,Object...args){if(sql.startsWith("INSERT IGNORE")){database.putIfAbsent((String)args[0],new LinkedHashMap<>(Map.of("revision",0,"config_json",args[1])));return 1;}database.put((String)args[2],new LinkedHashMap<>(Map.of("revision",args[0],"config_json",args[1])));return 1;}
        };
        ConstructionAreaService areas=new ConstructionAreaService(null,null){@Override public Map<String,Object> detail(String w,String id){if(!w.equals("w")||!List.of("a","b").contains(id))throw new RuleException(404,"scope");return Map.of("id",id,"name",id);}};
        Path kmz=Files.createTempFile("analysis-config-",".kmz");byte[] bytes=ConstructionOverallChecks.fixture("reachPoint");Files.write(kmz,bytes);String[] sign={DigestUtils.md5DigestAsHex(bytes)};
        Map<String,Object> route=new LinkedHashMap<>(Map.of("overall_id","overall","version",1,"wayline_id","file","file_sign",sign[0],"name","route","points",List.of(ConstructionOverallChecks.point(1,"WP1"),ConstructionOverallChecks.point(2,"WP2"),ConstructionOverallChecks.point(3,"WP3")),"bindings",List.of(Map.of("area_id","a","area_name","a","waypoints",List.of(1)),Map.of("area_id","b","area_name","b","waypoints",List.of(2)))));
        ConstructionOverallService overall=new ConstructionOverallService(null,null,null,null,null){@Override public Map<String,Object> stored(String w,String id){if(!w.equals("w")||!id.equals("overall"))throw new RuleException(404,"scope");return route;}};
        IWaylineFileService files=(IWaylineFileService)Proxy.newProxyInstance(ConstructionAnalysisConfigChecks.class.getClassLoader(),new Class[]{IWaylineFileService.class},(p,m,a)->{if(m.getName().equals("getWaylineByWaylineId"))return Optional.of(new GetWaylineListResponse().setSign(sign[0]));if(m.getName().equals("getObjectUrl"))return kmz.toUri().toURL();throw new UnsupportedOperationException(m.getName());});
        ConstructionWaylineService waylines=new ConstructionWaylineService(null,null,null,null,null){@Override public Map<String,Object> detail(String w,String a,String f){if(!w.equals("w")||!a.equals("a")||!f.equals("file"))throw new RuleException(404,"scope");return Map.of("name","single","area_version",1);}};
        ConstructionAnalysisConfigService service=new ConstructionAnalysisConfigService(db,areas,waylines,overall,files);
        try {
            Map<String,Object> initial=service.get("w",null,null,null);
            check(((List<?>)((Map<?,?>)initial.get("default_policy")).get("visible_checks")).size()==12,"defaults preserve twelve visible checks");
            reject(()->ConstructionAnalysisPolicy.validate(policy(true,null,"fire_smoke")));
            reject(()->ConstructionAnalysisPolicy.validate(policy(true,Double.NaN,"fire_smoke")));
            reject(()->ConstructionAnalysisPolicy.validate(policy(false,null,"unknown")));
            reject(()->ConstructionAnalysisPolicy.validate(policy(false,null,"fire_smoke","fire_smoke")));
            ConstructionAnalysisPolicy.validate(policy(true,0,"fire_smoke"));
            service.save("w",null,null,null,body(initial,"default",policy(false,null,"person_count"),0));
            reject(()->service.save("w",null,null,null,body(initial,"default",policy(false,null),0)));
            Map<String,Object> a=service.get("w","a",null,null);
            check(((Map<?,?>)a.get("area")).get("override")==null,"unset area inherits");
            service.save("w","a",null,null,body(a,"area",policy(true,80,"fire_smoke"),0));
            Map<String,Object> v=service.get("w",null,null,"overall");
            check(point(v,1).get("source").equals("area")&&point(v,2).get("source").equals("default"),"per-workface inheritance");
            Map<String,Object> unbound=body(v,"waypoint",policy(false,null),3);reject(()->service.save("w",null,null,"overall",unbound));
            v=service.save("w",null,null,"overall",body(v,"waypoint",policy(false,null),1));
            check(point(v,1).get("source").equals("waypoint")&&((List<?>)((Map<?,?>)point(v,1).get("effective")).get("visible_checks")).isEmpty(),"explicit empty does not inherit");
            Map<String,Object> frozen=service.freeze("w",null,"file",route);String before=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(frozen);
            a=service.get("w","a",null,null);service.save("w","a",null,null,body(a,"area",policy(true,95,"missing_helmet"),0));
            v=service.get("w",null,null,"overall");check(point(v,1).get("source").equals("waypoint"),"parent updates do not overwrite independent points");
            check(before.equals(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(frozen)),"frozen plan data does not follow live edits");
            v=service.save("w",null,null,"overall",body(v,"waypoint",null,1));check(point(v,1).get("source").equals("area"),"restore parent inheritance");
            Map<String,Object> single=service.get("w","a","file",null);check(!((List<?>)single.get("points")).isEmpty(),"single-area route uses actual KMZ indices");
            single=service.save("w","a","file",null,body(single,"waypoint",policy(false,null,"person_count"),1));check(point(single,1).get("source").equals("waypoint"),"single-area independent setting persists");
            check(point(service.get("w",null,null,"overall"),1).get("source").equals("area"),"single and overall contexts isolated");
            reject(()->service.get("foreign","a",null,null));reject(()->service.get("w",null,null,"other"));
            check(((Number)service.get("foreign",null,null,null).get("revision")).intValue()==0,"workspace default isolated");
            Map<String,Object> stale=body(v,"waypoint",policy(false,null),1);route.put("version",2);reject(()->service.save("w",null,null,"overall",stale));
            sign[0]="new-file-sign";route.put("file_sign",sign[0]);check(point(service.get("w",null,null,"overall"),1).get("override")==null,"changed file version does not reuse old waypoint overrides");
            System.out.println("Analysis configuration checks passed: 3-level inheritance, explicit empty, infrared validation, stale revision, workspace/file isolation, real single-route indices and immutable snapshots. No algorithm calls.");
        }finally{Files.deleteIfExists(kmz);}
    }
}
