package com.dji.sample.control.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.component.websocket.model.BizCodeEnum;
import com.dji.sample.component.websocket.service.IWebSocketMessageService;
import com.dji.sample.control.model.dto.ResultNotifyDTO;
import com.dji.sample.df.windDf.dao.WindTurbineMapper;
import com.dji.sample.df.windDf.model.entity.WindTurbine;
import com.dji.sample.df.uavCommonHandleDf.service.RoutePlanService;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sample.manage.model.enums.UserTypeEnum;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sdk.cloudapi.control.*;
import com.dji.sdk.cloudapi.control.api.AbstractControlService;
import com.dji.sdk.cloudapi.wayline.api.AbstractWaylineService;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.mqtt.MqttReply;
import com.dji.sdk.mqtt.events.TopicEventsRequest;
import com.dji.sdk.mqtt.events.TopicEventsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.dji.sdk.cloudapi.control.FlyToStatusEnum.WAYLINE_OK;

/**
 * @author sean
 * @version 1.7
 * @date 2023/7/4
 */
@Service
@Slf4j
public class SDKControlService extends AbstractControlService {

    @Autowired
    private IWebSocketMessageService webSocketMessageService;

    @Autowired
    private IDeviceRedisService deviceRedisService;

    @Autowired
    private ObjectMapper mapper;

    @Resource
    RedisUtils redisUtils;

    @Resource
    private WindTurbineMapper windTurbineMapper;

    @Resource
    RoutePlanService routePlan;

    @Resource
    IDeviceMapper deviceMapper;

    @Autowired
    @Qualifier("SDKWaylineService")
    private AbstractWaylineService abstractWaylineService;

