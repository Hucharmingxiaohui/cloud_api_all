package com.dji.sample.control.service.impl;

import com.dji.sample.df.frogJumpDf.service.FrogJumpLandingRecommendationService;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.manage.service.IDeviceService;
import com.dji.sdk.cloudapi.device.DeviceTypeEnum;
import com.dji.sdk.mqtt.ChannelName;
import com.dji.sdk.mqtt.MqttReply;
import com.dji.sdk.mqtt.events.TopicEventsRequest;
import com.dji.sdk.mqtt.events.TopicEventsResponse;
import com.dji.sdk.mqtt.services.ServicesPublish;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static com.dji.sample.control.service.impl.MobileDockCalibrationRules.*;

@Service
public class MobileDockCalibrationService {
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(MobileDockCalibrationService.class);
    private final MobileDockCalibrationTelemetry telemetry;
    private final FrogJumpLandingRecommendationService docks;
    private final IDeviceRedisService devices;
    private final IDeviceService registry;
    private final ServicesPublish publisher;
    private final RedisTemplate<String,Object> redis;
    public MobileDockCalibrationService(MobileDockCalibrationTelemetry telemetry,FrogJumpLandingRecommendationService docks,
            IDeviceRedisService devices,IDeviceService registry,ServicesPublish publisher,RedisTemplate<String,Object> redis) {
        this.telemetry=telemetry;this.docks=docks;this.devices=devices;this.registry=registry;this.publisher=publisher;this.redis=redis;
    }
    private void authorize(String workspace,String sn) {
        if(!docks.isWorkspaceDock(workspace,sn)) throw new IllegalArgumentException("机巢不属于当前工作空间");
        if(registry.getDeviceBySn(sn).map(d->d.getType()!=DeviceTypeEnum.DOCK3).orElse(true))
            throw new IllegalArgumentException("车载机巢标定当前仅支持大疆机场 3");
    }
    private String key(String sn) {return "mobile-calibration:operation:"+sn;}
    private void save(String key,Object value) {redis.opsForValue().set(key,value,1,TimeUnit.DAYS);}
    private Map<String,Object> readResult(String bid) {
        String resultKey="mobile-calibration:result:"+bid;
        try { return map(redis.opsForValue().get(resultKey)); }
        catch(org.springframework.data.redis.serializer.SerializationException incompatible) {
            // Older versions stored final Map.of instances without NON_FINAL type metadata.
            // Read only this result key as plain JSON; retain the original TTL and outcome.
            byte[] raw=redis.execute((org.springframework.data.redis.core.RedisCallback<byte[]>) connection ->
                    connection.get(resultKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            if(raw==null) return Collections.emptyMap();
            return legacyResult(raw);
        }
    }
    static Map<String,Object> legacyResult(byte[] raw) {
        try {
            Map<String,Object> value=new com.fasterxml.jackson.databind.ObjectMapper().readValue(raw,LinkedHashMap.class);
            if(value.size()!=2 || !Set.of("ok","failed").contains(String.valueOf(value.get("state")))
                    || !(value.get("message") instanceof String)) throw new IllegalArgumentException("Invalid calibration result");
            return value;
        } catch(Exception e) { throw new IllegalArgumentException("标定结果缓存格式异常，请联系维护人员检查",e); }
    }
    public Map<String,Object> status(String workspace,String sn) {
        authorize(workspace,sn);
        Map<String,Object> result=new LinkedHashMap<>();
        Map<String,Object> fields=telemetry.fields(sn);
        result.put("ready",false);result.put("sn",sn);
        result.put("position_state",map(fields.get("position_state")).get("value"));
        result.put("home_position_is_valid",map(fields.get("home_position_is_valid")).get("value"));
        result.put("coordinate_received_at",map(fields.get("self_converge_coordinate")).get("received_at"));
        try {
            if(!Boolean.TRUE.equals(devices.checkDeviceOnline(sn))) throw new IllegalArgumentException("机场离线，请恢复连接");
            result.put("coordinate",coordinates(fields,System.currentTimeMillis()));
            result.put("ready",true);result.put("message","已取得最新自收敛坐标，请确认车辆已停稳");
        } catch(IllegalArgumentException e) { result.put("message",e.getMessage()); }
        Map<String,Object> op=map(redis.opsForValue().get(key(sn)));
        if(!op.isEmpty()) {
            Map<String,Object> view=new LinkedHashMap<>(op);
            Map<String,Object> event=readResult(String.valueOf(op.get("bid")));
            if(!event.isEmpty()) view.putAll(event);
            if("pending".equals(view.get("state"))&&System.currentTimeMillis()-((Number)op.get("started_at")).longValue()>120000) {
                view.put("state","unknown");view.put("message","标定结果等待超时，状态未知，请核对机场状态后再操作");
            }
            if("pending".equals(view.get("state"))) {result.put("ready",false);result.put("message","标定已提交，等待机场结果");}
            result.put("operation",view);
        }
        return result;
    }
    public synchronized Map<String,Object> start(String workspace,String sn) {
        Map<String,Object> before=status(workspace,sn);
        if(!Boolean.TRUE.equals(before.get("ready"))) throw new IllegalArgumentException(String.valueOf(before.get("message")));
        // Always read the server's current samples. Client coordinates are never accepted.
        Map<String,Object> coordinate=coordinates(telemetry.fields(sn),System.currentTimeMillis());
        String bid=UUID.randomUUID().toString();
        // Distributed short lease prevents concurrent POSTs from different application instances.
        if(!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("mobile-calibration:lease:"+sn,bid,15,TimeUnit.SECONDS)))
            throw new IllegalArgumentException("标定请求正在处理，请稍后刷新");
        Map<String,Object> op=new LinkedHashMap<>();
        op.put("bid",bid);op.put("workspace",workspace);op.put("started_at",System.currentTimeMillis());
        op.put("coordinate",coordinate);op.put("state","pending");op.put("message","等待机场接收标定指令");
        save(key(sn),op);
        log.info("Mobile dock calibration submitted: sn={}, bid={}, coordinate={}",sn,bid,coordinate);
        try {
            var response=publisher.publish(sn,"rtk_calibration",payload(sn,coordinate),bid,0,8000L);
            var reply=response.getData();
            Object state=map(reply.getOutput()).get("status");
            if(!reply.getResult().isSuccess()||Set.of("failed","rejected","canceled","timeout").contains(String.valueOf(state))) {
                op.put("state","failed");op.put("message","机场拒绝标定："+reply.getResult()+"，状态："+state);
            } else { op.put("message","机场已接收，等待标定结果"); }
        } catch(Exception e) {
            // Publish may have reached the device; never auto-retry or report success.
            op.put("message","暂未收到指令响应，继续等待机场结果，请勿重复提交");
            log.warn("Mobile dock calibration reply uncertain: sn={}, bid={}",sn,bid,e);
        }
        save(key(sn),op);
        return status(workspace,sn);
    }
    @ServiceActivator(inputChannel="inbound_events_rtk_calibration",outputChannel=ChannelName.OUTBOUND_EVENTS)
    public TopicEventsResponse<MqttReply> calibrationResult(TopicEventsRequest<Map<String,Object>> request) {
        String sn=request.getFrom();
        Map<String,Object> op=map(redis.opsForValue().get(key(sn)));
        if(!op.isEmpty()&&Objects.equals(op.get("bid"),request.getBid())) {
            Map<String,Object> outcome=result(sn,request.getData());
            log.info("Mobile dock calibration result: sn={}, bid={}, outcome={}",sn,request.getBid(),outcome);
            // First terminal event wins; duplicate or delayed unrelated events cannot change it.
            redis.opsForValue().setIfAbsent("mobile-calibration:result:"+request.getBid(),new LinkedHashMap<>(outcome),1,TimeUnit.DAYS);
        }
        return new TopicEventsResponse<MqttReply>().setBid(request.getBid()).setTid(request.getTid())
                .setMethod(request.getMethod()).setTimestamp(System.currentTimeMillis())
                .setData(request.isNeedReply()?MqttReply.success():null);
    }
}
