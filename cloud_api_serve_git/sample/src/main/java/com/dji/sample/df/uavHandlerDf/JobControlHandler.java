package com.dji.sample.df.uavHandlerDf;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.df.framework.vo.Result;
import com.dji.sample.center.config.CenterNormalConfig;
import com.dji.sample.center.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.ftp.FtpUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.PatrolResultItem;
import com.dji.sample.center.v2022.command.upload.PatrolStatusItem;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.mediaDf.controller.FileControllerDf;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.wind.controller.FjReportController;
import com.dji.sample.df.wind.dao.DefectEntityMapper;
import com.dji.sample.df.wind.dao.FanStationPointsMapper;
import com.dji.sample.df.wind.dao.FanWaylinePointsMapper;
import com.dji.sample.df.wind.model.entity.DefectEntity;
import com.dji.sample.df.wind.model.entity.FanStationPoints;
import com.dji.sample.df.wind.model.entity.FanWaylinePoints;
import com.dji.sample.df.wind.utils.FileNameUtils;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.service.impl.WaylineJobServiceImpl;
import com.dji.sdk.cloudapi.wayline.WaylineErrorCodeEnum;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.net.URL;
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
public class JobControlHandler {

    @Autowired
    private PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;
    @Autowired
    private IWaylineJobMapper waylineJobMapper;
    @Autowired
    WaylineJobServiceImpl waylineJobServiceimpl;
    @Resource
    private RedisUtils redisUtils;
    @Autowired
    private FjReportController fjReportController;
    @Autowired
    FileControllerDf fileControllerDf;
    @Autowired
    private IFileService fileService;
    @Resource
    FanStationPointsMapper fanStationPointsMapper;
    @Autowired
    private PatrolHostSocketClient patrolHostSocketClient;
    @Autowired
    private CenterNormalConfig centerConfig;
    @Autowired
    DefectEntityMapper defectEntityMapper;
    @Autowired
    UniPointMapper2 uniPointMapper2;
    @Autowired
    FanWaylinePointsMapper fanWaylinePointsMapper;
    @Resource
    private IDeviceMapper deviceMapper;

    private final Map<String, UploadStallInfo> uploadStallMap = new ConcurrentHashMap<>();

    private static class UploadStallInfo {
        int lastUploadedCount;
        long lastChangeTime;
    }

    // 存储正在监控的任务
    private static final Map<String,Map<String,Long>> monitoringTasks = new ConcurrentHashMap<>();

    // 存储分析中的任务
    private final Map<String, String> analyzingTasks = new ConcurrentHashMap<>(); // taskCode -> jobId
    private final Map<String, Long> analysisStartTime = new ConcurrentHashMap<>(); // taskCode -> 开始时间

    @Value("${normalStation.stationCode}")
    private String normalStationCode;

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
     * 定时任务：每3秒检查一次任务状态
     */
    @Scheduled(fixedDelay = 3000)
    public void checkTaskStatus() {
        if (monitoringTasks.isEmpty()) {
            return;
        }
//      设置开始2小时后还没传完即为异常，就定时删除任务监控，检查并移除超时的任务
        long currentTime = System.currentTimeMillis();
        removeTimedOutTasks(currentTime);
//      遍历所有正在监控的任务
        for (Map.Entry<String, Map<String,Long>> entry : monitoringTasks.entrySet()) {
//          处理任务
            processMonitoredTask(entry);
        }
    }



