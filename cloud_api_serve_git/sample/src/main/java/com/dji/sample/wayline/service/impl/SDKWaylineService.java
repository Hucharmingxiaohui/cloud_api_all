package com.dji.sample.wayline.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.center.utils.StringUtils;
import com.dji.sample.common.error.CommonErrorEnum;
import com.dji.sample.component.mqtt.model.EventsReceiver;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.IOssService;
import com.dji.sample.component.websocket.model.BizCodeEnum;
import com.dji.sample.component.websocket.service.IWebSocketMessageService;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.wind.config.WaylineUrlConfig;
import com.dji.sample.df.wind.dao.FanWaylinePointsMapper;
import com.dji.sample.df.wind.dao.WindTurbineMapper;
import com.dji.sample.df.wind.model.entity.FanWaylinePoints;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import com.dji.sample.df.wind.service.RoutePlanService;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.dao.IWorkspaceMapper;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sample.manage.model.entity.WorkspaceEntity;
import com.dji.sample.manage.model.enums.UserTypeEnum;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.media.dao.IFileMapper;
import com.dji.sample.media.model.MediaFileCountDTO;
import com.dji.sample.media.model.MediaFileEntity;
import com.dji.sample.media.service.IMediaRedisService;
import com.dji.sample.wayline.dao.IWaylineFileMapper;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.model.enums.WaylineJobStatusEnum;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sample.wayline.service.IWaylineJobService;
import com.dji.sample.wayline.service.IWaylineRedisService;
import com.dji.sdk.cloudapi.airsense.AirsenseWarning;
import com.dji.sdk.cloudapi.wayline.*;
import com.dji.sdk.cloudapi.wayline.api.AbstractWaylineService;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.mqtt.MqttReply;
import com.dji.sdk.mqtt.events.EventsDataRequest;
import com.dji.sdk.mqtt.events.TopicEventsRequest;
import com.dji.sdk.mqtt.events.TopicEventsResponse;
import com.dji.sdk.mqtt.requests.TopicRequestsRequest;
import com.dji.sdk.mqtt.requests.TopicRequestsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author sean
 * @version 1.7
 * @date 2023/7/7
 */
@Service
@Slf4j
public class SDKWaylineService extends AbstractWaylineService {

    @Autowired
    private IDeviceRedisService deviceRedisService;

    @Autowired
    private IWaylineRedisService waylineRedisService;

    @Autowired
    private IMediaRedisService mediaRedisService;

    @Autowired
    private IWebSocketMessageService webSocketMessageService;

    @Autowired
    private IWaylineJobService waylineJobService;

    @Autowired
    private IWaylineFileService waylineFileService;

    @Autowired
    private IWaylineJobMapper waylineJobMapper;

    @Autowired
    @Lazy
    private RoutePlanService routePlan;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private IDeviceMapper deviceMapper;

    @Resource
    private WindTurbineMapper windTurbineMapper;

    @Resource
    private IOssService ossService;

    @Resource
    IFileMapper fileMapper;

    @Autowired
    private IWorkspaceMapper workspaceMapper;

    @Autowired
    private WaylineUrlConfig waylineUrlConfig;

    @Autowired
    FanWaylinePointsMapper fanWaylinePointsMapper;

    @Autowired
    PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;

    @Autowired
    IWaylineFileMapper  waylineFileMapper;

    private final ConcurrentMap<String, AtomicInteger> processedWaypoints = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, AtomicInteger> processedinWaypoints = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, AtomicInteger> processedFlyTo = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Boolean> processingFlags = new ConcurrentHashMap<>();

    // 1. 首先在类中定义Map来记录任务尝试次数
    private static final Map<String, Integer> taskAttempts = new ConcurrentHashMap<>();

    // 2. 添加最大尝试次数的常量
    private static final int MAX_ATTEMPTS = 5;

    @Override
    public TopicEventsResponse<MqttReply> deviceExitHomingNotify(TopicEventsRequest<DeviceExitHomingNotify> request, MessageHeaders headers) {
        return super.deviceExitHomingNotify(request, headers);
    }

