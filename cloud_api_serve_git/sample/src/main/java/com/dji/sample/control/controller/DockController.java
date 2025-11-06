package com.dji.sample.control.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.control.model.enums.DroneAuthorityEnum;
import com.dji.sample.control.model.enums.RemoteDebugMethodEnum;
import com.dji.sample.control.model.param.*;
import com.dji.sample.control.service.IControlService;
import com.dji.sample.control.service.IControlService2;
import com.dji.sample.df.importKmzNoValiDf.service.ImportKmzNoValiService;
import com.dji.sample.df.wind.service.WindTurbineService;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.dao.IWorkspaceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sample.manage.model.entity.WorkspaceEntity;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.dji.sample.wayline.model.param.CreateJobParam;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.cloudapi.control.FileParam;
import com.dji.sdk.cloudapi.device.ExitWaylineWhenRcLostEnum;
import com.dji.sdk.cloudapi.wayline.*;
import com.dji.sdk.common.HttpResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

/**
 * @author sean
 * @version 1.2
 * @date 2022/7/29
 */
@RestController
@Slf4j
@RequestMapping("${url.control.prefix}${url.control.version}/devices")
public class DockController {

    @Autowired
    private IControlService controlService;

    @Autowired
    private IControlService2 controlService2;

    @Resource
    WindTurbineService windTurbineService;

    @Resource
    IDeviceMapper deviceMapper;

    @Autowired
    IWorkspaceMapper workspaceMapper;

    @Autowired
    ImportKmzNoValiService importKmzNoValiService;

    @Resource
    RedisUtils redisUtils;

    @Autowired
    private IWaylineFileService waylineFileService;

    @PostMapping("/{sn}/jobs/{service_identifier}")
    public HttpResultResponse createControlJob(@PathVariable String sn,
                                               @PathVariable("service_identifier") String serviceIdentifier,
                                               @Valid @RequestBody(required = false) RemoteDebugParam param) {
        return controlService.controlDockDebug(sn, RemoteDebugMethodEnum.find(serviceIdentifier), param);
    }

    @PostMapping("/{sn}/jobs/fly-to-point")
    public HttpResultResponse flyToPoint(@PathVariable String sn, @Valid @RequestBody FlyToPointParam param) {
        return controlService.flyToPoint(sn, param);
    }

    @DeleteMapping("/{sn}/jobs/fly-to-point")
    public HttpResultResponse flyToPointStop(@PathVariable String sn) {
        return controlService.flyToPointStop(sn);
    }

    @PostMapping("/{sn}/jobs/takeoff-to-point")
    public HttpResultResponse takeoffToPoint(@PathVariable String sn, @Valid @RequestBody TakeoffToPointParam param) {
        return controlService.takeoffToPoint(sn, param);
    }

    @PostMapping("/{sn}/authority/flight")
    public HttpResultResponse seizeFlightAuthority(@PathVariable String sn) {
        return controlService.seizeAuthority(sn, DroneAuthorityEnum.FLIGHT, null);
    }

    @PostMapping("/{sn}/authority/payload")
    public HttpResultResponse seizePayloadAuthority(@PathVariable String sn, @Valid @RequestBody DronePayloadParam param) {
        return controlService.seizeAuthority(sn, DroneAuthorityEnum.PAYLOAD, param);
    }

    @PostMapping("/{sn}/payload/commands")
    public HttpResultResponse payloadCommands(@PathVariable String sn, @Valid @RequestBody PayloadCommandsParam param) throws Exception {
        param.setSn(sn);
        return controlService.payloadCommands(param);
    }

    @PostMapping("/jobs/in_flight_wayline_deliver")
    public HttpResultResponse inFlightWaylineDeliver() {
        String fileName="tower停机测试";
        WaylineFileEntity entity = importKmzNoValiService.getWaylineByFileName(fileName);

        InFlightWaylineDeliverParam param = new InFlightWaylineDeliverParam();
        DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));
        // get wayline file
        WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());
        Optional<GetWaylineListResponse> waylineFile = waylineFileService.getWaylineByWaylineId(workspaceEntity.getWorkspaceId(),entity.getWaylineId());
        if (waylineFile.isEmpty()) {
            log.error("The wayline file doesn't exist.");
        }
        // get file url
        URL url1 = null;
        try {
            url1 = waylineFileService.getObjectUrl(workspaceEntity.getWorkspaceId(), waylineFile.get().getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        FileParam fileParam = new FileParam();
        fileParam.setFingerprint(waylineFile.get().getSign());
        fileParam.setUrl(url1.toString());
        param.setFile(fileParam);
//              失控返航
        param.setOutOfControlAction(OutOfControlActionEnum.RETURN_TO_HOME);
        param.setExitWaylineWhenRcLost(ExitWaylineWhenRcLostEnum.EXECUTE_RC_LOST_ACTION);
        param.setRthAltitude(30);
        param.setRthMode(RthModeEnum.PRESET_HEIGHT);
        param.setWaylinePrecisionType(WaylinePrecisionTypeEnum.RTK);
        String waylineName = entity.getName();
        CreateJobParam createJobParam = new CreateJobParam();
        createJobParam.setName(waylineName);
        createJobParam.setFileId(entity.getWaylineId());
        createJobParam.setDockSn(deviceEntity.getDeviceSn());
        createJobParam.setWaylineType(WaylineTypeEnum.WAYPOINT);
//              任务类型为立即执行，是否后续不需要额外判断了
        createJobParam.setTaskType(TaskTypeEnum.IMMEDIATE);
        createJobParam.setRthAltitude(30);
        createJobParam.setOutOfControlAction(OutOfControlActionEnum.RETURN_TO_HOME);
        createJobParam.setMinBatteryCapacity(50);
        createJobParam.setMinStorageCapacity(null);
        List<Long> task_days=new ArrayList<>();
        createJobParam.setTaskDays(task_days);
        List<List<Long>> task_periods=new ArrayList<>();
        createJobParam.setTaskPeriods(task_periods);
//              空中航线没有创建计划，计划id和job_id设为一样
        String job_id = UUID.randomUUID().toString();
        createJobParam.setPlanId(job_id);
        param.setInFlightWaylineId(job_id);
        return controlService2.inFlightWaylineDeliver(deviceEntity.getDeviceSn(), param,createJobParam);

    }

}
