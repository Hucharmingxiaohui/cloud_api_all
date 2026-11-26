package com.dji.sample.df.constructionDf;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.*;
public class ConstructionTowerChecks {
    static void check(boolean ok){if(!ok)throw new AssertionError();}
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        Map<String,Map<String,Object>> towers=new HashMap<>();
        Map<String,List<Map<String,Object>>> lineRows=new HashMap<>();
        JdbcTemplate db=new JdbcTemplate(){
            @Override public List<Map<String,Object>> queryForList(String sql,Object... a){
                List<Map<String,Object>> found=new ArrayList<>();
                if(sql.contains("construction_tower")) {
                    String w=String.valueOf(a[0]);
                    for(Map<String,Object> t:towers.values()) {
                        if(!w.equals(t.get("workspace_id"))) continue;
                        if(sql.contains("tower_no=?")) {
                            if(!String.valueOf(a[1]).equals(t.get("tower_no"))) continue;
                            if(sql.contains("id<>?") && String.valueOf(a[2]).equals(t.get("id"))) continue;
                        } else if(sql.contains("id=?") && !String.valueOf(a[1]).equals(t.get("id"))) {
                            continue;
                        }
                        found.add(new HashMap<>(t));
                    }
                } else if(sql.contains("construction_line")) {
                    lineRows.forEach((k,v)->found.addAll(v));
                }
                return found;
            }
            @Override public int update(String sql,Object... a){
                if(sql.startsWith("INSERT")){Map<String,Object> t=new HashMap<>();t.put("id",a[0]);t.put("workspace_id",a[1]);t.put("tower_no",a[2]);t.put("lng",a[3]);t.put("lat",a[4]);towers.put((String)a[0],t);return 1;}
                if(sql.startsWith("DELETE"))return towers.remove(String.valueOf(a[1]))==null?0:1;
                return towers.containsKey(String.valueOf(a[a.length-1]))?1:0;
            }
        };
        CustomClaim claim=new CustomClaim();claim.setWorkspaceId("w");
        HttpServletRequest r=(HttpServletRequest)Proxy.newProxyInstance(ConstructionTowerChecks.class.getClassLoader(),new Class[]{HttpServletRequest.class},(p,m,a)->claim);
        ConstructionTowerController c=new ConstructionTowerController(db);

        // 编号规范：空编号/坐标缺失拒绝（400）；重复编号冲突（409）
        check(c.create(r,new HashMap<>(Map.of("tower_no",""))).getCode()==400);
        check(c.create(r,new HashMap<>(Map.of("tower_no","A1"))).getCode()==400);
        HttpResultResponse createdResp=c.create(r,new HashMap<>(Map.of("tower_no","A1","lng",103.55,"lat",24.75)));
        check(createdResp.getCode()==0);
        Map<String,Object> created=(Map<String,Object>)createdResp.getData();
        Map<String,Object> body=new HashMap<>();body.put("tower_no","A1");body.put("lng",103.55);body.put("lat",24.75);
        check(c.create(r,body).getCode()==409);

        // 排序键：A2 < A10；档标签拼接
        check(ConstructionLineGeometry.sortKey("A2").compareTo(ConstructionLineGeometry.sortKey("A10"))<0);
        check("A2-A3".equals(ConstructionLineGeometry.spanLabel("A2","A3")));
        try {
            JdbcTemplate chainDb=new JdbcTemplate(){
                @Override public List<Map<String,Object>> queryForList(String sql,Object... a) {
                    return List.of(Map.of("id","a","tower_no","A12","lng",103.01,"lat",24.0),
                            Map.of("id","c","tower_no","C2","lng",103.02,"lat",24.0),
                            Map.of("id","b","tower_no","A11","lng",103.03,"lat",24.0));
                }
            };
            ConstructionLineService service=new ConstructionLineService(null,null,chainDb);
            java.lang.reflect.Method method=ConstructionLineService.class.getDeclaredMethod("towerChain",String.class,List.class);
            method.setAccessible(true);
            Object chain=method.invoke(service,"w",List.of("c","b","a"));
            java.lang.reflect.Field nos=chain.getClass().getDeclaredField("nos");nos.setAccessible(true);
            check(nos.get(chain).equals(List.of("C2","A11","A12")));
            java.lang.reflect.Field points=chain.getClass().getDeclaredField("points");points.setAccessible(true);
            List<ConstructionAreaGeometry.Point> ordered=(List<ConstructionAreaGeometry.Point>)points.get(chain);
            check(ordered.get(0).lng==103.02&&ordered.get(1).lng==103.03&&ordered.get(2).lng==103.01);
        } catch(ReflectiveOperationException e) {throw new AssertionError(e);}

        // 投影：航点落在折线中部偏 A2 一侧 → 最近段为 A1-A2(index0)
        List<ConstructionAreaGeometry.Point> line=List.of(new ConstructionAreaGeometry.Point(103,24),new ConstructionAreaGeometry.Point(103.02,24),new ConstructionAreaGeometry.Point(103.04,24));
        check(ConstructionLineGeometry.nearestSpanIndex(line,new ConstructionAreaGeometry.Point(103.005,24.0005))==0);
        check(ConstructionLineGeometry.nearestSpanIndex(line,new ConstructionAreaGeometry.Point(103.03,24.0008))==1);

        // 被线路引用的杆塔删除拒绝（模拟 referenced 查询命中线路）
        lineRows.put("L1",List.of(new HashMap<>(Map.of("id","L1","name","A线","tower_ids","[\""+created.get("id")+"\"]"))));
        check(c.delete(r,String.valueOf(created.get("id"))).getCode()==409);
        lineRows.remove("L1");
        check(c.delete(r,String.valueOf(created.get("id"))).getCode()==0);
        check(c.update(r,String.valueOf(created.get("id")),new HashMap<>(Map.of("tower_no","A1","lng",103.55,"lat",24.75))).getCode()==409);

        // matchPhoto：快照带 segments 时，匹配照片携带档标签
        Map<String,Object> snapshot=new HashMap<>();
        snapshot.put("points",List.of(Map.of("index",1,"photo_suffixes",List.of("航点1"))));
        snapshot.put("bindings",List.of(new HashMap<>(Map.of("area_id","line:L1","area_name","线路 · A线","waypoints",List.of(1),"segments",Map.of("1","A2-A3")))));
        Map<String,Object> matched=ConstructionOverallService.matchPhoto(snapshot,"DJI_2026_A线_航点1.jpg");
        check("MATCHED".equals(matched.get("association"))&&"A2-A3".equals(matched.get("segment")));
        snapshot.put("bindings",List.of(Map.of("area_id","line:L1","area_name","线路 · A线","waypoints",List.of(1))));
        matched=ConstructionOverallService.matchPhoto(snapshot,"DJI_2026_A线_航点1.jpg");
        check(!matched.containsKey("segment"));
        System.out.println("Tower checks passed: numbering rules, duplicate 409, referenced-delete 409, span projection/sort/label, matchPhoto segment attribution.");
    }
}