    @Override
    public TopicEventsResponse<MqttReply> flighttaskProgress(TopicEventsRequest<EventsDataRequest<FlighttaskProgress>> response, MessageHeaders headers) {
        EventsReceiver<FlighttaskProgress> eventsReceiver = new EventsReceiver<>();
        eventsReceiver.setResult(response.getData().getResult());
        eventsReceiver.setOutput(response.getData().getOutput());
        eventsReceiver.setBid(response.getBid());
        eventsReceiver.setSn(response.getGateway());

        FlighttaskProgress output = eventsReceiver.getOutput();
        log.info("Task progress: {}", output.getProgress().toString());
        if (!eventsReceiver.getResult().isSuccess()) {
            log.error("Task progress ===> Error: " + eventsReceiver.getResult());
        }

        Optional<DeviceDTO> deviceOpt = deviceRedisService.getDeviceOnline(response.getGateway());
        if (deviceOpt.isEmpty()) {
            return new TopicEventsResponse<>();
        }

        FlighttaskStatusEnum statusEnum = output.getStatus();

        waylineRedisService.setRunningWaylineJob(response.getGateway(), eventsReceiver);

        Integer currentWaypointIndex = response.getData().getOutput().getExt().getCurrentWaypointIndex();
        String flightId = response.getData().getOutput().getExt().getFlightId();

        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId, flightId));
        WaylineFileEntity waylineFileEntity = waylineFileMapper.selectOne(new LambdaQueryWrapper<WaylineFileEntity>().
                eq(WaylineFileEntity::getWaylineId, waylineJobEntity.getFileId()));
        String name = waylineFileEntity.getName();
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
        log.info("执行普通航线任务-"+output.getExt().getFlightId()+"  当前航点号为"+currentWaypointIndex+"号");
//      判断为风机任务，只有第一个风机的top会走普通航线，其他都是空中航线
        if(pubWaylineJobPlanDfEntity.getPlanType()==1){
            // 快速检查航点是否变化
            AtomicInteger previous = processedWaypoints.get(flightId);
            if (!(previous != null && previous.get() >= currentWaypointIndex)) {
                if(currentWaypointIndex == 2){
                if (processingFlags.putIfAbsent(flightId + "_" + currentWaypointIndex, true) == null) {
                    try {
                        log.info("接受顶端航线:" + flightId + "接受航点: " + currentWaypointIndex);
                        String url = waylineUrlConfig.getWaylineStateUrl();
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("waylineType", "flightTask");
                        jsonObject.put("status", statusEnum);
                        jsonObject.put("wayPoint", currentWaypointIndex);
                        jsonObject.put("flightName", name);
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
                                log.info("收到顶端方法返回参数----" + jsonResponse);
//                              ypcx不为空代表为顶端航线，会返回偏航角，下发指令飞行至中心点
                                if (jsonResponse.getString("desc").equals("1")) {
                                    if (jsonResponse.getString("ypcx") != null && jsonResponse.getString("ypcx") != "") {
                                        String turbineName = jsonResponse.getString("turbine_name");
                                        String yaw = jsonResponse.getString("ypcx");
                                        redisUtils.set("ypcx",yaw);
                                        double value = Double.parseDouble(yaw);
                                        WindTurbine windTurbine = windTurbineMapper.selectOne(new LambdaQueryWrapper<WindTurbine>().eq(WindTurbine::getTurbineName, turbineName));
                                        windTurbine.setApproachYaw(value);
                                        windTurbineMapper.updateById(windTurbine);
                                        log.info("开始执行飞向中心点----");
                                        routePlan.flyToWayline(turbineName, value);
                                        log.info("开始执行飞向中心点结束----");
                                        redisUtils.set("in_fight_state", "flyto");
                                        //存redis,为当前任务的风机名
                                        redisUtils.set("turbineName", turbineName);
                                    }
                                }else if (jsonResponse.getString("desc").equals("0")) {
//                                  分析失败返航
                                    log.info("顶端分析失败返航-----");
                                    this.returnHome(SDKManager.getDeviceSDK(pubWaylineJobPlanDfEntity.getDockSn()));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } finally {
                        processingFlags.remove(flightId + "_" + currentWaypointIndex);
                    }
                  }
                }
                processedWaypoints.put(flightId, new AtomicInteger(currentWaypointIndex));
            }
        }
        int videoPointNum=0;
        FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
                .eq(FanWaylinePoints::getJobId, redisUtils.get("jobId"))
                .orderByDesc(FanWaylinePoints::getId)
                .last("LIMIT 1"));
        if(fanWaylinePoints!=null){
            Integer jobType = fanWaylinePoints.getJobType();
            if (jobType == 1 && fanWaylinePoints.getVideoFanPoints()!=null) {
//                videoPoints  可能是null
                JSONArray videoPoints = JSON.parseArray(fanWaylinePoints.getVideoFanPoints());
                videoPointNum = videoPoints.size();
            }
        }

        if (statusEnum.isEnd()) {
            WaylineJobDTO job = WaylineJobDTO.builder()
                    .jobId(response.getBid())
                    .status(WaylineJobStatusEnum.SUCCESS.getVal())
                    .completedTime(LocalDateTime.now())
                    .mediaCount(output.getExt().getMediaCount()+videoPointNum)
                    .build();

            // record the update of the media count.
            if (Objects.nonNull(job.getMediaCount()) && job.getMediaCount() != 0) {
                mediaRedisService.setMediaCount(response.getGateway(), job.getJobId(),
                        MediaFileCountDTO.builder().deviceSn(deviceOpt.get().getChildDeviceSn())
                                .jobId(response.getBid()).mediaCount(job.getMediaCount()).uploadedCount(0).build());
            }

            if (FlighttaskStatusEnum.OK != statusEnum) {
                job.setCode(eventsReceiver.getResult().getCode());
                job.setStatus(WaylineJobStatusEnum.FAILED.getVal());
            }
            waylineJobService.updateJob(job);
            waylineRedisService.delRunningWaylineJob(response.getGateway());
            waylineRedisService.delPausedWaylineJob(response.getBid());
        }

        webSocketMessageService.sendBatch(deviceOpt.get().getWorkspaceId(), UserTypeEnum.WEB.getVal(),
                BizCodeEnum.FLIGHT_TASK_PROGRESS.getCode(), eventsReceiver);

        return new TopicEventsResponse<>();
    }

    //  空中航线接收mqtt消息
    @Override
    public TopicEventsResponse<MqttReply> inFlighttaskProgress(TopicEventsRequest<InFlighttaskProgress> response, MessageHeaders headers) {

        log.info("Task progress: {}", response.getData().getProgress().toString());
        if (response.getData().getResult()!=0) {
            log.error("Task progress ===> ErrorCode: " + response.getData().getResult());
        }

        Integer status = response.getData().getStatus();
        Integer currentWaypointIndex = response.getData().getWayPointIndex();
        String flightId = response.getData().getInFlightWaylineId();

        WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());
        String workspaceId = workspaceEntity.getWorkspaceId();

        log.info("执行空中航线任务-"+flightId+"  当前航点号为"+currentWaypointIndex+"号");
