package com.dji.sample.control.service.impl;

import com.dji.sample.df.frogJumpDf.service.FrogJumpLandingRecommendationService;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.service.*;
import com.dji.sdk.cloudapi.device.DeviceTypeEnum;
import com.dji.sdk.mqtt.osd.TopicOsdRequest;
import com.dji.sdk.mqtt.events.*;
import com.dji.sdk.mqtt.services.*;
import org.springframework.data.redis.core.*;
import java.lang.reflect.Proxy;
import java.util.*;
import static com.dji.sample.control.service.impl.MobileDockCalibrationRules.*;

public class MobileDockCalibrationChecks {
    static void check(boolean value,String label) {if(!value)throw new AssertionError(label);}
    static void rejects(Runnable work) {try {work.run();throw new AssertionError("expected rejection");}catch(IllegalArgumentException expected){}}
    static class MemoryRedis extends RedisTemplate<String,Object> {
        final org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer serializer = new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer(
                new com.fasterxml.jackson.databind.ObjectMapper().activateDefaultTyping(new com.fasterxml.jackson.databind.ObjectMapper().getPolymorphicTypeValidator(),
                        com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL,com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY));
        final Map<String,Object> values=new HashMap<>();
        final ValueOperations<String,Object> ops=(ValueOperations<String,Object>)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ValueOperations.class},(p,m,a)->{
            switch(m.getName()) {
                case "get": return serializer.deserialize((byte[])values.get(a[0]));
                case "set": values.put((String)a[0],serializer.serialize(a[1]));return null;
                case "setIfAbsent": if(values.containsKey(a[0]))return false;values.put((String)a[0],serializer.serialize(a[1]));return true;
                default: throw new UnsupportedOperationException(m.getName());
            }
        });
        @Override public ValueOperations<String,Object> opsForValue(){return ops;}
        @Override public <T> T execute(RedisCallback<T> callback) {
            return callback.doInRedis((org.springframework.data.redis.connection.RedisConnection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{org.springframework.data.redis.connection.RedisConnection.class},(p,m,a)->{
                if("get".equals(m.getName()))return values.get(new String((byte[])a[0],java.nio.charset.StandardCharsets.UTF_8));
                throw new UnsupportedOperationException(m.getName());
            }));
        }
    }
    static TopicOsdRequest<Map<String,Object>> osd(Map<String,Object> data,long time) {
        return new TopicOsdRequest<Map<String,Object>>().setGateway("dock").setFrom("dock").setTimestamp(time).setData(data);
    }
    static Map<String,Object> data() {
        return new LinkedHashMap<>(Map.of("self_converge_coordinate",Map.of("longitude",103.54641941713427,"latitude",24.757457497099608,"height",2039.6725488281249),
                "position_state",Map.of("is_fixed",2),"mode_code",5,"drone_in_dock",1,"emergency_stop_state",0));
    }
    public static void main(String[] args) {
        MemoryRedis redis=new MemoryRedis();
        MobileDockCalibrationTelemetry telemetry=new MobileDockCalibrationTelemetry(redis);
        long now=System.currentTimeMillis();
        telemetry.record("dock",osd(data(),now));
        Map<String,Object> coordinate=coordinates(telemetry.fields("dock"),System.currentTimeMillis());
        check(coordinate.get("longitude").equals(103.54641941713427),"double precision preserved");
        Object old=telemetry.fields("dock").get("self_converge_coordinate");
        telemetry.record("dock",osd(Map.of("mode_code",0),now+1));
        check(old.equals(telemetry.fields("dock").get("self_converge_coordinate")),"split OSD does not refresh coordinates");
        rejects(()->coordinates(telemetry.fields("dock"),now+16000));
        telemetry.record("dock",osd(Map.of("self_converge_coordinate",Map.of("longitude",0,"latitude",0)),now+2));
        rejects(()->coordinates(telemetry.fields("dock"),System.currentTimeMillis()+100));
        telemetry.record("dock",osd(data(),now+3));
        telemetry.record("dock",osd(Map.of("mode_code",4),now+4));
        rejects(()->coordinates(telemetry.fields("dock"),System.currentTimeMillis()+100));
        telemetry.record("dock",osd(data(),now+5));
        telemetry.record("dock",osd(Map.of("position_state",Map.of("is_fixed",1)),now+6));
        rejects(()->coordinates(telemetry.fields("dock"),System.currentTimeMillis()+100));
        telemetry.record("dock",osd(data(),now+7));
        var registry=(IDeviceService)Proxy.newProxyInstance(MobileDockCalibrationChecks.class.getClassLoader(),new Class[]{IDeviceService.class},(p,m,a)->Optional.of(DeviceDTO.builder().type(DeviceTypeEnum.DOCK3).build()));
        var devices=(IDeviceRedisService)Proxy.newProxyInstance(MobileDockCalibrationChecks.class.getClassLoader(),new Class[]{IDeviceRedisService.class},(p,m,a)->true);
        var docks=new FrogJumpLandingRecommendationService(null,null,null){@Override public boolean isWorkspaceDock(String w,String sn){return "w".equals(w)&&"dock".equals(sn);}};
        final int[] calls={0};
        var publish=new ServicesPublish(){@Override public TopicServicesResponse<ServicesReplyData> publish(String sn,String method,Object body,String bid,int retry,long timeout){
            calls[0]++;check(retry==0,"no auto retry");check("rtk_calibration".equals(method),"method");
            Map<String,Object> target=map(((List<?>)map(body).get("devices")).get(0));
            check(code(target.get("type"),1)&&"3".equals(target.get("module")),"API1 manual dock payload");
            check(map(target.get("data")).equals(coordinate),"use self converge instead of ordinary position");
            return new TopicServicesResponse<ServicesReplyData>().setData(new ServicesReplyData().setResult(new ServicesErrorCode(0)).setOutput(Map.of("status","ok")));
        }};
        var service=new MobileDockCalibrationService(telemetry,docks,devices,registry,publish,redis);
        rejects(()->service.start("other","dock"));check(calls[0]==0,"workspace gate");
        Map<String,Object> started=service.start("w","dock");
        check("pending".equals(map(started.get("operation")).get("state")),"ack ok is not completion");
        rejects(()->service.start("w","dock"));check(calls[0]==1,"duplicate start blocked");
        String bid=String.valueOf(map(started.get("operation")).get("bid"));
        Map<String,Object> event=Map.of("result",0,"output",Map.of("status","ok","ext",Map.of("devices",List.of(Map.of("sn","dock","module","3","result",0,"status","ok")))));
        var request=new TopicEventsRequest<Map<String,Object>>().setFrom("dock").setBid("wrong").setTid("tid").setMethod("rtk_calibration").setNeedReply(true).setData(event);
        service.calibrationResult(request);check("pending".equals(map(service.status("w","dock").get("operation")).get("state")),"ignore wrong bid");
        request.setBid(bid);var reply=service.calibrationResult(request);
        check("ok".equals(map(service.status("w","dock").get("operation")).get("state")),"device result completes");
        redis.values.put("mobile-calibration:result:"+bid,redis.serializer.serialize(result("dock",event)));
        check("ok".equals(map(service.status("w","dock").get("operation")).get("state")),"legacy result without class metadata still readable");
        check(Boolean.TRUE.equals(service.status("w","dock").get("ready")),"legacy successful calibration does not block next calibration");
        redis.values.remove("mobile-calibration:lease:dock");
        check("pending".equals(map(service.start("w","dock").get("operation")).get("state")),"new calibration after legacy success");
        check(calls[0]==2,"only explicit second request publishes again");
        check(bid.equals(reply.getBid())&&"tid".equals(reply.getTid())&&reply.getData()!=null,"event ack correlation");
        check("failed".equals(result("dock",Map.of("result",0,"output",Map.of("status","ok"))).get("state")),"missing per-device result not success");
        check(EventsMethodEnum.find("rtk_calibration")==EventsMethodEnum.RTK_CALIBRATION,"event routed");
        System.out.println("Mobile dock checks passed: fresh/split telemetry, precision, incomplete coordinates, convergence, busy dock, scope, payload, duplicate command, ACK vs result, event correlation. No real device commands sent.");
    }
}