    /**
     * 处理单个被监控任务：查询航线状态并按状态执行上报/后续逻辑
     */
    private void processMonitoredTask(Map.Entry<String, Map<String,Long>> entry) {
        String taskCode = entry.getKey();
        Map<String, Long> value = entry.getValue();
        String taskName;
        if (!value.isEmpty()) {
            taskName = value.keySet().iterator().next();
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
                return;
            }
            String jobId = jobIdObj.toString();

            // 查询航线任务状态
            WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                    .eq(WaylineJobEntity::getJobId, redisUtils.get("jobId").toString())
            );
            if (waylineJobEntity == null) {
                log.warn("未找到航线任务，移除监控: taskCode={}, jobId={}", taskCode, jobId);
                monitoringTasks.remove(taskCode);
                uploadStallMap.remove(taskCode);
                return;
            }
            WaylineJobDTO waylineJobDTO = waylineJobServiceimpl.entity2Dto(waylineJobEntity);
            Integer status = waylineJobDTO.getStatus();
            String isCenterTask = redisUtils.get("isCenterTask").toString();
            // 如果状态为2（执行中），上报状态
            if (status == 2) {
//              上报任务状态
                if (isCenterTask.equals("1") && !jobId.equals(taskCode)) {
                    sendWindTurbineTaskStatus(taskCode,taskName,0);
                }
            } else if (status == 3 || status == 1|| status == 5|| status == 4) {
//              上报任务状态
                if (isCenterTask.equals("1")&& !jobId.equals(taskCode)) {
                    sendWindTurbineTaskStatus(taskCode,taskName,1);
                }
                if(status == 3){
//              任务完成，执行上报结果
                    handleUploadProgress(jobId, taskCode, taskName, waylineJobEntity, waylineJobDTO, isCenterTask);
                }else {
                    monitoringTasks.remove(taskCode);
                    log.info("任务失败/取消/终止，停止监控: taskCode={}", taskCode);
                    uploadStallMap.remove(taskCode);
                }
            }

        } catch (Exception e) {
            log.error("检查任务状态失败: taskCode={}", taskCode, e);
        }
    }

    /**
     * 任务状态为已完成（status==3）时，根据上传进度和计划类型执行后续处理
     */
    private void handleUploadProgress(String jobId, String taskCode, String taskName, WaylineJobEntity waylineJobEntity, WaylineJobDTO waylineJobDTO, String isCenterTask) throws Exception {
        log.info("图片上传数为"+waylineJobDTO.getUploadedCount()+"总数为"+waylineJobDTO.getMediaCount());
        int uploaded = waylineJobDTO.getUploadedCount();
        int total = waylineJobDTO.getMediaCount();
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
        Integer planType = pubWaylineJobPlanDfEntity.getPlanType();
//      上传数等于总数开始后续处理
        if(uploaded == total){
            handleUploadCompleted(planType, jobId, taskCode, taskName, waylineJobEntity, isCenterTask);
//          只针对航点航线任务，如果拍照上传数停滞，则执行下面的逻辑
        }else if (planType == 0 && uploaded >= total - 2 && uploaded < total) {
            handleUploadStall(jobId, taskCode, taskName, waylineJobEntity, isCenterTask, uploaded, total);
        }else {
            // 上传数未达到阈值，不做处理，但需要重置停滞记录（防止残留）
            if (planType == 0) {
                uploadStallMap.remove(taskCode);
            }
        }
    }

    /**
     * 上传数已达到总数（uploaded == total）时，按计划类型执行分析/上报并停止监控
     */
    private void handleUploadCompleted(Integer planType, String jobId, String taskCode, String taskName, WaylineJobEntity waylineJobEntity, String isCenterTask) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("jobId", jobId);

        if(planType==1){
            log.info("处理风机任务结果----");
            startAsyncAnalysisAndStopMonitoring(jobId, taskCode, taskName);
        }else if(planType==0){
            log.info("处理航点航线任务结果----");
            try {
                Result result = fjReportController.pictureSaveAndAnalysis(jsonObject);
                if(result.getCode() == 0){
                    if(isCenterTask.equals("1") && !jobId.equals(taskCode)){
                        sendPatrolResult(taskCode, taskName, waylineJobEntity);
                    }
                }
            } catch (Exception e) {
                log.error("pictureSaveAndAnalysis failed", e);
            } finally {
                monitoringTasks.remove(taskCode);
                uploadStallMap.remove(taskCode);
                log.info("任务完成，停止监控: taskCode={}", taskCode);
            }
        }else if(planType==3){
            log.info("处理普通任务结果----");
            Result result = fjReportController.pictureSaveAndAnalysis(jsonObject);
            if(result.getCode() == 0){
                if(isCenterTask.equals("1")&& !jobId.equals(taskCode)){
                    sendPatrolResult(taskCode, taskName, waylineJobEntity);
                }
            }
            monitoringTasks.remove(taskCode);
            uploadStallMap.remove(taskCode);
            log.info("任务完成，停止监控: taskCode={}", taskCode);
        }else if(planType==4){
            log.info("处理光伏任务结果----");
            startAsyncAnalysisAndStopMonitoring(jobId, taskCode, taskName);
        }
    }

    /**
     * 异步启动图片保存分析并轮询分析状态，同时停止当前任务监控（风机/光伏计划共用）
     */
    private void startAsyncAnalysisAndStopMonitoring(String jobId, String taskCode, String taskName) {
        // 1. 异步启动图片保存分析
        new Thread(() -> {
            try {
//              调用分析接口
                Result analyzed = fjReportController.isAnalyzed(jobId);
                Integer data = (Integer) analyzed.getData();
//              只有未分析时进行分析
                if(data==0){
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("jobId", jobId);
//                  保存图片并分析
                    Result result = fjReportController.pictureSaveAndAnalysis(jsonObject);
                    log.info("图片分析已启动: jobId={}, result={}", jobId, result);
                }
        // 2. 开始轮询检查分析状态
                startAnalysisMonitoring(jobId, taskCode,taskName);
            } catch (Exception e) {
                log.error("启动图片分析失败: jobId={}", jobId, e);
                // 分析失败也要从监控中移除
                monitoringTasks.remove(taskCode);
                uploadStallMap.remove(taskCode);
            }
        }).start();
        monitoringTasks.remove(taskCode);
        uploadStallMap.remove(taskCode);
        log.info("任务完成，停止监控: taskCode={}", taskCode);
    }

    /**
     * 航点航线任务上传数停滞检测：停滞超过30秒则强制进入后续处理
     */
    private void handleUploadStall(String jobId, String taskCode, String taskName,
                                   WaylineJobEntity waylineJobEntity, String isCenterTask,
                                   int uploaded, int total) throws Exception {
        // 获取该任务的停滞信息
        UploadStallInfo stallInfo = uploadStallMap.get(taskCode);
        long now = System.currentTimeMillis();
        if (stallInfo == null) {
            // 第一次进入停滞检测，记录当前上传数和时间
            stallInfo = new UploadStallInfo();
            stallInfo.lastUploadedCount = uploaded;
            stallInfo.lastChangeTime = now;
            uploadStallMap.put(taskCode, stallInfo);
            log.info("开始监控上传停滞: taskCode={}, 当前上传={}/{}", taskCode, uploaded, total);
        } else {
            // 检查上传数是否有变化
            if (stallInfo.lastUploadedCount != uploaded) {
                // 上传数有增加，更新时间
                stallInfo.lastUploadedCount = uploaded;
                stallInfo.lastChangeTime = now;
                log.info("上传数更新: taskCode={}, 当前上传={}/{}", taskCode, uploaded, total);
            } else {
                // 上传数未变化，检查是否超过30秒
                long stagnantDuration = now - stallInfo.lastChangeTime;
                if (stagnantDuration >= 30_000) {  // 30秒阈值
                    log.warn("上传数已停滞超过30秒: taskCode={}, 上传={}/{}, 强制进入后续处理",
                            taskCode, uploaded, total);
                    // 执行与完全相同时相同的后续逻辑
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("jobId", jobId);
                    Result result = fjReportController.pictureSaveAndAnalysis(jsonObject);
                    if(result.getCode() == 0){
                        if(isCenterTask.equals("1")&& !jobId.equals(taskCode)){
                            sendPatrolResult(taskCode, taskName, waylineJobEntity);
                        }
                    }
                    monitoringTasks.remove(taskCode);
                    uploadStallMap.remove(taskCode);
                    log.info("停滞任务已处理并移除监控: taskCode={}", taskCode);
                } else {
                    log.debug("上传数停滞中，已持续{}ms: taskCode={}, 上传={}/{}",
                            stagnantDuration, taskCode, uploaded, total);
                }
            }
        }
    }