//      如果为空且当前航电为0，则再次下发
        if(flightId.isEmpty() && currentWaypointIndex==0){
            String jobId = redisUtils.get("jobId").toString();
            // 检查该任务是否已超过最大尝试次数
            if (isMaxAttemptsExceeded(jobId)) {
                log.warn("任务{}尝试次数已达上限{}次，停止下发，执行返航", jobId, MAX_ATTEMPTS);
                // 执行返航操作
                WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>().
                        eq(WaylineJobEntity::getJobId, jobId));
                PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                        .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
                this.returnHome(SDKManager.getDeviceSDK(pubWaylineJobPlanDfEntity.getDockSn()));
                // 清理该任务的记录
                taskAttempts.remove(jobId);
                return new TopicEventsResponse<>();
            }
            // 增加尝试次数
            incrementAttemptCount(jobId);
            log.info("空中下发失败。第{}次尝试下发-----------", getAttemptCount(jobId));
            String inFightState = redisUtils.get("in_fight_state").toString();
            String turbineName = redisUtils.get("turbineName").toString();
            if(inFightState.equals("working")){
                routePlan.workingWayline(turbineName,null);
            }else if(inFightState.equals("stop")){
                String ypjd = redisUtils.get("ypjd").toString();
                double value = Double.parseDouble(ypjd);
                routePlan.stopWayline(turbineName, value,null);
            }else if(inFightState.equals("flyto")){
                String ypcx = redisUtils.get("ypcx").toString();
                double value = Double.parseDouble(ypcx);
                routePlan.flyToWayline(turbineName, value);
            }else {
//               什么都不做
            }
            return new TopicEventsResponse<>();
        }
        String fightState = redisUtils.get("in_fight_state").toString();