    private final ConcurrentMap<String, FlyToStatusEnum> processedFlyTo = new ConcurrentHashMap<>();

//  指令飞行暂时不用，回头注释掉
    @Override
    public TopicEventsResponse<MqttReply> flyToPointProgress(TopicEventsRequest<FlyToPointProgress> request, MessageHeaders headers) {
        String dockSn = request.getGateway();

        Optional<DeviceDTO> deviceOpt = deviceRedisService.getDeviceOnline(dockSn);
        if (deviceOpt.isEmpty()) {
            log.error("The dock is offline.");
            return null;
        }
        FlyToStatusEnum status = request.getData().getStatus();
        String flyToId = request.getData().getFlyToId();
        if (request.getData() != null) {
            if (status.equals(WAYLINE_OK)) {
                processedFlyTo.compute(flyToId, (key, current) -> {
                    if (current == null) {
                        // 写发送逻辑
                        String url = "http://172.20.63.157:5002/state";
                        JSONObject jsonObject = new JSONObject();
                        JSONObject jsonObject1 = new JSONObject();

                        String turbineName = redisUtils.get("turbineName").toString();

                        WindTurbine windTurbine = windTurbineMapper.selectOne(new LambdaQueryWrapper<WindTurbine>().eq(WindTurbine::getTurbineName, turbineName));
                        jsonObject1.put("height", windTurbine.getBladeCenterHeight());
                        jsonObject1.put("lat", windTurbine.getPeakLatitude());
                        jsonObject1.put("lon", windTurbine.getPeakLongitude());
                        jsonObject.put("waylineType", "flyToPoint");
                        jsonObject.put("flightName", turbineName + "front");
                        jsonObject.put("status", status);
                        jsonObject.put("centerPoint", jsonObject1);
                        String jsonInput = jsonObject.toString();
                        try {
                            URL obj = new URL(url);
                            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                            // 设置请求方法
                            con.setRequestMethod("POST");
                            con.setRequestProperty("Content-Type", "application/json");
                            con.setDoOutput(true);
                            // 发送请求
                            try (OutputStream os = con.getOutputStream()) {
                                byte[] input = jsonInput.getBytes("utf-8");
                                os.write(input, 0, input.length);
                            }
                            // 获取响应
                            int responseCode = con.getResponseCode();
                            System.out.println("Response Code: " + responseCode);

                            try (BufferedReader br = new BufferedReader(
                                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                                StringBuilder response1 = new StringBuilder();
                                String responseLine;
                                while ((responseLine = br.readLine()) != null) {
                                    response1.append(responseLine.trim());
                                }
                                JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
                                log.info("接受空中航线下发条件---"+jsonResponse.toString());
                                if (jsonResponse.getString("desc").equals("1")) {
                                    if (jsonResponse.getString("yppd") != null || jsonResponse.getString("ypjd") != null) {
                                        if (jsonResponse.getString("yppd") != null) {
                                            //不停机巡检
                                            turbineName = jsonResponse.getString("turbine_name");
                                            redisUtils.set("in_fight_state","working");
                                            routePlan.workingWayline(turbineName,null);
                                        } else if (jsonResponse.getString("ypjd") != null) {
                                            //停机巡检
                                            log.info("执行停机巡检-------------");
                                            redisUtils.set("in_fight_state","stop");
                                            turbineName = jsonResponse.getString("turbine_name");
                                            String ypjd = jsonResponse.getString("ypjd");
                                            double value = Double.parseDouble(ypjd);
                                            routePlan.stopWayline(turbineName, value,null);
                                        }
                                    }
                                } else {
                                    //分析失败返航
                                    DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));
                                    abstractWaylineService.returnHome(SDKManager.getDeviceSDK(deviceEntity.getDeviceSn()));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return current;
                });
            }
        }
            FlyToPointProgress eventsReceiver = request.getData();
            webSocketMessageService.sendBatch(deviceOpt.get().getWorkspaceId(), UserTypeEnum.WEB.getVal(),
                    BizCodeEnum.FLY_TO_POINT_PROGRESS.getCode(),
                    ResultNotifyDTO.builder().sn(dockSn)
                            .message(eventsReceiver.getResult().toString())
                            .result(eventsReceiver.getResult().getCode())
                            .build());
            return new TopicEventsResponse<MqttReply>().setData(MqttReply.success());

    }

    @Override
    public TopicEventsResponse<MqttReply> takeoffToPointProgress(TopicEventsRequest<TakeoffToPointProgress> request, MessageHeaders headers) {
        String dockSn  = request.getGateway();

        Optional<DeviceDTO> deviceOpt = deviceRedisService.getDeviceOnline(dockSn);
        if (deviceOpt.isEmpty()) {
            log.error("The dock is offline.");
            return null;
        }

        TakeoffToPointProgress eventsReceiver = request.getData();
        webSocketMessageService.sendBatch(deviceOpt.get().getWorkspaceId(), UserTypeEnum.WEB.getVal(),
                BizCodeEnum.TAKE_OFF_TO_POINT_PROGRESS.getCode(),
                ResultNotifyDTO.builder().sn(dockSn)
                        .message(eventsReceiver.getResult().toString())
                        .result(eventsReceiver.getResult().getCode())
                        .build());

        return new TopicEventsResponse<MqttReply>().setData(MqttReply.success());
    }

    @Override
    public TopicEventsResponse<MqttReply> drcStatusNotify(TopicEventsRequest<DrcStatusNotify> request, MessageHeaders headers) {
        String dockSn  = request.getGateway();

        Optional<DeviceDTO> deviceOpt = deviceRedisService.getDeviceOnline(dockSn);
        if (deviceOpt.isEmpty()) {
            return null;
        }

        DrcStatusNotify eventsReceiver = request.getData();
        if (DrcStatusErrorEnum.SUCCESS != eventsReceiver.getResult()) {
            webSocketMessageService.sendBatch(
                    deviceOpt.get().getWorkspaceId(), UserTypeEnum.WEB.getVal(), BizCodeEnum.DRC_STATUS_NOTIFY.getCode(),
                    ResultNotifyDTO.builder().sn(dockSn)
                            .message(eventsReceiver.getResult().getMessage())
                            .result(eventsReceiver.getResult().getCode()).build());
        }
        return new TopicEventsResponse<MqttReply>().setData(MqttReply.success());
    }

    @Override
    public TopicEventsResponse<MqttReply> joystickInvalidNotify(TopicEventsRequest<JoystickInvalidNotify> request, MessageHeaders headers) {
        String dockSn  = request.getGateway();

        Optional<DeviceDTO> deviceOpt = deviceRedisService.getDeviceOnline(dockSn);
        if (deviceOpt.isEmpty()) {
            return null;
        }

        JoystickInvalidNotify eventsReceiver = request.getData();
        webSocketMessageService.sendBatch(
                deviceOpt.get().getWorkspaceId(), UserTypeEnum.WEB.getVal(), BizCodeEnum.JOYSTICK_INVALID_NOTIFY.getCode(),
                ResultNotifyDTO.builder().sn(dockSn)
                        .message(eventsReceiver.getReason().getMessage())
                        .result(eventsReceiver.getReason().getVal()).build());
        return new TopicEventsResponse<MqttReply>().setData(MqttReply.success());
    }
}
