package com.dji.sample.control.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.control.model.enums.DroneAuthorityEnum;
import com.dji.sample.control.model.enums.RemoteDebugMethodEnum;
import com.dji.sample.control.model.param.*;
import com.dji.sample.control.service.IControlService;
import com.dji.sample.control.service.IControlService2;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.importKmzNoValiDf.service.ImportKmzNoValiService;
import com.dji.sample.df.wind.service.WindTurbineService;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.dao.IWorkspaceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sample.manage.model.entity.WorkspaceEntity;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.model.param.CreateJobParam;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.cloudapi.control.FileParam;
import com.dji.sdk.cloudapi.device.ExitWaylineWhenRcLostEnum;
import com.dji.sdk.cloudapi.wayline.*;
import com.dji.sdk.common.HttpResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.dji.sample.df.wind.utils.FileUtil.convert;

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

    // 最大重试次数，防止无限循环
    private static final int MAX_RETRY_COUNT = 10;
    // 重试间隔时间（毫秒）
    private static final long RETRY_INTERVAL_MS = 2000;

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
    public HttpResultResponse inFlightWaylineDeliver(@RequestBody JSONObject jsonObject) throws IOException, SQLException {
        String routeName = jsonObject.getString("routeName");
        // 项目根目录下的文件路径（根据实际部署环境调整）
        String projectPath = System.getProperty("user.dir");
        String filePath = projectPath + File.separator + "file" + File.separator + "kmz" + File.separator + routeName + ".kmz";
        MultipartFile file = convert(filePath);
        if (Objects.isNull(file)) {
            log.error("kmz文件未检测到");
        }
        String workspaceId = "e3dea0f5-37f2-4d79-ae58-490af3228069";
        String creator = "adminPC";
        importKmzNoValiService.importKmzFile(file, workspaceId, creator, null);
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.endsWith(".kmz")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        WaylineFileEntity entity = importKmzNoValiService.getWaylineByFileName(fileName);
        if (Objects.isNull(entity)) {
            log.error("导入外部航线失败");
        }
        InFlightWaylineDeliverParam param = new InFlightWaylineDeliverParam();

        // get wayline file
        WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());

        Optional<GetWaylineListResponse> waylineFile = waylineFileService.getWaylineByWaylineId(workspaceEntity.getWorkspaceId(), entity.getWaylineId());
        if (waylineFile.isEmpty()) {
            log.error("The wayline file doesn't exist.");
        }

        DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));
        URL url1 = waylineFileService.getObjectUrl(workspaceEntity.getWorkspaceId(), waylineFile.get().getId());
        FileParam fileParam = new FileParam();
        fileParam.setFingerprint(waylineFile.get().getSign());
        fileParam.setUrl(url1.toString());
        param.setFile(fileParam);
//      失控返航
        param.setOutOfControlAction(OutOfControlActionEnum.RETURN_TO_HOME);
        param.setExitWaylineWhenRcLost(ExitWaylineWhenRcLostEnum.EXECUTE_RC_LOST_ACTION);
        param.setRthAltitude(30);
        param.setRthMode(RthModeEnum.PRESET_HEIGHT);
        param.setWaylinePrecisionType(WaylinePrecisionTypeEnum.RTK);
        String waylineName = entity.getName();
//      其实这个参数没什么用，是为了生成job记录，空中航线的job记录会自动跟着第一条航线，所以后面的逻辑给注释了
        CreateJobParam createJobParam = new CreateJobParam();
        createJobParam.setName(waylineName);
        createJobParam.setFileId(entity.getWaylineId());
        createJobParam.setDockSn(deviceEntity.getDeviceSn());
        createJobParam.setWaylineType(WaylineTypeEnum.WAYPOINT);
//      任务类型为立即执行，是否后续不需要额外判断了
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
        return performDeliveryWithRetry(deviceEntity.getDeviceSn(), param,createJobParam);
    }

    public HttpResultResponse performDeliveryWithRetry(String sn, InFlightWaylineDeliverParam param, CreateJobParam createJobParam) {
        int retryCount = 0;
        int code = -1; // 初始化为-1以进入循环

        // 当返回码为-1且未超过最大重试次数时，持续重试
        while (code == -1 && retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            System.out.println("第 " + retryCount + " 次尝试发送...");

            // 调用您的业务方法
            // 此处替换为您实际的 service 调用
            HttpResultResponse httpResultResponse = controlService2.inFlightWaylineDeliver(sn, param, createJobParam);

            code = httpResultResponse.getCode();

            // 如果本次返回码仍为-1，且未达到最大重试次数，则等待后继续
            if (code == -1 && retryCount < MAX_RETRY_COUNT) {
                System.out.println("发送失败(code=-1)，" + RETRY_INTERVAL_MS + "毫秒后重试...");
                try {
                    // 线程休眠，间隔一段时间再重试
                    TimeUnit.MILLISECONDS.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("重试过程被中断");
                    break;
                }
            }
        }
        HttpResultResponse httpResultResponse=new HttpResultResponse();
        // 循环结束后，根据最终状态输出结果
        if (code != -1) {
            System.out.println("发送成功！最终返回码: " + code);
            httpResultResponse.setCode(code);
            httpResultResponse.setMessage("空中下发成功");
            return httpResultResponse;
        } else {
            System.out.println("已达到最大重试次数 (" + MAX_RETRY_COUNT + ")，发送最终失败。");
            httpResultResponse.setCode(code);
            httpResultResponse.setMessage("空中下发失败");
            return httpResultResponse;
        }
    }

}