//  检查分析状态，分析结束后上报结果（图片和报告，目前适用于风机）
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
                    if(isCenterTask.equals("1")&& !jobId.equals(taskCode)){
                        sendPatrolResult(taskCode, taskName, waylineJobEntity);
                    }
                    // 2. 分析完成，执行后续逻辑，生成报告上传上级
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("jobId", jobId);
                    Result hisTaskReport = fjReportController.createHisTaskReport(jsonObject);
                    log.info("已生成完报告-------");
                    if(isCenterTask.equals("1")&& !jobId.equals(taskCode)){
                        log.info("上传报告-------");
                        sendPatrolReportResult(taskCode, taskName, waylineJobEntity);
                    }
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
            DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 0));
            String deviceSn = deviceEntity.getDeviceSn();
            if(planType==1){
                // 风机计报结果
                sendFanPatrolResult(taskCode, taskName, waylineJobEntity, deviceSn);
            }else if(planType==0) {
                // 航点航线上报结果
                sendNormalPatrolResult(taskCode, taskName, waylineJobEntity, deviceSn);
            }
        } catch (Exception e) {
            log.error("上报巡视结果失败: taskCode={}", taskCode, e);
        }
    }

    /**
     * 风机计划（planType==1）巡视结果上报：
     * 遍历任务媒体文件，匹配风机站点点位，上传缺陷分析图并推送点位报文
     */
    private void sendFanPatrolResult(String taskCode, String taskName, WaylineJobEntity waylineJobEntity, String deviceSn) throws Exception {
        HttpResultResponse mediaFileByJobId = fileControllerDf.getMediaFileByJobId(waylineJobEntity.getJobId(), "e3dea0f5-37f2-4d79-ae58-490af3228069", 1L, 500L, new HashMap<>());
        PaginationData<MediaFileDTO> data1 = (PaginationData< MediaFileDTO >)mediaFileByJobId.getData();
        List<MediaFileDTO> list = data1.getList();
        for (MediaFileDTO mediaFileDTO : list) {

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
//              没有匹配到就不发送，是无人机多拍了
                continue;
            }

            PatrolHostCommand commandData = patrolHostSocketClient.getBaseCommand("61", "", normalStationCode);
            String destDir = "/" + taskCode;
            DefectEntity defectEntity = defectEntityMapper.selectOne(new LambdaQueryWrapper<DefectEntity>()
                    .eq(DefectEntity::getJobId, waylineJobEntity.getJobId())
                    .eq(DefectEntity::getFanCode, mediaFileDTO.getFanCode())
                    .eq(DefectEntity::getFanPart, mediaFileDTO.getFanPart()));
            String imagePath = defectEntity.getImagePath();
//          直接传分析图，加缺陷字段defectDescription
            String defectDescription = defectEntity.getDefectDescription();
            String destName = new File(imagePath).getName();
            String destName1 = FileNameUtils.convertChineseToPinyinInitials(destName);
            FtpUtils.getInstance().uploadToCenterNormal(imagePath, destDir, destName1);
            //推送点位报文
            String format = String.format("%s/%s", destDir, destName1);

            PatrolResultItem item = new PatrolResultItem();
            item.setPatroldevice_name("大疆M4TD");
            item.setPatroldevice_code(deviceSn);
            item.setTask_name(taskName);
            item.setTask_code(taskCode);
            item.setDevice_name(pointName);
            item.setDevice_id(pointId);
            item.setValue("");
            item.setUnit("");
//          用Value_unit存储缺陷数据
            item.setValue_unit(defectDescription);
            item.setTime(DateUtils.getNowDateTimeStr());
//          识别类型先设置为空
            item.setRecognition_type("");
            item.setFile_path(format);
            item.setFile_type("2");
            item.setRectangle("");
            item.setTask_patrolled_id(waylineJobEntity.getJobId());
//          item.setDefect_description(defectDescription);
            item.setObj_id("");
            item.setValid("1");
            commandData.addItem(item);
            patrolHostSocketClient.sendCommand(commandData, PatrolResultItem.class);
            log.info("上报风机巡视图片--------: ");
        }
    }

    /**
     * 航点航线计划（planType==0）巡视结果上报：
     * 根据文件名解析航点序号与可见光/红外类型，匹配统一点位，上传原图并推送点位报文
     */
    private void sendNormalPatrolResult(String taskCode, String taskName, WaylineJobEntity waylineJobEntity, String deviceSn) throws Exception {
//      航点航线计划上传照片
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
            PatrolHostCommand commandData = patrolHostSocketClient.getBaseCommand("61", "", normalStationCode);
            String destDir = "/" + taskCode;
            String regId=uniPoint.getPointCode()+picType;
            String replace = fileName.replace(".jpeg", "");
            String filePath="/ftpdir/admin_files/recfile_images/"+waylineJobEntity.getJobId()+"/"+regId+"_"+replace+".jpg";
            String destName = new File(filePath).getName();
            String destName1 = FileNameUtils.convertChineseToPinyinInitials(destName);
            FtpUtils.getInstance().uploadToCenterNormal(filePath, destDir, destName1);
            //推送点位报文
            String format = String.format("%s/%s", destDir, destName1);

            PatrolResultItem item = new PatrolResultItem();
            item.setPatroldevice_name("大疆M4TD");
            item.setPatroldevice_code(deviceSn);
            item.setTask_name(taskName);
            item.setTask_code(taskCode);
            item.setDevice_name(uniPoint.getPointName());
            item.setDevice_id(uniPoint.getPointCode());
            item.setValue("");
            item.setUnit("");
            item.setValue_unit("");
            item.setTime(DateUtils.getNowDateTimeStr());
//          识别类型先设置为空
            item.setRecognition_type("");
            item.setFile_path(format);
            item.setFile_type("2");
            item.setRectangle("");
            item.setTask_patrolled_id(waylineJobEntity.getJobId());
            item.setObj_id("");
            item.setValid("1");
            commandData.addItem(item);
            patrolHostSocketClient.sendCommand(commandData, PatrolResultItem.class);
            log.info("上报航点航线巡视图片--------: ");
        }
    }
