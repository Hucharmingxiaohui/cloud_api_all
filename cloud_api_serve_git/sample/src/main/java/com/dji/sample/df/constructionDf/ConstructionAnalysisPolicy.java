package com.dji.sample.df.constructionDf;

import java.util.*;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;

/** Configuration only. No algorithm invocation or temperature evaluation. */
public final class ConstructionAnalysisPolicy {
    public static final List<String> CODES=List.of("missing_helmet","phone_use","smoking","person_fallen","missing_safety_clothing","person_count","fire_smoke","adverse_weather","animal_intrusion","barrier_crossing","person_under_lifting_load","missing_hook_latch");
    public static final List<String> NAMES=List.of("未佩戴安全帽","违规玩手机","工作场所吸烟","人员倒地","未穿安全服","人员数量统计","火灾烟雾","恶劣天气","动物入侵","人员违规跨栏","吊物下方站人","防脱钩缺失");
    public static List<Map<String,String>> catalog(){List<Map<String,String>> out=new ArrayList<>();for(int i=0;i<CODES.size();i++)out.add(Map.of("code",CODES.get(i),"name",NAMES.get(i)));return out;}
    public static Map<String,Object> defaults(){Map<String,Object> ir=new LinkedHashMap<>();ir.put("enabled",false);ir.put("max_temperature_c",null);ir.put("min_hotspot_pixels",1);return Map.of("visible_checks",CODES,"infrared",ir);}
    public static Map<String,Object> validate(Object value){
        if(!(value instanceof Map))throw bad("检测配置格式无效");Map<?,?> p=(Map<?,?>)value;
        if(!p.keySet().equals(Set.of("visible_checks","infrared")))throw bad("检测配置字段不完整或存在未知字段");
        if(!(p.get("visible_checks") instanceof List))throw bad("请选择可见光检测项目");
        Set<String> selected=new HashSet<>();for(Object item:(List<?>)p.get("visible_checks"))if(!(item instanceof String)||!CODES.contains(item)||!selected.add((String)item))throw bad("可见光检测项目无效或重复");
        if(!(p.get("infrared") instanceof Map))throw bad("红外检测配置缺失");Map<?,?> raw=(Map<?,?>)p.get("infrared");
        if(!raw.keySet().equals(Set.of("enabled","max_temperature_c","min_hotspot_pixels"))||!(raw.get("enabled") instanceof Boolean))throw bad("红外检测配置格式无效");
        boolean enabled=Boolean.TRUE.equals(raw.get("enabled"));Object threshold=raw.get("max_temperature_c");
        if(threshold!=null||enabled)bounded(threshold,-50,1000,"温度阈值须为-50至1000℃的数值");
        double pixels=bounded(raw.get("min_hotspot_pixels"),1,10000000,"高温区域最小像素数须为1至10000000的整数");
        if(pixels!=Math.rint(pixels))throw bad("高温区域最小像素数必须为整数");
        Map<String,Object> ir=new LinkedHashMap<>();ir.put("enabled",enabled);ir.put("max_temperature_c",threshold==null?null:((Number)threshold).doubleValue());ir.put("min_hotspot_pixels",(int)pixels);
        List<String> checks=new ArrayList<>();for(String code:CODES)if(selected.contains(code))checks.add(code);
        return Map.of("visible_checks",checks,"infrared",ir);
    }
    private static double bounded(Object value,double min,double max,String message){if(!(value instanceof Number))throw bad(message);double n=((Number)value).doubleValue();if(!Double.isFinite(n)||n<min||n>max)throw bad(message);return n;}
    private static RuleException bad(String text){return new RuleException(400,text);}
}
