package com.dji.sample.df.wind.mqtt;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.df.framework.vo.Result;
import com.dji.sample.center.config.CenterFtpsNormalConfig;
import com.dji.sample.center.config.CenterNormalConfig;
import com.dji.sample.center.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.ftp.FtpUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.PatrolResultItem;
import com.dji.sample.center.v2022.command.upload.PatrolStatusItem;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.dji.sample.df.mediaDf.controller.FileControllerDf;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.wind.config.FjFileConfig;
import com.dji.sample.df.wind.config.LyFtpsProperties;
import com.dji.sample.df.wind.controller.FjReportController;
import com.dji.sample.df.wind.dao.DefectEntityMapper;
import com.dji.sample.df.wind.dao.FanStationPointsMapper;
import com.dji.sample.df.wind.dao.FanWaylinePointsMapper;
import com.dji.sample.df.wind.dao.WindTurbineMapper;
import com.dji.sample.df.wind.handler.PictureSaveHandler;
import com.dji.sample.df.wind.model.entity.DefectEntity;
import com.dji.sample.df.wind.model.entity.FanStationPoints;
import com.dji.sample.df.wind.model.entity.FanWaylinePoints;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import com.dji.sample.df.wind.timer.TaskTimerManager;
import com.dji.sample.df.wind.utils.FileNameUtils;
import com.dji.sample.media.controller.FileController;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.service.impl.WaylineJobServiceImpl;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import com.dji.sdk.mqtt.longyuan.MqttMessageHandler;
import com.dji.sdk.mqtt.longyuan.MqttStandardMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LongyuanMqttHandler implements MqttMessageHandler {

    @Autowired
    private MqttMessageSender mqttSender;
    @Autowired
    WindTurbineMapper windTurbineMapper;
    @Autowired
    private PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;
    @Autowired
    PubWaylineJobPlanDfService pubWaylineJobPlanDfService;
    @Autowired
    private IWaylineJobMapper waylineJobMapper;
    @Autowired
    WaylineJobServiceImpl waylineJobServiceimpl;
    @Resource
    private RedisUtils redisUtils;
    @Autowired
    TaskTimerManager taskTimerManager;
    @Autowired
    private FjReportController fjReportController;
    @Autowired
    private FjFileConfig fileConfig;
    @Autowired
    LyFtpsProperties  lyFtpsProperties;
    @Autowired
    FileController fileController;
    @Autowired
    FileControllerDf fileControllerDf;
    @Autowired
    private IFileService fileService;
    @Autowired
    private OssServiceContext ossService;
    @Resource
    FanStationPointsMapper fanStationPointsMapper;
    @Autowired
    private PatrolHostSocketClient patrolHostSocketClient;
    @Autowired
    private CenterNormalConfig centerConfig;
    @Autowired
    CenterFtpsNormalConfig centerFtpsNormalConfig;
    @Autowired
    DefectEntityMapper defectEntityMapper;
    @Autowired
    private PictureSaveHandler pictureSaveHandler;
    @Autowired
    UniPointMapper2 uniPointMapper2;
    @Autowired
    FanWaylinePointsMapper fanWaylinePointsMapper;

    // 存储正在监控的任务
    private static final Map<String,Map<String,Long>> monitoringTasks = new ConcurrentHashMap<>();

    // ========== 分析状态监控逻辑 ==========

    // 存储分析中的任务
    private final Map<String, String> analyzingTasks = new ConcurrentHashMap<>(); // taskCode -> jobId
    private final Map<String, Long> analysisStartTime = new ConcurrentHashMap<>(); // taskCode -> 开始时间

    /**
     * 添加到分析监控
     */
    private void addToAnalysisMonitoring(String taskCode, String jobId) {
        analyzingTasks.put(taskCode, jobId);
        analysisStartTime.put(taskCode, System.currentTimeMillis());
        log.info("开始监控分析状态: taskCode={}, jobId={}", taskCode, jobId);
    }

    @Value("${uavPlatform.sender}")
    private String sender;

    @Value("${uavPlatform.stationCode}")
    private String stationCode;

    /**
     * 开始监控任务状态
     */
    public static void startMonitoringTask(String taskCode, String taskName) {
        Map map =new HashMap<>();
        map.put(taskName,  System.currentTimeMillis());
        monitoringTasks.put(taskCode,map);
        log.info("开始监控任务状态: taskCode={}, taskName={}", taskCode, taskName);
    }
    /**
     * 停止监控任务状态
     */
    public void stopMonitoringTask(String taskCode) {
        monitoringTasks.remove(taskCode);
        log.info("停止监控任务状态: taskCode={}", taskCode);
    }

    @Override
    public boolean supports(String topic) {
        return "/patrol/data/LyGroupToSub/guangxi_weilan".equals(topic);
    }

    @Override
    public void handleMessage(String topic, String payload) {
        log.info("收到消息: topic={}", topic);
    }

    @Override
    public void handleStandardMessage(String topic, MqttStandardMessage message) {
        log.info("处理消息: messageId={}, category={}, action={}",
                message.getMessageId(), message.getCategory(), message.getAction());

        String handlerKey = message.getCategory() + ":" + message.getAction();

        switch (handlerKey) {
            case "model:sync":       // 点位模型同步
                handleModelSync(message);
                break;
            case "task:dispatch":    // 任务下发
                handleTaskDispatch(message);
                break;
            default:
                log.warn("未处理的消息类型: category={}, action={}",
                        message.getCategory(), message.getAction());
        }
    }

    // ========== 核心业务处理方法 ==========

    /**
     * 1. 处理点位模型同步（上对下）
     */
    private void handleModelSync(MqttStandardMessage message) {
        Map<String, Object> data = message.getData();
        String syncType = (String) data.get("syncType");
        String description = (String) data.get("description");

        log.info("处理点位模型同步请求: syncType={}, description={}",
                syncType, description);

        try {
            // 1. 根据 syncType 查询对应的模型数据
            List<Map<String, Object>> modelData = queryModelDataByType(syncType);

            // 2. 将 modelData 转换为 JSON 字符串，格式化为可读格式
                        String jsonContent = JSON.toJSONString(
                                modelData,
                                SerializerFeature.PrettyFormat,
                                SerializerFeature.WriteMapNullValue,
                                SerializerFeature.WriteDateUseDateFormat
                        );

            // 3. 生成 JSON 文件名（使用时间戳或UUID确保唯一性）
                        String timestamp = String.valueOf(System.currentTimeMillis());
                        String jsonFileName = "model_data_" + timestamp + ".json";
            // 4. 构建 ObjectKey（存储路径）
            String objectKey = OssConfiguration.objectDirPrefix + "/model/" + jsonFileName;

            // 5. 将 JSON 字符串转换为 InputStream 并上传到 OSS
            try (InputStream jsonInputStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8))) {
                ossService.putObject(OssConfiguration.bucket, objectKey, jsonInputStream);

            } catch (Exception e) {
                // 处理异常
                throw new RuntimeException("上传JSON文件失败", e);
            }

            URL objectUrl = ossService.getObjectUrl(OssConfiguration.bucket, objectKey);
            String urlString = objectUrl.toString();

            // 2. 构建响应报文
            Map<String, Object> response = new HashMap<>();

            response.put("messageId", "uuid-" + UUID.randomUUID().toString().substring(0, 8));
            response.put("timestamp", getCurrentTime());
            response.put("sender", sender);
            response.put("stationCode",stationCode);
            response.put("category", "model");
            response.put("action", "upload");

            // 3. 构建 data 部分
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("modeType", syncType); // 使用上级传来的 syncType
            responseData.put("description", description);
            responseData.put("fileUrl",urlString);
            responseData.put("modelData", "");

            response.put("data", responseData);

            // 4. 发送响应
            mqttSender.sendToPatrolData(response);
            log.info("发送模型数据响应: syncType={}, 数据条数={}",
                    syncType, modelData.size());

        } catch (Exception e) {
            log.error("点位模型同步处理失败", e);
            sendErrorResponse(message, "model", "upload", "MODEL_SYNC_ERROR", e.getMessage());
        }
    }

    private List<Map<String, Object>> queryModelDataByType(String syncType) {
        // 实际应从数据库查询
        List<Map<String, Object>> modelData = new ArrayList<>();

        if ("2".equals(syncType)) {
            // 巡视点位模型
            modelData.addAll(queryPatrolPointModels());
        }

        return modelData;
    }

    private List<Map<String, Object>> queryPatrolPointModels() {
        List<Map<String, Object>> models = new ArrayList<>();
        List<WindTurbine> windTurbines = windTurbineMapper.selectList(new HashMap());

        for (WindTurbine windTurbine : windTurbines) {
            List<FanStationPoints> fanStationPoints = fanStationPointsMapper.selectList(new LambdaQueryWrapper<FanStationPoints>()
                    .eq(FanStationPoints::getMainDeviceId, windTurbine.getId()));
            // 示例数据1
            if (!fanStationPoints.isEmpty()) {
                for (FanStationPoints fanStationPoint : fanStationPoints) {
                    Map<String, Object> point1 = new HashMap<>();

                    point1.put("stationName", fanStationPoint.getStationName());
                    point1.put("stationCode", stationCode);
                    point1.put("areaId", fanStationPoint.getAreaId());
                    point1.put("areaName", fanStationPoint.getAreaName());
                    point1.put("bayId", fanStationPoint.getBayId());
                    point1.put("bayName", fanStationPoint.getBayName());
                    point1.put("mainDeviceId", fanStationPoint.getMainDeviceId());
                    point1.put("mainDeviceName", fanStationPoint.getMainDeviceName());
                    point1.put("componentId", fanStationPoint.getComponentId());
                    point1.put("componentName", fanStationPoint.getComponentName());
                    point1.put("pointId", fanStationPoint.getPointId());
                    point1.put("pointName", fanStationPoint.getPointName());
                    point1.put("deviceType", fanStationPoint.getDeviceType());
                    point1.put("meterType", fanStationPoint.getMeterType());
                    point1.put("appearanceType", fanStationPoint.getAppearanceType());
                    point1.put("saveType", fanStationPoint.getSaveType());
                    point1.put("recognitionType", fanStationPoint.getRecognitionType());
                    point1.put("phase", fanStationPoint.getPhase());
                    point1.put("deviceInfo", fanStationPoint.getDeviceInfo());
                    point1.put("dataType", fanStationPoint.getDataType());
                    point1.put("lowerValue", fanStationPoint.getLowerValue());
                    point1.put("upperValue", fanStationPoint.getUpperValue());
                    point1.put("videoPos", fanStationPoint.getVideoPos());
                    point1.put("pointType", fanStationPoint.getPointType());
                    point1.put("labelAttrib", fanStationPoint.getLabelAttrib());
                    models.add(point1);
                }
            }

        }
        return models;
    }

    /**
     * 2. 处理任务下发（上对下）
     */
    private void handleTaskDispatch(MqttStandardMessage message) {
        Map<String, Object> data = message.getData();
        String taskCode = (String) data.get("taskCode");
        String taskName = (String) data.get("taskName");
        Integer deviceLevel = (Integer) data.get("deviceLevel");
        String deviceList = (String) data.get("deviceList");
        String fixedStartTime = (String) data.get("fixedStartTime");

        log.info("处理任务下发: taskCode={}, taskName={}",
                taskCode, taskName );

        try {
            // 1. 检查设备层级和列表
            if (deviceLevel == 2) {
                // 2. 检查设备列表是否只有一个ID
                String[] deviceIds = deviceList.split(",");
                if (deviceIds.length == 1) {
                    String singleDeviceId = deviceIds[0].trim();

                    // 3. 查询所有风机
                    List<WindTurbine> windTurbines = windTurbineMapper.selectList(new HashMap<>());

                    // 4. 检查是否能匹配上风机ID
                    WindTurbine matchedTurbine = windTurbines.stream()
                            .filter(wt -> singleDeviceId.equals(wt.getId()) ||
                                    singleDeviceId.equals(wt.getId()))
                            .findFirst()
                            .orElse(null);
                    if (matchedTurbine!=null) {

                        // 存入定时任务
                        taskTimerManager.addScheduledTask(1,taskCode, fixedStartTime,
                                singleDeviceId, taskName);

                    }
                }
            }

        } catch (Exception e) {
            log.error("任务处理失败", e);
            sendErrorResponse(message, "task", "dispatch_ack", "TASK_PROCESS_ERROR", e.getMessage());
        }
    }

    /**
     * 定时任务：每3秒检查一次任务状态
     */
    @Scheduled(fixedDelay = 3000)
    public void checkTaskStatus() {
        if (monitoringTasks.isEmpty()) {
            return;
        }

        // 遍历所有正在监控的任务
        for (Map.Entry<String, Map<String,Long>> entry : monitoringTasks.entrySet()) {
            String taskCode = entry.getKey();
            Map<String, Long> value = entry.getValue();
            String taskName;
            if (!value.isEmpty()) {
                // 获取第一个键
                taskName = value.keySet().iterator().next();
                // 获取第一个值
                Long time = value.get(taskName);
                log.info("第一个键值对: {} = {}", taskName, time);
            } else {
                taskName = null;
            }
            try {
                // 从Redis获取jobId
                Object jobIdObj = redisUtils.get("jobId");
                if (jobIdObj == null) {
                    log.warn("未找到jobId，跳过任务状态检查: taskCode={}", taskCode);
                    continue;
                }
                String jobId = jobIdObj.toString();

                // 查询航线任务状态
                WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, redisUtils.get("jobId").toString())
                );
                if (waylineJobEntity == null) {
                    log.warn("未找到航线任务，移除监控: taskCode={}, jobId={}", taskCode, jobId);
                    monitoringTasks.remove(taskCode);
                    continue;
                }

                WaylineJobDTO waylineJobDTO = waylineJobServiceimpl.entity2Dto(waylineJobEntity);
                Integer status = waylineJobDTO.getStatus();
                String isCenterTask = redisUtils.get("isCenterTask").toString();
                // 如果状态为2（执行中），上报状态
                if (status == 2) {
                    if (isCenterTask.equals("1")) {
                        sendWindTurbineTaskStatus(taskCode,taskName,0);
                    }
                } else if (status == 3 || status == 1|| status == 5|| status == 4) {
                    // 如果任务已完成（假设状态3为完成，还有别的），停止监控
                    if (isCenterTask.equals("1")) {
                        sendWindTurbineTaskStatus(taskCode,taskName,1);
                    }
//                    sendWindTurbineTaskStatus(taskCode,taskName,1);
                    if(status == 3){
                        // 查询航线任务状态
                        log.info("上传数为"+waylineJobDTO.getUploadedCount()+"总数为"+waylineJobDTO.getMediaCount());
                        if(waylineJobDTO.getUploadedCount()==waylineJobDTO.getMediaCount()){
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("jobId", jobId);
//                          需要区分是风机任务和普通任务，风机任务走这个逻辑，普通任务直接上传结果（风机任务也直接回传结果只不过继续执行分析逻辑）
                            PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                                    .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
                            Integer planType = pubWaylineJobPlanDfEntity.getPlanType();
                            if(planType==1){
                                log.info("进入分析逻辑---------");
                                // 1. 异步启动图片保存分析
                                new Thread(() -> {
                                    try {
                                        // 调用分析接口（可能会很慢）
                                        Result analyzed = fjReportController.isAnalyzed(jobId);
                                        Integer data = (Integer) analyzed.getData();
//                                      只有未分析时进行分析
                                        if(data==0){
                                            Result result = fjReportController.pictureSave(jsonObject);
                                            log.info("图片分析已启动: jobId={}, result={}", jobId, result);
                                        }
                                        // 2. 开始轮询检查分析状态
                                        startAnalysisMonitoring(jobId, taskCode,taskName);
                                    } catch (Exception e) {
                                        log.error("启动图片分析失败: jobId={}", jobId, e);
                                        // 分析失败也要从监控中移除
                                        monitoringTasks.remove(taskCode);
                                    }
                                }).start();
                                monitoringTasks.remove(taskCode);
                                log.info("任务完成，停止监控: taskCode={}", taskCode);
                            }else if(planType==0){
//                              普通任务先不分析直接保存
                                Result<Map> result = pictureSaveHandler.pictureSave(jobId);
                                if(result.getCode() == 0){
                                    if(isCenterTask.equals("1")){
                                        sendPatrolResult(taskCode, taskName, waylineJobEntity);
                                    }
                                }
                                monitoringTasks.remove(taskCode);
                                log.info("任务完成，停止监控: taskCode={}", taskCode);
                            }
                        }
                    }else {
                        monitoringTasks.remove(taskCode);
                        log.info("任务失败/取消/终止，停止监控: taskCode={}", taskCode);
                    }
                }

            } catch (Exception e) {
                log.error("检查任务状态失败: taskCode={}", taskCode, e);
            }
        }
    }


    private void startAnalysisMonitoring(String jobId, String taskCode,String taskName) {
        // 创建定时检查任务
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            try {
                // 1. 检查分析状态
                Result analyzed = fjReportController.isAnalyzed(jobId);
                Integer data = (Integer) analyzed.getData();
                String isCenterTask = redisUtils.get("isCenterTask").toString();
                if (data==1) {
                    log.info("分析完成: taskCode={}, jobId={}", taskCode, jobId);
                    WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                            .eq(WaylineJobEntity::getJobId, jobId)
                    );
                    log.info("分析完成上传照片--------");
                    if(isCenterTask.equals("1")){
                        sendPatrolResult(taskCode, taskName, waylineJobEntity);
                    }
                    // 2. 分析完成，执行后续逻辑，生成报告上传上级
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("jobId", jobId);
                    Result hisTaskReport = fjReportController.createHisTaskReport(jsonObject);
                    log.info("已生成完报告-------");

                    // 3. 清理监控
                    analyzingTasks.remove(taskCode);
                    analysisStartTime.remove(taskCode);
                    executor.shutdown();

                } else {
                    // 检查是否超时（比如30分钟）
                    Long startTime = analysisStartTime.get(taskCode);
                    if (startTime != null &&
                            System.currentTimeMillis() - startTime > 30 * 60 * 1000L) {
                        log.warn("分析超时: taskCode={}, jobId={}", taskCode, jobId);

                        // 清理
                        analyzingTasks.remove(taskCode);
                        analysisStartTime.remove(taskCode);
                        executor.shutdown();
                    } else {
                        log.debug("分析中: taskCode={}, jobId={}", taskCode, jobId);
                    }
                }

            } catch (Exception e) {
                log.error("检查分析状态失败: jobId={}", jobId, e);
            }
        }, 10, 3, TimeUnit.SECONDS); // 10秒后开始，每30秒检查一次
    }

    public void sendPatrolResult(String taskCode, String taskName,WaylineJobEntity waylineJobEntity) {
        try {
            PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                    .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
            Integer planType = pubWaylineJobPlanDfEntity.getPlanType();
            if(planType==1){
                HttpResultResponse mediaFileByJobId = fileControllerDf.getMediaFileByJobId(waylineJobEntity.getJobId(), "e3dea0f5-37f2-4d79-ae58-490af3228069", 1L, 500L, new HashMap<>());
                PaginationData<MediaFileDTO> data1 = (PaginationData< MediaFileDTO >)mediaFileByJobId.getData();
                List<MediaFileDTO> list = data1.getList();
                for (MediaFileDTO mediaFileDTO : list) {
                    URL url = fileService.getObjectUrl("e3dea0f5-37f2-4d79-ae58-490af3228069",mediaFileDTO.getFileId());
                    String urlString = url.toString();
                    log.info("图片映射路径为"+urlString);
                    Map<String, Object> resultMessage = new HashMap<>();

                    resultMessage.put("messageId", "uuid-" + UUID.randomUUID().toString().substring(0, 8));
                    resultMessage.put("timestamp", getCurrentTime());
                    resultMessage.put("sender", sender);
                    resultMessage.put("stationCode", stationCode);
                    resultMessage.put("category", "task");
                    resultMessage.put("action", "result");
                    FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>().eq(FanWaylinePoints::getJobId, waylineJobEntity.getJobId()));
                    Integer jobType = fanWaylinePoints.getJobType();

                    FanStationPoints fanStationPoints = fanStationPointsMapper.selectOne(new LambdaQueryWrapper<FanStationPoints>()
                            .eq(FanStationPoints::getPointName, waylineJobEntity.getFanName() + "-" + mediaFileDTO.getFanCode() + "-" + mediaFileDTO.getFanPart()+"_"+jobType));
                    String pointId ="-----";
                    String pointName ="错误点位";
                    if (fanStationPoints != null) {
                        pointName=fanStationPoints.getPointName();
                        pointId=fanStationPoints.getPointId();
                    }else {
//                  没有匹配到就不发送，是无人机多拍了
                        continue;
                    }
                    String taskPatrolledId = String.format("%s_%s_%s",
                            stationCode,taskCode, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                    Map<String, Object> data = new HashMap<>();
                    data.put("patrolDeviceName","大疆M4td");
                    data.put("patrolDeviceCode", "1581F8HGX253800A030D");
                    data.put("taskName", taskName);
                    data.put("taskCode", taskCode);
//              点位名称是拼接，点位id是文件id
                    data.put("pointName", pointName);
                    data.put("pointId", pointId);
                    data.put("valueType", ""); // 4=红外测温
                    data.put("value", "");
                    data.put("valueUnit", "");
                    data.put("unit", "");
                    data.put("time","");
                    data.put("recognitionType", ""); // 4=红外识别
                    data.put("fileType", "3"); // 1=图片
                    data.put("filePath",urlString);
                    data.put("rectangle","");
                    data.put("taskPatrolledId", taskPatrolledId);
                    data.put("valid","1");

                    resultMessage.put("data", data);
                    // 发送到MQTT
//                mqttSender.sendToPatrolData(resultMessage);

                    PatrolHostCommand commandData = patrolHostSocketClient.getBaseCommand("61", "", stationCode);
                    String destDir = centerFtpsNormalConfig.getFileSavePath() + "/" + stationCode + "/" + waylineJobEntity.getJobId() + "/";
//                String localFile = point.getMediaFileDTOS().get(0).getFilePath();
                    DefectEntity defectEntity = defectEntityMapper.selectOne(new LambdaQueryWrapper<DefectEntity>()
                            .eq(DefectEntity::getJobId, waylineJobEntity.getJobId())
                            .eq(DefectEntity::getFanCode, mediaFileDTO.getFanCode())
                            .eq(DefectEntity::getFanPart, mediaFileDTO.getFanPart()));
                    String imagePath = defectEntity.getImagePath();
                    String filePath = convertImagePath(imagePath);
                    String destName = new File(filePath).getName();
                    String destName1 = FileNameUtils.convertChineseToPinyinInitials(destName);
                    FtpUtils.getInstance().uploadToCenterNormal(filePath, destDir, destName1);
                    //推送点位报文
                    String format = String.format("%s/%s", destDir, destName1);

                    PatrolResultItem item = new PatrolResultItem();
                    item.setPatroldevice_name("大疆M4td");
                    item.setPatroldevice_code("1581F8HGX253800A030D");
                    item.setTask_name(taskName);
                    item.setTask_code(taskCode);
                    item.setDevice_name(pointName);
                    item.setDevice_id(pointId);
                    item.setValue("");
                    item.setUnit("");
                    item.setValue_unit("");
                    item.setTime(DateUtils.getNowDateTimeStr());
//              识别类型先设置为空
                    item.setRecognition_type("");
                    item.setFile_path(format);
                    item.setFile_type("2");
                    item.setRectangle("");
                    item.setTask_patrolled_id(waylineJobEntity.getJobId());
                    item.setObj_id("");
                    item.setValid("1");
                    commandData.addItem(item);
                    patrolHostSocketClient.sendCommand(commandData, PatrolResultItem.class);
                    log.info("上报巡视图片--------: ");
                }
            }else if(planType==0) {
//                  普通计划上传照片（待测）
                    HttpResultResponse mediaFileByJobId = fileControllerDf.getMediaFileByJobId(waylineJobEntity.getJobId(), "e3dea0f5-37f2-4d79-ae58-490af3228069", 1L, 500L, new HashMap<>());
                    PaginationData<MediaFileDTO> data1 = (PaginationData<MediaFileDTO>) mediaFileByJobId.getData();
                    List<MediaFileDTO> list = data1.getList();
                    for (MediaFileDTO mediaFileDTO : list) {
                        String fileName = mediaFileDTO.getFileName();
                        Integer pointPos = extractWaypointNumber(fileName);
                        String picType = extractTOrV(fileName);
                        Integer picType1 = 0;
                        if(picType.equals("V")){
                            picType1 = 0;
                        }else if(picType.equals("T")){
                            picType1 = 1;
                        }
                        UniPoint uniPoint = uniPointMapper2.selectOne(new LambdaQueryWrapper<UniPoint>()
                                .eq(UniPoint::getWaylineId, waylineJobEntity.getFileId())
                                .eq(UniPoint::getWaylinePointPos, pointPos)
                                .eq(UniPoint::getPicType,picType1));
                        if(uniPoint == null){
                            log.info("未查到对应点位-----");
                            continue;
                        }

                        PatrolHostCommand commandData = patrolHostSocketClient.getBaseCommand("61", "", stationCode);
                        String destDir = centerFtpsNormalConfig.getFileSavePath() + "/" + stationCode + "/" + waylineJobEntity.getJobId() + "/";
                        String regId=uniPoint.getPointCode()+picType;
                        String replace = fileName.replace(".jpeg", "");
                        String filePath="/ftpdir/admin_files/recfile_images/"+waylineJobEntity.getJobId()+"/"+regId+"_"+replace+".jpg";
                        String destName = new File(filePath).getName();
                        String destName1 = FileNameUtils.convertChineseToPinyinInitials(destName);
                        FtpUtils.getInstance().uploadToCenterNormal(filePath, destDir, destName1);
                        //推送点位报文
                        String format = String.format("%s/%s", destDir, destName1);

                        PatrolResultItem item = new PatrolResultItem();
                        item.setPatroldevice_name("大疆M4td");
                        item.setPatroldevice_code("1581F8HGX253800A030D");
                        item.setTask_name(taskName);
                        item.setTask_code(taskCode);
                        item.setDevice_name(uniPoint.getPointName());
                        item.setDevice_id(uniPoint.getPointCode());
                        item.setValue("");
                        item.setUnit("");
                        item.setValue_unit("");
                        item.setTime(DateUtils.getNowDateTimeStr());
//              识别类型先设置为空
                        item.setRecognition_type("");
                        item.setFile_path(format);
                        item.setFile_type("2");
                        item.setRectangle("");
                        item.setTask_patrolled_id(waylineJobEntity.getJobId());
                        item.setObj_id("");
                        item.setValid("1");
                        commandData.addItem(item);
                        patrolHostSocketClient.sendCommand(commandData, PatrolResultItem.class);
                        log.info("上报巡视图片--------: ");
                    }
                }

        } catch (Exception e) {
            log.error("上报巡视结果失败: taskCode={}", taskCode, e);
        }
    }

    public Integer extractWaypointNumber(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        // 正则表达式：匹配"航点"后面的一个或多个数字
        // 注意：航点可能是中文，数字可能是一位或多位
        Pattern pattern = Pattern.compile("航点(\\d+)");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // 如果数字太大或格式错误，返回null
                return null;
            }
        }

        return null;
    }

    public static String extractTOrV(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        // 移除路径和扩展名，只获取文件名部分
        String nameWithoutPath = fileName.substring(fileName.lastIndexOf('/') + 1);
        nameWithoutPath = nameWithoutPath.substring(nameWithoutPath.lastIndexOf('\\') + 1);
        String nameWithoutExt = nameWithoutPath.split("\\.", 2)[0];

        // 方法1：使用正则表达式匹配_T_或_V_模式
        Pattern pattern = Pattern.compile("_(T|V)_");
        Matcher matcher = pattern.matcher(nameWithoutExt);

        if (matcher.find()) {
            return matcher.group(1);  // 直接返回字符串
        }
        return null;
    }



    public static String convertImagePath(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return imagePath;
        }

        // 匹配 defect_out/ 和 _result数字 模式
        String pattern = "defect_out/(.*?)_result\\d+(\\.\\w+)$";
        String replacement = "$1$2";

        return imagePath.replaceAll(pattern, replacement);
    }


    // ========== 上报方法（下对上） ==========

    /**
     * 3. 上报任务状态（下对上）
     */
    private void sendWindTurbineTaskStatus(String taskCode,String taskName,Integer isFinished) {
        Map<String, Object> statusMessage = new HashMap<>();

        statusMessage.put("messageId", "uuid-" + UUID.randomUUID().toString().substring(0, 8));
        statusMessage.put("timestamp", getCurrentTime());
        statusMessage.put("sender", sender);
        statusMessage.put("stationCode",stationCode);
        statusMessage.put("category", "task");
        statusMessage.put("action", "status");

        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId, redisUtils.get("jobId").toString())
        );
        WaylineJobDTO waylineJobDTO = waylineJobServiceimpl.entity2Dto(waylineJobEntity);

        Map<String, Object> data = new HashMap<>();
        // 生成任务巡逻ID，格式：stationCode_taskCode_时间戳
        String taskPatrolledId = String.format("%s_%s_%s",
                stationCode,taskCode, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        Integer taskState = waylineJobDTO.getStatus();
        String mappedState = mapTaskStateToCode(taskState);
        Integer progress = null;

        if(isFinished == 1){
            progress=100;
        }else if(isFinished == 0){
            progress = waylineJobDTO.getProgress();
        }
        data.put("taskPatrolledId", waylineJobEntity.getJobId());
        data.put("taskName", taskName);
        data.put("taskCode", taskCode);
        data.put("taskState", mappedState);
        data.put("planStartTime", waylineJobDTO.getBeginTime());
        data.put("startTime",waylineJobDTO.getExecuteTime());
        data.put("taskProgress", progress + "%");
        data.put("taskEstimatedTime", calculateEstimatedTime(progress));
        data.put("description", "正在进行风机巡视，已完成"+progress+"%的巡视任务");

        statusMessage.put("data", data);
//      需要改成tcp上报状态
//        mqttSender.sendToPatrolData(statusMessage);

        List<PatrolStatusItem> patrolStatusItems = new ArrayList<>();
        PatrolStatusItem item = new PatrolStatusItem();
        item.setTask_patrolled_id(waylineJobEntity.getJobId());
        item.setTask_name(taskName);
        item.setTask_code(taskCode);
        item.setTask_state(mappedState);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        item.setStart_time(formatter.format(waylineJobDTO.getExecuteTime()));
        item.setPlan_start_time(formatter.format(waylineJobDTO.getBeginTime()));
        item.setTask_progress(progress + "%");
        item.setTask_estimated_time("");
        item.setDescription("");
        patrolStatusItems.add(item);

        PatrolHostCommand commandData = new PatrolHostCommand();
        commandData.addItems(patrolStatusItems);
        commandData.setSendCode(centerConfig.getStationCode());
        commandData.setReceiveCode(centerConfig.getServerCode());
        commandData.setType("41");
        patrolHostSocketClient.sendCommand(commandData, PatrolStatusItem.class);

        log.info("上报任务状态: taskCode={}, state={}, progress={}%",
                taskCode, mappedState, progress);
    }

    /**
     * 任务状态映射：将内部状态转换为报文规范中的状态码
     */
    private String mapTaskStateToCode(Integer internalState) {
        switch (internalState) {
            case 1 :return "5";    // 待执行
            case 2: return "2";  // 执行中
            case 3: return "1";  // 已完成
            case 4: return "4";     // 取消
            case 5: return "6";  // 失败
            case 6: return "3";  // 暂停
            default: return "2";           // 默认执行中
        }
    }

    /**
     * 计算预计剩余时间
     */
    private String calculateEstimatedTime(int progress) {
        if (progress >= 100) {
            return "00:00:00";
        }

        // 假设总耗时30分钟，根据进度计算剩余时间
        int totalSeconds = 25 * 60; // 30分钟
        int remainingSeconds = (int) ((100 - progress) / 100.0 * totalSeconds);

        int hours = remainingSeconds / 3600;
        int minutes = (remainingSeconds % 3600) / 60;
        int seconds = remainingSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // ========== 工具方法 ==========

    /**
     * 创建标准响应格式
     */
    private Map<String, Object> createResponse(MqttStandardMessage original,
                                               String category, String action) {
        Map<String, Object> response = new HashMap<>();
        response.put("messageId", UUID.randomUUID().toString());
        response.put("timestamp", getCurrentTime());
        response.put("sender", "PatrolSystem01"); // 你的系统标识
        response.put("stationCode", original.getStationCode());
        response.put("category", category);
        response.put("action", action);

        Map<String, Object> data = new HashMap<>();
        data.put("originalMessageId", original.getMessageId());
        data.put("responseTime", getCurrentTime());
        response.put("data", data);

        return response;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(MqttStandardMessage original, String category,
                                   String action, String errorCode, String errorMsg) {
        Map<String, Object> response = createResponse(original, category, action);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        data.put("result", "failed");
        data.put("errorCode", errorCode);
        data.put("errorMsg", errorMsg);

        mqttSender.sendToPatrolData(response);
        log.error("发送错误响应: category={}, action={}, errorCode={}",
                category, action, errorCode);
    }

    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


    @Override
    public String getHandlerName() {
        return "longyuan-core-handler";
    }
}
