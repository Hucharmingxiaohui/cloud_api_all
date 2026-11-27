package com.dji.sample.control.service.impl;

import java.util.*;

/** API 1 Dock 3: manual calibration using fresh, successfully converged OSD coordinates. */
public final class MobileDockCalibrationRules {
    private MobileDockCalibrationRules() {}
    public static Map<String,Object> map(Object value) {
        return value instanceof Map ? (Map<String,Object>) value : Collections.emptyMap();
    }
    public static boolean code(Object value,int expected) {
        return value instanceof Number && ((Number)value).doubleValue()==expected;
    }
    public static Object fresh(Map<String,Object> fields,String name,long now) {
        Map<String,Object> sample=map(fields.get(name));
        Object time=sample.get("received_at"), source=sample.get("timestamp");
        if(!(time instanceof Number)||!(source instanceof Number)
                ||now-((Number)time).longValue()<0||now-((Number)time).longValue()>15000
                ||now-((Number)source).longValue() < -5000||now-((Number)source).longValue()>15000)
            throw new IllegalArgumentException("机场上报缺失、过期或时钟异常："+name+"，请等待最新 OSD");
        return sample.get("value");
    }
    public static Map<String,Object> coordinates(Map<String,Object> fields,long now) {
        Map<String,Object> pos=map(fresh(fields,"position_state",now));
        if(!code(pos.get("is_fixed"),2)) throw new IllegalArgumentException("机场位置尚未收敛成功，请等待后再标定");
        Object mode=fresh(fields,"mode_code",now);
        if(!code(mode,0)&&!code(mode,5)) throw new IllegalArgumentException("机场须处于空闲或待标定状态，请先结束作业或调试");
        Object inDock=fresh(fields,"drone_in_dock",now);
        if(!Boolean.TRUE.equals(inDock)&&!code(inDock,1)) throw new IllegalArgumentException("请确认飞行器已回舱，再进行标定");
        Object emergency=fresh(fields,"emergency_stop_state",now);
        if(!Boolean.FALSE.equals(emergency)&&!code(emergency,0)) throw new IllegalArgumentException("机场急停状态异常，不能标定");
        Map<String,Object> coordinate=map(fresh(fields,"self_converge_coordinate",now));
        double lon=number(coordinate.get("longitude")),lat=number(coordinate.get("latitude")),height=number(coordinate.get("height"));
        if(Math.abs(lon)>180||Math.abs(lat)>90||(lon==0&&lat==0)||height==32767)
            throw new IllegalArgumentException("机场自收敛坐标无效，请等待定位恢复");
        return Map.of("longitude",lon,"latitude",lat,"height",height);
    }
    private static double number(Object value) {
        if(!(value instanceof Number)||!Double.isFinite(((Number)value).doubleValue()))
            throw new IllegalArgumentException("机场自收敛坐标不完整，不能标定");
        return ((Number)value).doubleValue();
    }
    public static Map<String,Object> payload(String sn,Map<String,Object> coordinate) {
        return Map.of("devices",List.of(Map.of("sn",sn,"type",1,"module","3","data",coordinate)));
    }
    public static Map<String,Object> result(String sn,Map<String,Object> data) {
        Map<String,Object> output=map(data.get("output"));
        if(!code(data.get("result"),0)) return Map.of("state","failed","message","标定失败，返回码："+data.get("result"));
        Object devices=map(output.get("ext")).get("devices");
        if(devices instanceof List) for(Object item:(List<?>)devices) {
            Map<String,Object> d=map(item);
            if(sn.equals(d.get("sn"))&&"3".equals(String.valueOf(d.get("module")))) {
                if("ok".equals(output.get("status"))&&"ok".equals(d.get("status"))&&code(d.get("result"),0))
                    return Map.of("state","ok","message","设备报告标定成功，请确认最新位置状态后执行任务");
                return Map.of("state","failed","message","标定未成功，设备状态："+d.get("status")+"，返回码："+d.get("result"));
            }
        }
        return Map.of("state","failed","message","标定结果缺少当前机场明细，不能确认成功");
    }
}
