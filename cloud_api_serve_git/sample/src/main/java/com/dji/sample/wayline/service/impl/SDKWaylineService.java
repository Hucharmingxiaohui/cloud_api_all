package com.dji.sample.wayline.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.common.error.CommonErrorEnum;
import com.dji.sample.component.mqtt.model.EventsReceiver;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.IOssService;
import com.dji.sample.component.redis.RedisConst;
import com.dji.sample.component.redis.RedisOpsUtils;
import com.dji.sample.component.websocket.model.BizCodeEnum;
import com.dji.sample.component.websocket.service.IWebSocketMessageService;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.windDf.config.WaylineUrlConfig;
import com.dji.sample.df.windDf.dao.FanWaylinePointsMapper;
import com.dji.sample.df.windDf.dao.WindTurbineMapper;
import com.dji.sample.df.windDf.model.entity.FanWaylinePoints;
import com.dji.sample.df.windDf.model.entity.WindTurbine;
import com.dji.sample.df.uavCommonHandleDf.service.RoutePlanService;
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
import com.dji.sdk.cloudapi.device.DockModeCodeEnum;
import com.dji.sdk.cloudapi.device.OsdDock;
import com.dji.sdk.cloudapi.wayline.*;
import com.dji.sdk.cloudapi.wayline.api.AbstractWaylineService;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.mqtt.MqttReply;
import com.dji.sdk.mqtt.ChannelName;
import com.dji.sdk.mqtt.events.EventsDataRequest;
import com.dji.sdk.mqtt.events.TopicEventsRequest;
import com.dji.sdk.mqtt.events.TopicEventsResponse;
import com.dji.sdk.mqtt.requests.TopicRequestsRequest;
import com.dji.sdk.mqtt.requests.TopicRequestsResponse;
import com.dji.sdk.mqtt.services.ServicesReplyData;
import com.dji.sdk.mqtt.services.TopicServicesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.integration.annotation.ServiceActivator;
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

    /**
     * 蛙跳 Redis key 保留时长：任务正常结束后无需残留，加 TTL 防止旧任务 key 污染新任务的判断。
     */
    private static final long FROG_JUMP_KEY_TTL_SECONDS = 2 * 60 * 60L;

    @Value("${wayline.frog-jump.progress-stale-timeout-seconds:120}")
    private long frogJumpProgressStaleTimeoutSeconds;

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
//        log.info("Task progress: {}", output.getProgress().toString());
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
        log.info("Flighttask progress detail: gateway={}, flightId={}, status={}, currentWaypointIndex={}, result={}, ext={}",
                response.getGateway(), flightId, statusEnum, currentWaypointIndex, eventsReceiver.getResult(), JSON.toJSONString(output.getExt()));
//      如果是蛙跳任务就保存progress到redis
        saveFrogJumpDockProgress(response.getGateway(), flightId, output);
//      蛙跳降落机场可能入舱断连后不再上报，仍在上报的一方需要兜底检查对端是否卡在降落收尾阶段
        stopCurrentDockIfPeerLandingProgressStale(response.getGateway(), flightId, output);
//      上报状态是否结束标志（status 终态或任务状态机 WAYLINE_END）
        boolean taskEnd = statusEnum.isEnd() || isFrogJumpMissionStateEnd(response.getGateway(), flightId, output);
//      如果结束状态则发送任务结束命令给另一条机场
        notifyFrogJumpPeerStopIfEnd(response.getGateway(), flightId, statusEnum, taskEnd);

        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId, flightId));
        WaylineFileEntity waylineFileEntity = waylineFileMapper.selectOne(new LambdaQueryWrapper<WaylineFileEntity>().
                eq(WaylineFileEntity::getWaylineId, waylineJobEntity.getFileId()));
        String name = waylineFileEntity.getName();
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
        log.info("正在执行普通航线任务："+output.getExt().getFlightId()+"，当前航点号为"+currentWaypointIndex+"号");
