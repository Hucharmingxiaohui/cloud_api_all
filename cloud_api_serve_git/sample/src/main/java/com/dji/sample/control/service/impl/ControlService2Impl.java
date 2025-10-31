package com.dji.sample.control.service.impl;

import com.df.framework.redis.RedisUtils;
import com.dji.sample.control.model.enums.DroneAuthorityEnum;
import com.dji.sample.control.model.param.DronePayloadParam;
import com.dji.sample.control.model.param.InFlightWaylineDeliverParam;
import com.dji.sample.control.service.IControlService2;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.service.IDevicePayloadService;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.manage.service.IDeviceService;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sample.wayline.model.dto.WaylineTaskConditionDTO;
import com.dji.sample.wayline.model.param.CreateJobParam;
import com.dji.sample.wayline.service.IWaylineJobService;
import com.dji.sample.wayline.service.IWaylineRedisService;
import com.dji.sdk.cloudapi.control.PayloadAuthorityGrabRequest;
import com.dji.sdk.cloudapi.control.api.AbstractControlService;
import com.dji.sdk.cloudapi.control.api.InFlightWaylineDeliverRequest;
import com.dji.sdk.cloudapi.debug.api.AbstractDebugService;
import com.dji.sdk.cloudapi.device.DroneModeCodeEnum;
import com.dji.sdk.cloudapi.device.PayloadIndex;
import com.dji.sdk.cloudapi.wayline.ExecutableConditions;
import com.dji.sdk.cloudapi.wayline.ReadyConditions;
import com.dji.sdk.cloudapi.wayline.TaskTypeEnum;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.exception.CloudSDKErrorEnum;
import com.dji.sdk.mqtt.services.ServicesReplyData;
import com.dji.sdk.mqtt.services.TopicServicesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class ControlService2Impl implements IControlService2 {

    @Autowired
    private IWaylineRedisService waylineRedisService;

    @Resource
    RedisUtils redisUtils;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceRedisService deviceRedisService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private IDevicePayloadService devicePayloadService;

    @Autowired
    private AbstractControlService abstractControlService;


    @Override
    public HttpResultResponse inFlightWaylineDeliver(String sn, InFlightWaylineDeliverParam param,CreateJobParam createJobParam) {
        try {
            checkFlyToCondition(sn);

            fillImmediateTime(createJobParam);

            for (Long taskDay : createJobParam.getTaskDays()) {
                LocalDate date = LocalDate.ofInstant(Instant.ofEpochSecond(taskDay), ZoneId.systemDefault());
                for (List<Long> taskPeriod : createJobParam.getTaskPeriods()) {
                    long beginTime = LocalDateTime.of(date, LocalTime.ofInstant(Instant.ofEpochSecond(taskPeriod.get(0)), ZoneId.systemDefault()))
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    long endTime = taskPeriod.size() > 1 ?
                            LocalDateTime.of(date, LocalTime.ofInstant(Instant.ofEpochSecond(taskPeriod.get(1)), ZoneId.systemDefault()))
                                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : beginTime;
//                  只有不是立即执行，并且最晚时间小于当前（已经执行完）的不执行，否则都执行
                    if (TaskTypeEnum.IMMEDIATE != createJobParam.getTaskType() && endTime < System.currentTimeMillis()) {
                        continue;
                    }
                    String creator = redisUtils.get("creator").toString();
                    String workspaceId = redisUtils.get("workspaceId").toString();
//                    Optional<WaylineJobDTO> waylineJobOpt = waylineJobService.createWaylineJob2(createJobParam,workspaceId, creator, beginTime, endTime);
//                    if (waylineJobOpt.isEmpty()) {
//                        throw new SQLException("Failed to create wayline job.");
//                    }
//                    WaylineJobDTO waylineJob = waylineJobOpt.get();
//                     If it is a conditional task type, add conditions to the job parameters.
//                    addConditions(waylineJob, createJobParam, beginTime, endTime);
                }
            }
            TopicServicesResponse<ServicesReplyData> response = abstractControlService.inFlightWaylineDeliver(
                    SDKManager.getDeviceSDK(sn), mapper.convertValue(param, InFlightWaylineDeliverRequest.class));
            ServicesReplyData reply = response.getData();
            return reply.getResult().isSuccess() ?
                    HttpResultResponse.success()
                    : HttpResultResponse.error("inFlightWaylineDeliver failed. " + reply.getResult());
        }catch (Exception e) {
            e.printStackTrace();
            return HttpResultResponse.error(e.getMessage());
        }
    }

    private void fillImmediateTime(CreateJobParam param) {
        if (TaskTypeEnum.IMMEDIATE != param.getTaskType()) {
            return;
        }
        long now = System.currentTimeMillis() / 1000;
        param.setTaskDays(List.of(now));
        param.setTaskPeriods(List.of(List.of(now)));
    }

    private void addConditions(WaylineJobDTO waylineJob, CreateJobParam param, Long beginTime, Long endTime) {
        if (TaskTypeEnum.CONDITIONAL != param.getTaskType()) {
            return;
        }

        waylineJob.setConditions(
                WaylineTaskConditionDTO.builder()
                        .executableConditions(Objects.nonNull(param.getMinStorageCapacity()) ?
                                new ExecutableConditions().setStorageCapacity(param.getMinStorageCapacity()) : null)
                        .readyConditions(new ReadyConditions()
                                .setBatteryCapacity(param.getMinBatteryCapacity())
                                .setBeginTime(beginTime)
                                .setEndTime(endTime))
                        .build());

        waylineRedisService.setConditionalWaylineJob(waylineJob);
        // key: wayline_job_condition, value: {workspace_id}:{dock_sn}:{job_id}
        boolean isAdd = waylineRedisService.addPrepareConditionalWaylineJob(waylineJob);
        if (!isAdd) {
            throw new RuntimeException("Failed to create conditional job.");
        }
    }

    private void checkFlyToCondition(String dockSn) {
        // TODO 设备固件版本不兼容情况
        Optional<DeviceDTO> dockOpt = deviceRedisService.getDeviceOnline(dockSn);
        if (dockOpt.isEmpty()) {
            throw new RuntimeException("The dock is offline, please restart the dock.");
        }

        DroneModeCodeEnum deviceMode = deviceService.getDeviceMode(dockOpt.get().getChildDeviceSn());
        if (DroneModeCodeEnum.MANUAL != deviceMode) {
//            throw new RuntimeException("The current state of the drone does not support this function, please try again later.");
        }

        HttpResultResponse result = seizeAuthority(dockSn, DroneAuthorityEnum.FLIGHT, null);
        if (HttpResultResponse.CODE_SUCCESS != result.getCode()) {
            throw new IllegalArgumentException(result.getMessage());
        }
    }

    public HttpResultResponse seizeAuthority(String sn, DroneAuthorityEnum authority, DronePayloadParam param) {
        TopicServicesResponse<ServicesReplyData> response;
        switch (authority) {
            case FLIGHT:
                if (deviceService.checkAuthorityFlight(sn)) {
                    return HttpResultResponse.success();
                }
                response = abstractControlService.flightAuthorityGrab(SDKManager.getDeviceSDK(sn));
                break;
            case PAYLOAD:
                if (checkPayloadAuthority(sn, param.getPayloadIndex())) {
                    return HttpResultResponse.success();
                }
                response = abstractControlService.payloadAuthorityGrab(SDKManager.getDeviceSDK(sn),
                        new PayloadAuthorityGrabRequest().setPayloadIndex(new PayloadIndex(param.getPayloadIndex())));
                break;
            default:
                return HttpResultResponse.error(CloudSDKErrorEnum.INVALID_PARAMETER);
        }

        ServicesReplyData serviceReply = response.getData();
        return serviceReply.getResult().isSuccess() ?
                HttpResultResponse.success()
                : HttpResultResponse.error(serviceReply.getResult());
    }

    private Boolean checkPayloadAuthority(String sn, String payloadIndex) {
        Optional<DeviceDTO> dockOpt = deviceRedisService.getDeviceOnline(sn);
        if (dockOpt.isEmpty()) {
            throw new RuntimeException("The dock is offline, please restart the dock.");
        }
        return devicePayloadService.checkAuthorityPayload(dockOpt.get().getChildDeviceSn(), payloadIndex);
    }

}
