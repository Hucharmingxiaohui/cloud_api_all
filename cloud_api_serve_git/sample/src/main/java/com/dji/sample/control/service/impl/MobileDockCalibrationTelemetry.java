package com.dji.sample.control.service.impl;

import com.dji.sdk.mqtt.osd.TopicOsdRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class MobileDockCalibrationTelemetry {
    private static final List<String> FIELDS=List.of("self_converge_coordinate","position_state","mode_code","drone_in_dock","emergency_stop_state","home_position_is_valid","deployment_mode");
    private final RedisTemplate<String,Object> redis;
    public MobileDockCalibrationTelemetry(RedisTemplate<String,Object> redis) {this.redis=redis;}
    public void record(String sn,Object value) {
        if(!(value instanceof TopicOsdRequest)) return;
        TopicOsdRequest<?> request=(TopicOsdRequest<?>)value;
        if(!sn.equals(request.getGateway())) return;
        Map<String,Object> data=MobileDockCalibrationRules.map(request.getData());
        long now=System.currentTimeMillis();
        if(request.getTimestamp()==null) return;
        for(String name:FIELDS) if(data.containsKey(name)) {
            // Replace whole field samples; never refresh an absent field using another OSD packet.
            Map<String,Object> sample=new LinkedHashMap<>();
            sample.put("value",data.get(name));sample.put("timestamp",request.getTimestamp());sample.put("received_at",now);
            String key="mobile-calibration:osd:"+sn+":"+name;
            Map<String,Object> previous=MobileDockCalibrationRules.map(redis.opsForValue().get(key));
            Object timestamp=previous.get("timestamp");
            if(timestamp instanceof Number&&((Number)timestamp).longValue()>request.getTimestamp()) continue;
            redis.opsForValue().set(key,sample,30,TimeUnit.SECONDS);
        }
    }
    public Map<String,Object> fields(String sn) {
        Map<String,Object> result=new LinkedHashMap<>();
        for(String name:FIELDS) result.put(name,redis.opsForValue().get("mobile-calibration:osd:"+sn+":"+name));
        return result;
    }
}