//      判断为风机任务，只有第一个风机的top会走普通航线，其他都是空中航线
        if(pubWaylineJobPlanDfEntity!=null && pubWaylineJobPlanDfEntity.getPlanType()==1){
            // 快速检查航点是否变化
            AtomicInteger previous = processedWaypoints.get(flightId);
            if (!(previous != null && previous.get() >= currentWaypointIndex)) {
                if(currentWaypointIndex == 2){
                if (processingFlags.putIfAbsent(flightId + "_" + currentWaypointIndex, true) == null) {
                    try {
                        log.info("正在执行风机顶端航线:" + flightId + "，当前航点号为: " + currentWaypointIndex);
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

                            try (BufferedReader br = new BufferedReader(
                                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                                StringBuilder response1 = new StringBuilder();
                                String responseLine;
                                while ((responseLine = br.readLine()) != null) {
                                    response1.append(responseLine.trim());
                                }
                                JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
                                log.info("收到风机顶端算法返回参数：" + jsonResponse);
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
                                        log.info("无人机开始执行飞向风机中心点...");
                                        routePlan.flyToWayline(turbineName, value);
//                                        log.info("无人机执行飞向风机中心点结束");
                                        redisUtils.set("in_fight_state", "flyto");
                                        //存redis,为当前任务的风机名
                                        redisUtils.set("turbineName", turbineName);
                                    }
                                }else if (jsonResponse.getString("desc").equals("0")) {
//                                  分析失败返航
                                    log.info("风机顶端算法分析失败执行返航...");
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

        if (taskEnd) {
            Integer mediaCount = output.getExt().getMediaCount();
            WaylineJobDTO job = WaylineJobDTO.builder()
                    .jobId(response.getBid())
                    .status(WaylineJobStatusEnum.SUCCESS.getVal())
                    .completedTime(LocalDateTime.now())
                    .mediaCount((mediaCount == null ? 0 : mediaCount) + videoPointNum)
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

//        log.info("Task progress: {}", response.getData().getProgress().toString());
        if (response.getData().getResult()!=0) {
            log.error("Task progress ===> ErrorCode: " + response.getData().getResult());
        }

        Integer status = response.getData().getStatus();
        Integer currentWaypointIndex = response.getData().getWayPointIndex();
        String flightId = response.getData().getInFlightWaylineId();

        WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());
        String workspaceId = workspaceEntity.getWorkspaceId();

        log.info("正在执行空中航线任务："+flightId+"  当前航点号为"+currentWaypointIndex+"号");
//      如果为空且当前航电为0，则再次下发
        if(flightId.isEmpty() && currentWaypointIndex==0){
            String jobId = redisUtils.get("jobId").toString();
            // 检查该任务是否已超过最大尝试次数
            if (isMaxAttemptsExceeded(jobId)) {
                log.warn("空中航线任务{}尝试次数已达上限{}次，停止下发，执行返航", jobId, MAX_ATTEMPTS);
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
            log.info("空中下发航线任务失败。第{}次尝试下发-----------", getAttemptCount(jobId));
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
        if("working".equals(fightState)){
            processedinWaypoints.compute(flightId, (key, current) -> {
                if (current == null || current.get() < currentWaypointIndex) {
                    log.info("正在执行风机不停机空中航线任务:" + flightId + "当前航点号为: " + currentWaypointIndex);
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
                        try(BufferedReader br = new BufferedReader(
                                new InputStreamReader(con.getInputStream(), "utf-8"))) {
                            StringBuilder response1 = new StringBuilder();
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response1.append(responseLine.trim());
                            }
                            JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
//                          此块变成开放接口分析服务那边进行调用
                            log.info("收到风机不停机视频算法返回数据：" + jsonResponse);
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
                                            log.info("风机不停机保存视频截图文件："+file.getOriginalFilename());
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
                                            log.info("风机不停机插入截图文件入库："+mediaFileEntity);
                                            fileMapper.insert(mediaFileEntity);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    log.info("风机不停机开始执行保存点位逻辑...");
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
                                    log.info("风机不停机执行保存点位逻辑结束");
                                }
                            } else if (jsonResponse.getString("desc").equals("0")) {
//                              分析失败返航
                                log.info("风机不停机视频算法分析失败返航...");
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
                        log.info("正在执行风机飞向中心点空中航线:" + flightId + "当前航点号为: " + currentWaypointIndex);
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

                            try (BufferedReader br = new BufferedReader(
                                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                                StringBuilder response1 = new StringBuilder();
                                String responseLine;
                                while ((responseLine = br.readLine()) != null) {
                                    response1.append(responseLine.trim());
                                }
                                JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
                                log.info("接受风机中心点算法分析返回参数："+jsonResponse.toString());
                                if (jsonResponse.getString("desc").equals("1")) {
                                    if (jsonResponse.getString("yppd") != null || jsonResponse.getString("ypjd") != null) {
                                        if (jsonResponse.getString("yppd") != null) {
                                            //不停机巡检
                                            log.info("无人机开始执行不停机巡检...");
                                            turbineName = jsonResponse.getString("turbine_name");
                                            routePlan.workingWayline(turbineName,null);
                                        } else if (jsonResponse.getString("ypjd") != null) {
                                            //停机巡检
                                            log.info("无人机开始执行停机巡检...");
                                            turbineName = jsonResponse.getString("turbine_name");
                                            String ypjd = jsonResponse.getString("ypjd");
                                            double value = Double.parseDouble(ypjd);
                                            redisUtils.set("ypjd",ypjd);
                                            routePlan.stopWayline(turbineName, value,null);
                                        }
                                    }
                                } else {
                                    //分析失败返航
                                    log.info("风机中心点算法分析失败返航...");
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
                    log.info("正在执行风机停机空中航线任务:" + flightId + "当前航点号为: " + currentWaypointIndex);
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
//            log.info("任务{}尝试次数已重置", jobId);
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

        // 返回空的成功响应
        return new TopicEventsResponse<MqttReply>()
                .setData(MqttReply.success());
    }

    @Override
    @ServiceActivator(inputChannel = ChannelName.INBOUND_REQUESTS_FLIGHTTASK_PROGRESS_GET, outputChannel = ChannelName.OUTBOUND_REQUESTS)
    public TopicRequestsResponse<MqttReply<FlighttaskProgressGetResponse>> flighttaskProgressGet(TopicRequestsRequest<FlighttaskProgressGetRequest> response, MessageHeaders headers) {
        String flightId = response.getData().getFlightId();
        Object pairValue = RedisOpsUtils.get(RedisConst.FROG_JUMP_TASK_PREFIX + flightId);
        if (pairValue == null) {
            log.warn("Flighttask progress get failed: frog jump pair not found, gateway={}, flightId={}", response.getGateway(), flightId);
            return new TopicRequestsResponse<MqttReply<FlighttaskProgressGetResponse>>().setData(MqttReply.error(CommonErrorEnum.ILLEGAL_ARGUMENT));
        }
        String peerDockSn = getProgressTargetSn(response);
        if (peerDockSn == null || peerDockSn.isBlank()) {
            peerDockSn = getFrogJumpPeerDockSn(response.getGateway(), String.valueOf(pairValue));
        }
        if (peerDockSn == null) {
            log.warn("Flighttask progress get failed: request gateway not in pair, gateway={}, flightId={}, pairValue={}",
                    response.getGateway(), flightId, pairValue);
            return new TopicRequestsResponse<MqttReply<FlighttaskProgressGetResponse>>().setData(MqttReply.error(CommonErrorEnum.ILLEGAL_ARGUMENT));
        }
        if (!isFrogJumpPairDock(peerDockSn, String.valueOf(pairValue))) {
            log.warn("Flighttask progress get failed: target sn not in pair, gateway={}, targetSn={}, flightId={}, pairValue={}",
                    response.getGateway(), peerDockSn, flightId, pairValue);
            return new TopicRequestsResponse<MqttReply<FlighttaskProgressGetResponse>>().setData(MqttReply.error(CommonErrorEnum.ILLEGAL_ARGUMENT));
        }
        FlighttaskProgress peerProgress = (FlighttaskProgress) RedisOpsUtils.get(getFrogJumpProgressKey(flightId, peerDockSn));
        if (peerProgress == null) {
            log.warn("Flighttask progress get failed: peer progress not found, gateway={}, peerDockSn={}, flightId={}",
                    response.getGateway(), peerDockSn, flightId);
            return new TopicRequestsResponse<MqttReply<FlighttaskProgressGetResponse>>().setData(MqttReply.error(CommonErrorEnum.ILLEGAL_ARGUMENT));
        }
        log.info("Flighttask progress get reply: gateway={}, peerDockSn={}, flightId={}, peerStatus={}, peerProgress={}",
                response.getGateway(), peerDockSn, flightId, peerProgress.getStatus(), peerProgress.getProgress());
        return new TopicRequestsResponse<MqttReply<FlighttaskProgressGetResponse>>().setData(MqttReply.success(new FlighttaskProgressGetResponse()
                .setFlightId(flightId)
                .setProgress(peerProgress.getProgress())
                .setStatus(peerProgress.getStatus())));
    }

    private String getProgressTargetSn(TopicRequestsRequest<FlighttaskProgressGetRequest> response) {
        if (response.getData().getTargetSn() != null && !response.getData().getTargetSn().isBlank()) {
            return response.getData().getTargetSn();
        }
        return response.getData().getSn();
    }

    private void saveFrogJumpDockProgress(String dockSn, String flightId, FlighttaskProgress progress) {
        Object pairValue = RedisOpsUtils.get(RedisConst.FROG_JUMP_TASK_PREFIX + flightId);
        if (pairValue == null || getFrogJumpPeerDockSn(dockSn, String.valueOf(pairValue)) == null) {
            return;
        }
        RedisOpsUtils.setWithExpire(getFrogJumpProgressKey(flightId, dockSn), progress, FROG_JUMP_KEY_TTL_SECONDS);
        RedisOpsUtils.setWithExpire(getFrogJumpProgressTimeKey(flightId, dockSn), System.currentTimeMillis(), FROG_JUMP_KEY_TTL_SECONDS);
    }

    private String getFrogJumpProgressKey(String flightId, String dockSn) {
        return RedisConst.FROG_JUMP_TASK_PREFIX + "progress" + RedisConst.DELIMITER + flightId + RedisConst.DELIMITER + dockSn;
    }

    private String getFrogJumpProgressTimeKey(String flightId, String dockSn) {
        return RedisConst.FROG_JUMP_TASK_PREFIX + "progress_time" + RedisConst.DELIMITER + flightId + RedisConst.DELIMITER + dockSn;
    }

    private String getFrogJumpPeerDockSn(String currentDockSn, String pairValue) {
        String[] dockSns = pairValue.split(RedisConst.DELIMITER);
        if (dockSns.length != 2) {
            return null;
        }
        if (currentDockSn.equals(dockSns[0])) {
            return dockSns[1];
        }
        if (currentDockSn.equals(dockSns[1])) {
            return dockSns[0];
        }
        return null;
    }

    private boolean isFrogJumpPairDock(String dockSn, String pairValue) {
        String[] dockSns = pairValue.split(RedisConst.DELIMITER);
        return dockSns.length == 2 && (dockSns[0].equals(dockSn) || dockSns[1].equals(dockSn));
    }
    /**
     * 判断蛙跳任务是否真正结束：只认降落机场的任务状态机终态 WAYLINE_END。
     * 起飞机场的 WAYLINE_END 只代表它已交出无人机/自己的航线段结束，任务仍由降落机场执行，
     * 不能视为整个任务结束，更不能因此给降落机场下发 stop（否则接机流程被杀、无人机悬空）。
     * DISCONNECT 在起飞前和执行中会反复出现（对频切换导致），不是结束信号，不能用判断结束。
     */
    private boolean isFrogJumpMissionStateEnd(String dockSn, String flightId, FlighttaskProgress progress) {
        if (flightId == null || progress == null || progress.getExt() == null
                || progress.getExt().getWaylineMissionState() != WaylineMissionStateEnum.WAYLINE_END) {
            return false;
        }
        Object pairValue = RedisOpsUtils.get(RedisConst.FROG_JUMP_TASK_PREFIX + flightId);
        if (pairValue == null || getFrogJumpPeerDockSn(dockSn, String.valueOf(pairValue)) == null) {
            return false;
        }
        if (!isFrogJumpLandingDock(dockSn, String.valueOf(pairValue))) {
            log.info("Frog jump takeoff dock wayline end ignored, mission continues on landing dock: flightId={}, dockSn={}, status={}",
                    flightId, dockSn, progress.getStatus());
            return false;
        }
        log.info("Frog jump task treated as ended because wayline mission ended: flightId={}, dockSn={}, status={}",
                flightId, dockSn, progress.getStatus());
        return true;
    }

    private void stopCurrentDockIfPeerLandingProgressStale(String currentDockSn, String flightId, FlighttaskProgress currentProgress) {
        if (flightId == null || currentProgress == null || currentProgress.getStatus() == null || currentProgress.getStatus().isEnd()) {
            return;
        }
        Object pairValue = RedisOpsUtils.get(RedisConst.FROG_JUMP_TASK_PREFIX + flightId);
        if (pairValue == null) {
            return;
        }
        String peerDockSn = getFrogJumpPeerDockSn(currentDockSn, String.valueOf(pairValue));
        if (peerDockSn == null) {
            return;
        }
        FlighttaskProgress peerProgress = (FlighttaskProgress) RedisOpsUtils.get(getFrogJumpProgressKey(flightId, peerDockSn));
        if (!isStaleLandingProgress(flightId, peerDockSn, peerProgress)) {
            return;
        }
        String stopKey = RedisConst.FROG_JUMP_TASK_PREFIX + "stale_stop" + RedisConst.DELIMITER + flightId + RedisConst.DELIMITER + currentDockSn;
        if (RedisOpsUtils.checkExist(stopKey)) {
            log.info("Frog jump stale peer fallback skipped because stop key exists: flightId={}, currentDockSn={}, peerDockSn={}, stopKey={}",
                    flightId, currentDockSn, peerDockSn, stopKey);
            return;
        }
        FlighttaskStopRequest request = new FlighttaskStopRequest()
                .setFlightId(flightId)
                .setReason(0);
        log.warn("Frog jump current dock stop because peer landing progress stale: flightId={}, currentDockSn={}, peerDockSn={}, timeoutSeconds={}, peerProgress={}",
                flightId, currentDockSn, peerDockSn, frogJumpProgressStaleTimeoutSeconds, JSON.toJSONString(peerProgress));
        try {
            TopicServicesResponse<ServicesReplyData> reply = flighttaskStop(SDKManager.getDeviceSDK(currentDockSn), request);
            boolean success = reply != null && reply.getData() != null && reply.getData().getResult() != null
                    && reply.getData().getResult().isSuccess();
            log.info("Frog jump stale peer fallback stop reply: flightId={}, currentDockSn={}, peerDockSn={}, result={}",
                    flightId, currentDockSn, peerDockSn, reply == null || reply.getData() == null ? null : reply.getData().getResult());
            if (success) {
                // 仅在 stop 成功后写入防重 key，失败时保留重试机会（下次 progress 上报会再次触发）
                RedisOpsUtils.setWithExpire(stopKey, true, FROG_JUMP_KEY_TTL_SECONDS);
            } else {
                log.warn("Frog jump stale peer fallback stop not success, stop key not set for retry: flightId={}, currentDockSn={}, peerDockSn={}",
                        flightId, currentDockSn, peerDockSn);
            }
        } catch (Exception e) {
            log.error("Frog jump stale peer fallback stop failed: flightId={}, currentDockSn={}, peerDockSn={}", flightId, currentDockSn, peerDockSn, e);
        }
    }

    private boolean isStaleLandingProgress(String flightId, String peerDockSn, FlighttaskProgress peerProgress) {
        if (peerProgress == null || peerProgress.getStatus() == null || peerProgress.getStatus().isEnd()
                || peerProgress.getProgress() == null || peerProgress.getExt() == null
                || !Objects.equals(flightId, peerProgress.getExt().getFlightId())) {
            return false;
        }
        // 对端机场已回到空闲，说明其任务状态机已收尾（对频切换后起飞机场任务会卡在 DISCONNECT，需要云端停止）
        DockModeCodeEnum peerDockMode = deviceRedisService.getDeviceOsd(peerDockSn, OsdDock.class)
                .map(OsdDock::getModeCode)
                .orElse(null);
        if (DockModeCodeEnum.IDLE != peerDockMode) {
            return false;
        }
        Object progressTimeValue = RedisOpsUtils.get(getFrogJumpProgressTimeKey(flightId, peerDockSn));
        Long progressTime = parseLong(progressTimeValue);
        if (progressTime == null) {
            return false;
        }
        long staleMillis = Math.max(frogJumpProgressStaleTimeoutSeconds, 1) * 1000;
        return System.currentTimeMillis() - progressTime >= staleMillis;
    }

    private Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && !((String) value).isBlank()) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 一方任务结束后通知对端停止。stop 方向必须区分角色：
     * - 降落机场结束：正常收尾路径，停止起飞机场（解除其 Working 卡死）；
     * - 起飞机场结束：若降落机场已上报 WAYLINE_END（无人机已交接、正在降落流程），绝不能停它，
     *   否则降落被中断、无人机悬空；仅在降落机场尚未接管（如起飞失败）时才停止它以避免干等。
     */
    private void notifyFrogJumpPeerStopIfEnd(String currentDockSn, String flightId, FlighttaskStatusEnum statusEnum, boolean taskEnd) {
        if (!taskEnd) {
            return;
        }
        Object pairValue = RedisOpsUtils.get(RedisConst.FROG_JUMP_TASK_PREFIX + flightId);
        if (pairValue == null) {
            return;
        }
        String peerDockSn = getFrogJumpPeerDockSn(currentDockSn, String.valueOf(pairValue));
        if (peerDockSn == null) {
            log.warn("Frog jump peer stop skipped: current dock not in pair, flightId={}, currentDockSn={}, pairValue={}", flightId, currentDockSn, pairValue);
            return;
        }
        boolean currentIsLanding = isFrogJumpLandingDock(currentDockSn, String.valueOf(pairValue));
        String skipReason = checkFrogJumpPeerStopAllowed(currentIsLanding, flightId, peerDockSn);
        if (skipReason != null) {
            log.info("Frog jump peer stop skipped: flightId={}, currentDockSn={}, currentIsLanding={}, peerDockSn={}, reason={}",
                    flightId, currentDockSn, currentIsLanding, peerDockSn, skipReason);
            return;
        }
        String stopKey = RedisConst.FROG_JUMP_TASK_PREFIX + "stop" + RedisConst.DELIMITER + flightId + RedisConst.DELIMITER + currentDockSn;
        if (RedisOpsUtils.checkExist(stopKey)) {
            log.info("Frog jump peer stop skipped because stop key exists: flightId={}, currentDockSn={}, peerDockSn={}, stopKey={}",
                    flightId, currentDockSn, peerDockSn, stopKey);
            return;
        }
        FlighttaskStopRequest request = new FlighttaskStopRequest()
                .setFlightId(flightId)
                .setReason(statusEnum == FlighttaskStatusEnum.OK ? 0 : 1);
        log.info("Frog jump peer stop request: flightId={}, currentDockSn={}, currentIsLanding={}, peerDockSn={}, status={}, reason={}",
                flightId, currentDockSn, currentIsLanding, peerDockSn, statusEnum, request.getReason());
        try {
            TopicServicesResponse<ServicesReplyData> reply = flighttaskStop(SDKManager.getDeviceSDK(peerDockSn), request);
            boolean success = reply != null && reply.getData() != null && reply.getData().getResult() != null
                    && reply.getData().getResult().isSuccess();
            log.info("Frog jump peer stop reply: flightId={}, peerDockSn={}, result={}",
                    flightId, peerDockSn, reply == null || reply.getData() == null ? null : reply.getData().getResult());
            if (success) {
                // 仅在 stop 成功后写入防重 key，失败时保留重试机会（下次 progress 上报会再次触发）
                RedisOpsUtils.setWithExpire(stopKey, true, FROG_JUMP_KEY_TTL_SECONDS);
            } else {
                log.warn("Frog jump peer stop not success, stop key not set for retry: flightId={}, currentDockSn={}, peerDockSn={}",
                        flightId, currentDockSn, peerDockSn);
            }
        } catch (Exception e) {
            log.error("Frog jump peer stop failed: flightId={}, currentDockSn={}, peerDockSn={}", flightId, currentDockSn, peerDockSn, e);
        }
    }

    /**
     * pair 顺序为 takeoff:landing（见 saveFrogJumpTaskPair），判断指定机场是否为降落机场。
     */
    private boolean isFrogJumpLandingDock(String dockSn, String pairValue) {
        String[] dockSns = pairValue.split(RedisConst.DELIMITER);
        return dockSns.length == 2 && dockSns[1].equals(dockSn);
    }

    /**
     * 返回 null 表示允许对对端下发 stop，否则返回跳过原因。
     */
    private String checkFrogJumpPeerStopAllowed(boolean currentIsLanding, String flightId, String peerDockSn) {
        if (currentIsLanding) {
            // 降落机场结束 → 收尾起飞机场；对端已非 Working 说明早已收尾，避免白发 stop
            if (!isFrogJumpDockWorking(peerDockSn)) {
                return "peer takeoff dock not working, already finished";
            }
            return null;
        }
        // 起飞机场结束 → 保护正在执行/降落的降落机场（无人机已交接或交接中，任务流程不能被打断）；
        // 仅当降落机场还处于准备阶段（如起飞失败永远等不到无人机）时才允许 stop 释放它
        FlighttaskProgress peerProgress = (FlighttaskProgress) RedisOpsUtils.get(getFrogJumpProgressKey(flightId, peerDockSn));
        WaylineMissionStateEnum peerMissionState = peerProgress == null || peerProgress.getExt() == null
                ? null : peerProgress.getExt().getWaylineMissionState();
        if (peerMissionState == WaylineMissionStateEnum.WAYLINE_END
                || peerMissionState == WaylineMissionStateEnum.ARRIVE_FIRST_WAYPOINT
                || peerMissionState == WaylineMissionStateEnum.WAYLINE_EXECUTING
                || peerMissionState == WaylineMissionStateEnum.WAYLINE_RECOVER
                || peerMissionState == WaylineMissionStateEnum.WAYLINE_BROKEN) {
            return "peer landing dock mission in flight/landing state " + peerMissionState + ", must not stop";
        }
        if (!isFrogJumpDockWorking(peerDockSn)) {
            return "peer landing dock not working, already finished";
        }
        return null;
    }

    private boolean isFrogJumpDockWorking(String dockSn) {
        return deviceRedisService.getDeviceOsd(dockSn, OsdDock.class)
                .map(OsdDock::getModeCode)
                .map(modeCode -> modeCode == DockModeCodeEnum.WORKING)
                .orElse(false);
    }
}
