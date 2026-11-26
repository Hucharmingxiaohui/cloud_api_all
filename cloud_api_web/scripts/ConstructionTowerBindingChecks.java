package com.dji.sample.df.constructionDf;

import java.util.*;
import java.lang.reflect.*;

/** Runs against the real binding validator; no database or aircraft required. */
public class ConstructionTowerBindingChecks {
    static ConstructionOverallService service;
    static Method method;
    static List<String> nos=List.of("A1","A2","A3");
    static Map<String,Object> binding(String id) {
        return new LinkedHashMap<>(Map.of("area_id",id,"waypoints",List.of(1,2)));
    }
    static void apply(Map<String,Object> binding,Map<String,String> selected)throws Exception {
        try { method.invoke(service,"w",selected,List.of(binding),List.of()); }
        catch(InvocationTargetException e) { throw (Exception)e.getCause(); }
    }
    static void reject(Map<String,Object> binding,Map<String,String> selected)throws Exception {
        try { apply(binding,selected); } catch(ConstructionAreaGeometry.RuleException expected) { return; }
        throw new AssertionError("Expected binding rejection");
    }
    static void check(boolean value) { if(!value)throw new AssertionError(); }
    public static void main(String[] args)throws Exception {
        service=new ConstructionOverallService(null,null,null,null,null);
        Field targets=ConstructionOverallService.class.getDeclaredField("lineTargets");targets.setAccessible(true);
        targets.set(service,new ConstructionLineTarget(null,null) {
            @Override public Map<String,Object> detail(String workspace,String id) {
                check(workspace.equals("w"));
                return Map.of("tower_nos",nos,"name","line");
            }
        });
        method=ConstructionOverallService.class.getDeclaredMethod("applySegments",String.class,Object.class,List.class,List.class);method.setAccessible(true);
        Map<String,Object> line=binding("line:L");
        apply(line,Map.of("1","A1","2","A1"));
        check(line.get("segments").equals(Map.of("1","A1","2","A1")));
        reject(binding("line:L"),Map.of("1","A1-A2","2","A2"));
        reject(binding("line:L"),Map.of("1","A99","2","A2"));
        reject(binding("line:L"),Map.of("1","A1"));
        reject(binding("line:L"),Map.of("1","A1","2","A2","3","A3"));
        Map<String,Object> area=binding("area");apply(area,Map.of());check(!area.containsKey("segments"));
        reject(binding("area"),Map.of("1","A1"));
        nos=List.of();Map<String,Object> drawn=binding("line:drawn");apply(drawn,Map.of());check(!drawn.containsKey("segments"));
        reject(binding("line:drawn"),Map.of("1","A1"));
        Map<String,Object> snapshot=Map.of("points",List.of(Map.of("index",1,"photo_suffixes",List.of("p1"))),"bindings",List.of(line));
        line.put("area_name","Line");
        check("A1".equals(ConstructionOverallService.matchPhoto(snapshot,"DJI_V_p1.jpg").get("segment")));
        line.put("segments",Map.of("1","A1-A2"));
        check("A1-A2".equals(ConstructionOverallService.matchPhoto(snapshot,"DJI_V_p1.jpg").get("segment")));
        System.out.println("PASS tower binding: explicit tower, multiple points per tower, invalid/old-span/missing rejected, areas and drawn lines preserved, new and historical photo attribution");
    }
}