//        String rthParam1 = redisUtils.get("rthParam").toString();
////      如果是停机/不停机航线，并且任务完成，且返航标志位为0,则进行下一个风机top航线下发
//        if(("working".equals(fightState) ||"stop".equals(fightState)) && "0".equals(rthParam1) && status == 6){
//            String turbineName = redisUtils.get("turbineName").toString();
//            if (StringUtils.isNotEmpty(turbineName) && !"noNextTurbine".equals(turbineName)) {
////              下发top空中航线
//                String windTurbineId = redisUtils.get("windTurbineId").toString();
//                routePlan.nextTopWayline(windTurbineId);
//            }
//        }
//      如果为多风机连续飞，要加风机策略标志位进行 &&
        if("working".equals(fightState)){
            processedinWaypoints.compute(flightId, (key, current) -> {
                if (current == null || current.get() < currentWaypointIndex) {
                    log.info("接受不停机空中航线:" + flightId + "接受航点: " + currentWaypointIndex);
                    String jobId = redisUtils.get("jobId").toString();
//                  航点大于1，说明成功则移除此任务
                    if(currentWaypointIndex>1){
                        resetAttemptCount(jobId);
                    }
                    String url = waylineUrlConfig.getWaylineStateUrl();
                    JSONObject jsonObject = new JSONObject();
                    String turbineName = redisUtils.get("turbineName").toString();
                    WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>().
                            eq(WaylineJobEntity::getJobId, jobId));
                    PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                            .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
                    String windTurbineId = pubWaylineJobPlanDfEntity.getFanId();
                    WindTurbine windTurbine = windTurbineMapper.selectById(windTurbineId);
                    jsonObject.put("waylineType", "inFlightTask");
                    jsonObject.put("status", status);
                    jsonObject.put("wayPoint", currentWaypointIndex);
                    jsonObject.put("flightName", turbineName+"working");
                    jsonObject.put("jobId", jobId);
                    jsonObject.put("blade_points", windTurbine.getBladePoints());
                    jsonObject.put("tower_points", windTurbine.getTowerPoints());
                    String jsonInput = jsonObject.toString();
                    try {
                        URL obj = new URL(url);
                        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                        // 设置请求方法
                        con.setRequestMethod("POST");
                        con.setRequestProperty("Content-Type", "application/json");
                        con.setDoOutput(true);
                        // 发送请求
                        try(OutputStream os = con.getOutputStream()) {
                            byte[] input = jsonInput.getBytes("utf-8");
                            os.write(input, 0, input.length);
                        }
                        // 获取响应
                        int responseCode = con.getResponseCode();
                        System.out.println("Response Code: " + responseCode);

                        try(BufferedReader br = new BufferedReader(
                                new InputStreamReader(con.getInputStream(), "utf-8"))) {
                            StringBuilder response1 = new StringBuilder();
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response1.append(responseLine.trim());
                            }
                            JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
                            log.info("收到不停机状态返回参数----" + jsonResponse);

                            if (jsonResponse.getString("desc").equals("1")) {
                                if (jsonResponse.getString("file_path") != null) {
//                                  处理抓拍图片
                                    String filePath = jsonResponse.getString("file_path");
                                    JSONArray imageList = jsonResponse.getJSONArray("imageList");
                                    for (int i = 0; i < imageList.size(); i++) {
                                        try {
                                            File checkFile = new File(filePath + imageList.getString(i));
                                            if (!checkFile.exists()) {
                                                log.warn("文件不存在: {}", filePath + imageList.getString(i));
                                                continue;  // 跳过这个文件
                                            }
                                            if (!checkFile.isFile()) {
                                                log.warn("路径不是文件: {}", filePath + imageList.getString(i));
                                                continue;
                                            }
                                            if (checkFile.length() == 0) {
                                                log.warn("文件为空: {}", filePath + imageList.getString(i));
                                                continue;
                                            }
                                            MultipartFile file = convert(checkFile);
                                            // 先检查文件是否存在且是文件（不是目录）
                                            log.info("保存文件视频截图文件---"+file.getOriginalFilename());
                                            String ObjectKey= OssConfiguration.objectDirPrefix + "/" + jobId + "/" +file.getOriginalFilename();;
                                            ossService.putObject(OssConfiguration.bucket, ObjectKey, file.getInputStream());
                                            MediaFileEntity  mediaFileEntity = new MediaFileEntity();
                                            mediaFileEntity.setFileId(UUID.randomUUID().toString());
                                            mediaFileEntity.setFileName(file.getOriginalFilename());
                                            mediaFileEntity.setFilePath(OssConfiguration.objectDirPrefix + "/" + jobId);
                                            mediaFileEntity.setObjectKey(ObjectKey);
                                            mediaFileEntity.setJobId(jobId);
                                            mediaFileEntity.setWorkspaceId(workspaceId);
                                            DeviceEntity dockEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDeviceSn, pubWaylineJobPlanDfEntity.getDockSn()));
                                            mediaFileEntity.setDrone(dockEntity.getDeviceSn());
//                                          负载暂时不写
                                            mediaFileEntity.setPayload(null);
                                            mediaFileEntity.setIsOriginal(true);
                                            log.info("插入文件入库---"+mediaFileEntity);
                                            fileMapper.insert(mediaFileEntity);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    log.info("开始执行保存点位----");
//                                  截图点位入库
                                    FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
                                            .eq(FanWaylinePoints::getJobId, jobId));
                                    for (int i = 0; i < imageList.size(); i++) {
                                        String fileName = imageList.getString(i);
                                        imageList.set(i, fileName.replace(".jpg", ""));
                                    }
                                    JSONArray jsonArray = new JSONArray();
                                    String videoFanPoints = fanWaylinePoints.getVideoFanPoints();
                                    if (videoFanPoints != null) {
                                        JSONArray videoPoints = JSON.parseArray(videoFanPoints);
                                        jsonArray.addAll(videoPoints);
                                        jsonArray.addAll(imageList);
                                    }else {
                                        jsonArray.addAll(imageList);
                                    }
                                    fanWaylinePoints.setVideoFanPoints(jsonArray.toJSONString());
                                    fanWaylinePointsMapper.updateById(fanWaylinePoints);
 //                                 保存job的保存标志位，正面保存完是1，反面保存完是2  注释掉因为不存在分析慢的问题
//                                    String segment = jsonResponse.getString("segment");
//                                    if(segment.equals("front")){
//                                        waylineJobEntity.setIsSaved(1);
//                                        waylineJobMapper.updateById(waylineJobEntity);
//                                    }else  if(segment.equals("back")){
//                                        waylineJobEntity.setIsSaved(2);
//                                        waylineJobMapper.updateById(waylineJobEntity);
//                                    }
                                    log.info("截图点位入库---"+fanWaylinePoints);
                                    log.info("处理返回图像---------------------");
                                }
                            } else if (jsonResponse.getString("desc").equals("0")) {
//                              分析失败返航
                                log.info("不停机分析失败返航-----");
                                this.returnHome(SDKManager.getDeviceSDK(pubWaylineJobPlanDfEntity.getDockSn()));
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return new AtomicInteger(currentWaypointIndex);
                }
                return current;
            });
        }else if("flyto".equals(fightState)){
            processedFlyTo.compute(flightId, (key, current) -> {
                if (current == null) {
                    if(currentWaypointIndex == 2){
                        log.info("接受中心点空中航线:" + flightId + "接受航点: " + currentWaypointIndex);
                        String url = waylineUrlConfig.getWaylineStateUrl();
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
                        jsonObject.put("wayPoint", currentWaypointIndex);
                        String jsonInput = jsonObject.toString();
                        log.info("中心点发送参数：");
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
                                log.info("接受中心点分析返回参数---"+jsonResponse.toString());
//                                String jobId = redisUtils.get("jobId").toString();
//                                WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
//                                        .eq(WaylineJobEntity::getJobId, jobId));
//                                PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
//                                        .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
//                                Integer strategyType = pubWaylineJobPlanDfEntity.getStrategyType();
//                                List<String> fanIdList=new ArrayList<>();
//                                if(strategyType==1){
//                                    fanIdList = pubWaylineJobPlanDfEntity.getFanIdList();
//                                }else {
//                                    fanIdList.add(pubWaylineJobPlanDfEntity.getFanId());
//                                }
//                                String lastFanId = fanIdList.get(fanIdList.size() - 1);
//                                WindTurbine lastFanIdwindTurbine = windTurbineMapper.selectById(lastFanId);
////                              返航标志位，1返航 0不返航（也是风机位次标志位，1是最后一个风机 0不是最后一个风机）,注意是string类型！！！
//                                String rthParam = "1";
                                if (jsonResponse.getString("desc").equals("1")) {
                                    if (jsonResponse.getString("yppd") != null || jsonResponse.getString("ypjd") != null) {
                                        if (jsonResponse.getString("yppd") != null) {
//                                          如果是最后一个风机，就返航，如果不是就不返航，测试先都不返航
                                            //不停机巡检
                                            log.info("执行不停机巡检-------------");
                                            turbineName = jsonResponse.getString("turbine_name");

//                                            if(lastFanIdwindTurbine.getTurbineName().equals(turbineName)){
//                                                rthParam = "1";
//                                                redisUtils.set("turbineName", "noNextTurbine");
//                                            }else {
//                                                rthParam = "0";
//                                                for (int i=0;i<fanIdList.size();i++) {
//                                                    WindTurbine nowFanIdwindTurbine = windTurbineMapper.selectById(fanIdList.get(i));
//                                                    if(nowFanIdwindTurbine.getTurbineName().equals(turbineName)) {
//                                                        WindTurbine nextTurbine = windTurbineMapper.selectById(fanIdList.get(i+1));
//                                                        redisUtils.set("turbineName", nextTurbine.getTurbineName());
//                                                        redisUtils.set("windTurbineId",nextTurbine.getId());
//                                                    }
//                                                }
//                                            }
//                                            redisUtils.set("rthParam",rthParam);
                                            routePlan.workingWayline(turbineName,null);
                                        } else if (jsonResponse.getString("ypjd") != null) {
                                            //停机巡检
                                            log.info("执行停机巡检-------------");
                                            turbineName = jsonResponse.getString("turbine_name");

//                                            if(lastFanIdwindTurbine.getTurbineName().equals(turbineName)){
//                                                rthParam = "1";
//                                                redisUtils.set("turbineName", "noNextTurbine");
//                                            }else {
//                                                rthParam = "0";
//                                                for (int i=0;i<fanIdList.size();i++) {
//                                                    WindTurbine nowFanIdwindTurbine = windTurbineMapper.selectById(fanIdList.get(i));
//                                                    if(nowFanIdwindTurbine.getTurbineName().equals(turbineName)) {
//                                                        WindTurbine nextTurbine = windTurbineMapper.selectById(fanIdList.get(i+1));
//                                                        redisUtils.set("turbineName", nextTurbine.getTurbineName());
//                                                        redisUtils.set("windTurbineId",nextTurbine.getId());
//                                                    }
//                                                }
//                                            }
                                            String ypjd = jsonResponse.getString("ypjd");
                                            double value = Double.parseDouble(ypjd);
                                            redisUtils.set("ypjd",ypjd);
//                                            redisUtils.set("rthParam",rthParam);
                                            routePlan.stopWayline(turbineName, value,null);
                                        }
                                    }
                                } else {
                                    //分析失败返航
                                    log.info("巡视类型分析失败返航-----");
                                    String jobId1 = redisUtils.get("jobId").toString();
                                    WaylineJobEntity waylineJobEntity1 = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                                            .eq(WaylineJobEntity::getJobId, jobId1));
                                    PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity1 = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                                            .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity1.getPlanId()));
                                    this.returnHome(SDKManager.getDeviceSDK(pubWaylineJobPlanDfEntity1.getDockSn()));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return new AtomicInteger(currentWaypointIndex);
                    }
                }
                return current;
            });
        }else if("stop".equals(fightState)){
            processedinWaypoints.compute(flightId, (key, current) -> {
                if (current == null || current.get() < currentWaypointIndex) {
                    log.info("接受停机空中航线:" + flightId + "接受航点: " + currentWaypointIndex);
                    String jobId = redisUtils.get("jobId").toString();
//                  航点大于1，说明成功则移除此任务
                    if(currentWaypointIndex>1){
                        resetAttemptCount(jobId);
                    }
                    return new AtomicInteger(currentWaypointIndex);
                }
                return current;
            });
        }

        return new TopicEventsResponse<>();
    }
    /**
     * 检查任务是否超过最大尝试次数
     */
    private boolean isMaxAttemptsExceeded(String jobId) {
        if (jobId == null || jobId.isEmpty()) {
            return false; // 没有jobId则不受限制
        }

        Integer attempts = taskAttempts.get(jobId);
        return attempts != null && attempts >= MAX_ATTEMPTS;
    }

    /**
     * 获取任务尝试次数
     */
    private int getAttemptCount(String jobId) {
        if (jobId == null || jobId.isEmpty()) {
            return 0;
        }
        return taskAttempts.getOrDefault(jobId, 0);
    }

    /**
     * 增加任务尝试次数
     */
    private void incrementAttemptCount(String jobId) {
        if (jobId == null || jobId.isEmpty()) {
            return;
        }

        taskAttempts.compute(jobId, (key, count) -> {
            if (count == null) {
                return 1;
            }
            return count + 1;
        });

        log.debug("任务{}尝试次数增加至: {}", jobId, taskAttempts.get(jobId));
    }

    /**
     * 重置任务尝试次数（任务成功时调用）
     */
    public void resetAttemptCount(String jobId) {
        if (jobId != null && !jobId.isEmpty()) {
            Integer i = taskAttempts.get(jobId);
            if (i != null) {
                taskAttempts.remove(jobId);
            }
            log.info("任务{}尝试次数已重置", jobId);
        }
    }


    public static MultipartFile convert(File file) throws IOException {
        return new CustomMultipartFile(
                file.getName(), file
        );
    }

    static class CustomMultipartFile implements MultipartFile {
        private final String name;
        private final File file;
        private FileInputStream fis;

        public CustomMultipartFile(String name, File file) throws IOException {
            this.name = name;
            this.file = file;
            this.fis = new FileInputStream(file);
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return null; }
        @Override public boolean isEmpty() { return file.length() == 0; }
        @Override public long getSize() { return file.length(); }

        @Override
        public byte[] getBytes() throws IOException {
            return new byte[0];
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return fis; // 直接返回文件流
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.copy(fis, dest.toPath());
        }
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Override
    public TopicRequestsResponse<MqttReply<FlighttaskResourceGetResponse>> flighttaskResourceGet(TopicRequestsRequest<FlighttaskResourceGetRequest> response, MessageHeaders headers) {
        String jobId = response.getData().getFlightId();

        Optional<DeviceDTO> deviceOpt = deviceRedisService.getDeviceOnline(response.getGateway());
        if (deviceOpt.isEmpty()) {
            log.error("The device is offline, please try again later.");
            return new TopicRequestsResponse().setData(MqttReply.error(CommonErrorEnum.DEVICE_OFFLINE));
        }
        Optional<WaylineJobDTO> waylineJobOpt = waylineJobService.getJobByJobId(deviceOpt.get().getWorkspaceId(), jobId);
        if (waylineJobOpt.isEmpty()) {
            log.error("The wayline job does not exist.");
            return new TopicRequestsResponse().setData(MqttReply.error(CommonErrorEnum.ILLEGAL_ARGUMENT));
        }

        WaylineJobDTO waylineJob = waylineJobOpt.get();

        // get wayline file
        Optional<GetWaylineListResponse> waylineFile = waylineFileService.getWaylineByWaylineId(waylineJob.getWorkspaceId(), waylineJob.getFileId());
        if (waylineFile.isEmpty()) {
            log.error("The wayline file does not exist.");
            return new TopicRequestsResponse().setData(MqttReply.error(CommonErrorEnum.ILLEGAL_ARGUMENT));
        }
        // get file url
        try {
            URL url = waylineFileService.getObjectUrl(waylineJob.getWorkspaceId(), waylineFile.get().getId());
            return new TopicRequestsResponse<MqttReply<FlighttaskResourceGetResponse>>().setData(
                    MqttReply.success(new FlighttaskResourceGetResponse()
                            .setFile(new FlighttaskFile()
                                    .setUrl(url.toString())
                                    .setFingerprint(waylineFile.get().getSign()))));
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
            return new TopicRequestsResponse().setData(MqttReply.error(CommonErrorEnum.SYSTEM_ERROR));
        }
    }

    @Override
    public TopicEventsResponse<MqttReply> returnHomeInfo(TopicEventsRequest<ReturnHomeInfo> request, MessageHeaders headers)  {
        // 空实现，只记录日志，不抛出异常
        log.debug("重写返航信息方法----");

        // 返回空的成功响应
        return new TopicEventsResponse<MqttReply>()
                .setData(MqttReply.success());
    }
}