//   上传巡视报告
    public void sendPatrolReportResult(String taskCode, String taskName,WaylineJobEntity waylineJobEntity) {
        try {
            PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                    .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
            Integer planType = pubWaylineJobPlanDfEntity.getPlanType();
            DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 0));
            String deviceSn = deviceEntity.getDeviceSn();
            if(planType==1){
                    PatrolHostCommand commandData = patrolHostSocketClient.getBaseCommand("61", "", normalStationCode);
                    String destDir = "/" + taskCode;
                    String reportPath ="/home/uav_server/report/"+waylineJobEntity.getName()+".docx";
                    String destName = new File(reportPath).getName();
                    String destName1 = FileNameUtils.convertChineseToPinyinInitials(destName);
                    FtpUtils.getInstance().uploadToCenterNormal(reportPath, destDir, destName1);
                    //推送点位报文
                    String format = String.format("%s/%s", destDir, destName1);

                    PatrolResultItem item = new PatrolResultItem();
                    item.setPatroldevice_name("大疆M4TD");
                    item.setPatroldevice_code(deviceSn);
                    item.setTask_name(taskName);
                    item.setTask_code(taskCode);
                    item.setDevice_name("");
                    item.setDevice_id("");
                    item.setValue("");
                    item.setUnit("");
                    item.setValue_unit("");
                    item.setTime(DateUtils.getNowDateTimeStr());
                    item.setRecognition_type("");
                    item.setFile_path(format);
//                  用4表示上传巡视报告
                    item.setFile_type("4");
                    item.setRectangle("");
                    item.setTask_patrolled_id(waylineJobEntity.getJobId());
                    item.setObj_id("");
                    item.setValid("1");
                    commandData.addItem(item);
                    patrolHostSocketClient.sendCommand(commandData, PatrolResultItem.class);
                    log.info("上报巡视报告--------: ");
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

        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId, redisUtils.get("jobId").toString())
        );
        WaylineJobDTO waylineJobDTO = waylineJobServiceimpl.entity2Dto(waylineJobEntity);

        Map<String, Object> data = new HashMap<>();

        Integer taskState = waylineJobDTO.getStatus();
        String mappedState = mapTaskStateToCode(taskState);
        // isFinished==1 时进度固定100%，否则取任务实时进度
        Integer progress = null;
        if(isFinished == 1){
            progress=100;
        }else if(isFinished == 0){
            progress = waylineJobDTO.getProgress();
        }
        // 根据映射后的状态码生成描述文案
        String description = buildTaskDescription(mappedState, progress, waylineJobDTO);
        statusMessage.put("data", data);

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
        item.setTask_estimated_time(calculateEstimatedTime(progress));
        item.setDescription(description);
        patrolStatusItems.add(item);

        PatrolHostCommand commandData = new PatrolHostCommand();
        commandData.addItems(patrolStatusItems);
        commandData.setSendCode(normalStationCode);
        commandData.setReceiveCode(centerConfig.getServerCode());
        commandData.setType("41");
        patrolHostSocketClient.sendCommand(commandData, PatrolStatusItem.class);

        log.info("上报任务状态: taskCode={}, state={}, progress={}%",
                taskCode, mappedState, progress);
    }

    /**
     * 根据映射后的状态码生成任务状态描述文案
     * 默认为执行中文案，其余状态覆盖为对应描述；失败(6)时附带错误原因
     */
    private String buildTaskDescription(String mappedState, Integer progress, WaylineJobDTO waylineJobDTO) {
        String description ="正在进行巡视，已完成"+progress+"%的巡视任务";
        if(mappedState.equals("1")){
            description="完成任务巡检";
        } else if(mappedState.equals("3")){
            description="巡检任务已暂停";
        }else if(mappedState.equals("4")){
            description="巡检任务已取消";
        }else if(mappedState.equals("5")){
            description="巡检任务待执行";
        }else if(mappedState.equals("6")){
            Integer code = waylineJobDTO.getCode();
            String message = WaylineErrorCodeEnum.find(code).getMessage();
            description="巡检任务失败，原因为："+message;
        }
        return description;
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
    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 检查并移除监控超时（超过2小时）的任务
     */
    private void removeTimedOutTasks(long currentTime) {
        long timeoutThreshold = 2 * 60 * 60 * 1000; // 2小时

        // 先检查超时的任务
        List<String> timeoutTasks = new ArrayList<>();
        for (Map.Entry<String, Map<String, Long>> entry : monitoringTasks.entrySet()) {
            String taskCode = entry.getKey();
            Map<String, Long> taskInfo = entry.getValue();

            if (taskInfo != null && !taskInfo.isEmpty()) {
                Long startTime = taskInfo.values().iterator().next();
                if (startTime != null && (currentTime - startTime > timeoutThreshold)) {
                    timeoutTasks.add(taskCode);
                    log.warn("任务监控超时，强制移除: taskCode={}, 已监控时长={}ms",
                            taskCode, currentTime - startTime);
                }
            }
        }

        // 移除超时任务
        for (String taskCode : timeoutTasks) {
            monitoringTasks.remove(taskCode);
            uploadStallMap.remove(taskCode);
        }
    }

}
