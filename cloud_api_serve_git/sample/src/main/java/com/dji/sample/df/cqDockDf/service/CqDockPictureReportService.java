package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.config.VTaskConfig;
import com.df.framework.utils.HttpUtils;
import com.df.server.config.AnalyseStandardConfig;
import com.df.server.dto.robotDog.AnalyseImageInfo;
import com.df.server.dto.robotDog.AnalyseParamsRecReq;
import com.df.server.dto.robotDog.AnalyseParamsReq;
import com.df.server.dto.robotDog.TaskInfo;
import com.df.server.service.his.impl.HisExePointAnalyseServiceImpl;
import com.dji.sample.center.config.CenterNormalConfig;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.ftp.FtpUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.PatrolResultItem;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.df.cqDockDf.config.CqDockProperties;
import com.dji.sample.df.cqDockDf.dao.CqDockTaskPictureMapper;
import com.dji.sample.df.cqDockDf.dao.CqDockTaskRecordMapper;
import com.dji.sample.df.cqDockDf.model.dto.CqApiResponse;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskPictureEntity;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskRecordEntity;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import com.dji.sample.df.temperatureMeasurementDF.modol.TemParamEntity;
import com.dji.sample.df.temperatureMeasurementDF.modol.TemResultEntity;
import com.dji.sample.df.temperatureMeasurementDF.service.TemMeasureService;
import com.dji.sample.df.windDf.config.FjFileConfig;
import com.dji.sample.df.windDf.utils.FileNameUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 重庆EUA图片处理：EUA任务完成后查图落库 -> 原图先 FTP 上传并 Type=61 上报上级巡视系统
 * -> 后续分析成功后按张增量上报分析图/测温结果。
 */
@Slf4j
@Service
public class CqDockPictureReportService {

    private static final com.fasterxml.jackson.databind.ObjectReader POINT_SNAPSHOT_READER =
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .readerFor(com.fasterxml.jackson.databind.JsonNode.class)
                    .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    /**
     * 图片类型：可见光
     */
    private static final int POINT_TYPE_VISIBLE = 0;
    /**
     * 图片类型：红外
     */
    private static final int POINT_TYPE_INFRARED = 1;
    /**
     * 分析状态
     */
    private static final int ANALYSIS_PENDING = 0;
    private static final int ANALYSIS_DONE = 1;
    private static final int ANALYSIS_FAILED = 2;
    private static final int ANALYSIS_INFRARED_DONE = 3;

    @Autowired
    private CqDockApiService cqDockApiService;
    @Autowired
    private CqDockTaskPictureMapper cqDockTaskPictureMapper;
    @Autowired
    private CqDockTaskRecordMapper cqDockTaskRecordMapper;
    @Autowired
    private PatrolHostSocketClient patrolHostSocketClient;
    @Autowired
    private CenterNormalConfig centerConfig;
    @Autowired
    private CqDockProperties cqDockProperties;
    @Autowired
    private UniPointMapper2 uniPointMapper2;
    @Autowired
    private TemMeasureService temMeasureService;
    @Autowired
    private HisExePointAnalyseServiceImpl hisExePointAnalyseServiceImpl;
    @Autowired
    private AnalyseStandardConfig analyseStandardConfig;
    @Autowired
    private VTaskConfig vTaskConfig;
    @Autowired
    private HttpUtils httpUtils;
    @Autowired
    private FjFileConfig fjFileConfig;
    @Value("${server.port}")
    private String serverPort;

    /**
     * EUA任务完成（状态7）后的入口：HTTP查图 -> 落库+本地保存 -> 原图先统一上报 -> 分析结果后续按张增量上报。
     */
    public void fetchSaveAndReport(String taskCode, String taskName, String euaTaskId) {
//      获取并本地保存图片
        fetchAndSavePictures(taskCode, taskName, euaTaskId);
//      原图先统一上报，避免分析服务异常导致上级完全收不到图片
        reportOriginalPictures(taskCode, taskName, euaTaskId);
//      后续分析结果按张增量上报
        startAnalysisAndReport(taskCode, taskName, euaTaskId);
    }

    /**
     * 平台内EUA计划入口：HTTP查图 -> 落库+本地保存 -> 可见光分析/红外测温，不向上级巡视系统上报Type=61。
     */
    public void fetchSaveAndAnalyseOnly(String taskCode, String taskName, String euaTaskId) {
        fetchAndSavePictures(taskCode, taskName, euaTaskId);
        startAnalysisOnly(taskCode, euaTaskId);
    }

    /**
     * MQTT主动推送链路：只入库和下载到本地，分析上报统一走主动查图链路。
     */
    public void saveMqttPicture(String topic, JSONObject picture) {
        String euaTaskId = firstText(picture, "taskId", "euaTaskId", "eua_task_id");
        if (!StringUtils.hasText(euaTaskId)) {
            log.warn("EUA MQTT图片推送缺少taskId，忽略: topic={}, payload={}", topic, picture.toJSONString());
            return;
        }

        CqDockTaskRecordEntity record = findTaskRecord(euaTaskId);
        if (record == null) {
            log.warn("EUA MQTT图片任务记录不存在，忽略: topic={}, euaTaskId={}, kept=0, ignored=1", topic, euaTaskId);
            return;
        }
        String taskCode = record.getBusinessId();
        String taskName = record.getTaskName();
        Set<String> allowedPointCodes = loadAllowedPointCodes(record);
        String pointId = firstText(picture, "pointId", "deviceId", "photoPointId");
        if (allowedPointCodes == null || !StringUtils.hasText(pointId) || !allowedPointCodes.contains(pointId.trim())) {
            log.warn("EUA MQTT图片不属于任务间隔，忽略: topic={}, taskCode={}, euaTaskId={}, bayId={}, pointId={}, kept=0, ignored=1",
                    topic, taskCode, euaTaskId, record.getBayId(), pointId);
            return;
        }
//      目前MQTT推送图片没有上报上级，只通过主动调用图片接口进行上报
        savePicture(taskCode, taskName, euaTaskId, picture, "mqtt");
        log.info("EUA MQTT图片已入库/本地保存: topic={}, taskCode={}, euaTaskId={}, pictureName={}, totalNum={}",
                topic, taskCode, euaTaskId, picture.getString("pictureName"), picture.getString("totalNum"));
    }

    /**
     * 智能分析回调是否属于重庆EUA图片。
     */
    public boolean isCqAnalyseRequest(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return false;
        }
        Integer count = cqDockTaskPictureMapper.selectCount(new LambdaQueryWrapper<CqDockTaskPictureEntity>()
                .eq(CqDockTaskPictureEntity::getRequestId, requestId));
        return count != null && count > 0;
    }

    /**
     * 智能分析回调处理（可见光）：更新分析结果；成功返回分析图时单张增量上报。
     */
    public void handleAnalyseCallback(com.df.server.dto.robotDog.AnalyseParamsRecReq analyseParamsRecReq) {
        String requestId = analyseParamsRecReq.getRequestId();
        CqDockTaskPictureEntity row = cqDockTaskPictureMapper.selectOne(new LambdaQueryWrapper<CqDockTaskPictureEntity>()
                .eq(CqDockTaskPictureEntity::getRequestId, requestId)
                .last("LIMIT 1"));
        if (row == null) {
            log.warn("CQ分析回调未找到对应图片记录: requestId={}", requestId);
            return;
        }

        String code = "2002";
        String pointValUnit = "算法分析失败（没有分析结果）";
        String pointVal = "";
        String resImageUrl = "";
        List<AnalyseParamsRecReq.ResultList> resultsList = analyseParamsRecReq.getResultList();
        if (resultsList != null && !resultsList.isEmpty()) {
            AnalyseParamsRecReq.ResultList resultList = resultsList.get(0);
            if (resultList != null && resultList.getResults() != null) {
                List<AnalyseParamsRecReq.ResultList.Result> results = resultList.getResults();
                if (!results.isEmpty()) {
                    AnalyseParamsRecReq.ResultList.Result result = results.get(0);
                    code = result.getCode();
                    resImageUrl = result.getResImageUrl();
                    pointValUnit = result.getDesc();
                    if ("2002".equals(code)) {
                        pointValUnit = StringUtils.hasText(pointValUnit) ? "算法分析失败（" + pointValUnit + "）" : "算法分析失败";
                    }
                    if ("2001".equals(code)) {
                        pointValUnit = "图像数据错误";
                    }
                }
            }
        }
        if ("2000".equals(code)) {
            try {
                UniPoint uniPoint = resolveUniPoint(row);
                Integer pointAnalyseCategory = uniPoint == null ? null : uniPoint.getPointAnalyseCategory();
                java.util.HashMap<String, String> valueResultMap =
                        hisExePointAnalyseServiceImpl.updatePointVal(analyseParamsRecReq, pointAnalyseCategory);
                pointVal = valueResultMap.get("point_val");
                pointValUnit = valueResultMap.get("point_val_unit");
                resImageUrl = valueResultMap.get("resImageUrl");
            } catch (Exception e) {
                log.error("CQ可见光分析结果解析失败: requestId={}", requestId, e);
                pointValUnit = "分析结果解析失败（" + e.getMessage() + "）";
            }
        }

        boolean analyseOk = "2000".equals(code);
        row.setAnalysisImageUrl(StringUtils.hasText(resImageUrl) ? resImageUrl : null);
        if (analyseOk && StringUtils.hasText(resImageUrl)) {
            try {
                resolveAnalysisLocalImage(row.getBusinessId(), row);
            } catch (Exception e) {
                analyseOk = false;
                pointValUnit = "分析服务未生成或未上传结果图（" + e.getMessage() + "）";
                log.error("CQ可见光分析成功但结果图不可用: taskCode={}, euaTaskId={}, pictureName={}, resImageUrl={}",
                        row.getBusinessId(), row.getEuaTaskId(), row.getPictureName(), resImageUrl, e);
            }
        }
        row.setPointVal(pointVal);
        row.setPointValUnit(pointValUnit);
        row.setAnalysisStatus(analyseOk ? ANALYSIS_DONE : ANALYSIS_FAILED);
        row.setUpdateTime(new Date());
        cqDockTaskPictureMapper.updateById(row);
        log.info("CQ可见光分析回调完成: taskCode={}, euaTaskId={}, pictureName={}, code={}, resImageUrl={}",
                row.getBusinessId(), row.getEuaTaskId(), row.getPictureName(), code, resImageUrl);

        if (analyseOk && StringUtils.hasText(row.getAnalysisImageUrl()) && shouldReportToPatrol(row)) {
            reportSingleAnalysisResult(row);
        } else if (!analyseOk) {
            log.warn("CQ可见光分析失败，仅记录状态不再阻塞原图上报: taskCode={}, pictureName={}, pointValUnit={}",
                    row.getBusinessId(), row.getPictureName(), row.getPointValUnit());
        } else if (!shouldReportToPatrol(row)) {
            log.info("CQ平台内EUA任务可见光分析完成，不上报巡视: taskCode={}, pictureName={}", row.getBusinessId(), row.getPictureName());
        }
    }

    private int fetchAndSavePictures(String taskCode, String taskName, String euaTaskId) {
        CqApiResponse response = cqDockApiService.pictureList(euaTaskId);
        if (response == null || !(Boolean.TRUE.equals(response.getSuccess()) || Objects.equals(response.getCode(), 200))) {
            log.warn("EUA图片列表获取失败: taskCode={}, euaTaskId={}, msg={}", taskCode, euaTaskId, response == null ? null : response.getMsg());
            return 0;
        }
        List<JSONObject> pictures = extractPictures(response.getData());
        if (pictures.isEmpty()) {
            log.info("EUA图片列表为空: taskCode={}, euaTaskId={}", taskCode, euaTaskId);
            return 0;
        }
        CqDockTaskRecordEntity record = StringUtils.hasText(taskCode) && StringUtils.hasText(euaTaskId)
                ? cqDockTaskRecordMapper.selectOne(new LambdaQueryWrapper<CqDockTaskRecordEntity>()
                .eq(CqDockTaskRecordEntity::getBusinessId, taskCode)
                .eq(CqDockTaskRecordEntity::getEuaTaskId, euaTaskId)
                .last("LIMIT 1")) : null;
        Set<String> allowedPointCodes = loadAllowedPointCodes(record);
        if (allowedPointCodes == null) {
            String bayId = record == null ? null : record.getBayId();
            log.warn("EUA图片间隔过滤失败，忽略本次列表: taskCode={}, euaTaskId={}, bayId={}, reason={}, kept=0, ignored={}",
                    taskCode, euaTaskId, bayId, record == null ? "任务记录不存在" : "任务间隔为空", pictures.size());
            return 0;
        }
        if (allowedPointCodes.isEmpty()) {
            String bayId = record.getBayId();
            log.warn("EUA图片间隔无有效点位，忽略本次列表: taskCode={}, euaTaskId={}, bayId={}, kept=0, ignored={}",
                    taskCode, euaTaskId, bayId, pictures.size());
            return 0;
        }
        int count = 0;
        int kept = 0;
        for (JSONObject picture : pictures) {
            String pointId = firstText(picture, "pointId", "deviceId", "photoPointId");
            if (!StringUtils.hasText(pointId) || !allowedPointCodes.contains(pointId.trim())) {
                continue;
            }
            kept++;
//          保存图片
            if (savePicture(taskCode, taskName, euaTaskId, picture, "http") != null) {
                count++;
            }
        }
        log.info("EUA图片间隔过滤完成: taskCode={}, euaTaskId={}, bayId={}, kept={}, ignored={}",
                taskCode, euaTaskId, record.getBayId(), kept, pictures.size() - kept);
        return count;
    }

    private Set<String> loadAllowedPointCodes(CqDockTaskRecordEntity record) {
        if (record == null) {
            return null;
        }
        if (record.getPointCodes() != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode parsed = POINT_SNAPSHOT_READER.readValue(record.getPointCodes());
                if (parsed == null || !parsed.isArray()) {
                    return Collections.emptySet();
                }
                Set<String> snapshot = new HashSet<>();
                for (com.fasterxml.jackson.databind.JsonNode code : parsed) {
                    if (!code.isTextual() || !StringUtils.hasText(code.textValue())) {
                        return Collections.emptySet();
                    }
                    snapshot.add(code.textValue().trim());
                }
                return snapshot;
            } catch (java.io.IOException | RuntimeException e) {
                log.warn("EUA任务点位快照无效，拒绝接收图片: taskCode={}, euaTaskId={}", record.getBusinessId(), record.getEuaTaskId());
                return Collections.emptySet();
            }
        }
        if (!StringUtils.hasText(record.getBayId())) {
            return null;
        }
        Set<String> allowedPointCodes = new HashSet<>();
        List<UniPoint> bayPoints = uniPointMapper2.selectList(new LambdaQueryWrapper<UniPoint>()
                .eq(UniPoint::getBayId, record.getBayId()));
        for (UniPoint point : bayPoints) {
            if (StringUtils.hasText(point.getPointCode())) {
                allowedPointCodes.add(point.getPointCode().trim());
            }
        }
        return allowedPointCodes;
    }

    /**
     * 分析调度：红外同步测温，可见光提交智能分析服务等回调；无待分析项时直接上报。
     */
    private void startAnalysisAndReport(String taskCode, String taskName, String euaTaskId) {
        startAnalysis(taskCode, taskName, euaTaskId, true);
    }

    private void startAnalysisOnly(String taskCode, String euaTaskId) {
        startAnalysis(taskCode, null, euaTaskId, false);
    }

    private void startAnalysis(String taskCode, String taskName, String euaTaskId, boolean reportToPatrol) {
//      查询到此任务数据库里的图片
        List<CqDockTaskPictureEntity> rows = cqDockTaskPictureMapper.selectList(new LambdaQueryWrapper<CqDockTaskPictureEntity>()
                .eq(CqDockTaskPictureEntity::getBusinessId, taskCode)
                .eq(CqDockTaskPictureEntity::getEuaTaskId, euaTaskId));
        if (rows.isEmpty()) {
            log.warn("CQ无图片可分析上报: taskCode={}, euaTaskId={}", taskCode, euaTaskId);
            return;
        }
        int pendingCallback = 0;
        for (CqDockTaskPictureEntity row : rows) {
            // 已有终态的行（重复触发/断点续跑）跳过
            if (Objects.equals(row.getAnalysisStatus(), ANALYSIS_DONE)
                    || Objects.equals(row.getAnalysisStatus(), ANALYSIS_INFRARED_DONE)
                    || Objects.equals(row.getAnalysisStatus(), ANALYSIS_FAILED)) {
                continue;
            }
            try {
                UniPoint uniPoint = resolveUniPoint(row);
                if (uniPoint == null) {
                    markAnalysisFailed(row, "未在点位表中匹配到点位(point_code=" + row.getPointId() + ")");
                    continue;
                }
                if (row.getPointType() != null && row.getPointType() == POINT_TYPE_INFRARED) {
//                  红外图片测温
                    measureInfrared(row, uniPoint, reportToPatrol);
                } else {
//                  可见光图片对接分析
                    if (submitVisibleAnalysis(row, uniPoint, taskName, euaTaskId)) {
                        pendingCallback++;
                    }
                }
            } catch (Exception e) {
                markAnalysisFailed(row, e.getMessage());
                log.error("CQ图片分析调度失败: taskCode={}, pictureName={}", taskCode, row.getPictureName(), e);
            }
        }
        if (pendingCallback == 0) {
            log.info("CQ无待回调可见光图片，原图已先行上报，分析成功项已按张增量上报: taskCode={}, euaTaskId={}", taskCode, euaTaskId);
        } else {
            log.info("CQ图片已提交智能分析，等待回调: taskCode={}, euaTaskId={}, 待回调数量={}", taskCode, euaTaskId, pendingCallback);
        }
    }

    /**
     * pointId 即 df_uni_point.point_code。
     */
    private UniPoint resolveUniPoint(CqDockTaskPictureEntity row) {
        if (!StringUtils.hasText(row.getPointId())) {
            return null;
        }
        return uniPointMapper2.selectOne(new LambdaQueryWrapper<UniPoint>()
                .eq(UniPoint::getPointCode, row.getPointId().trim())
                .last("LIMIT 1"));
    }

    /**
     * 红外：大疆测温SDK同步测温（需要点位红外坐标，前期可以手动先填入测试）。
     */
    private void measureInfrared(CqDockTaskPictureEntity row, UniPoint uniPoint) {
        measureInfrared(row, uniPoint, true);
    }

    private void measureInfrared(CqDockTaskPictureEntity row, UniPoint uniPoint, boolean reportToPatrol) {
        String coordinate = uniPoint.getInfraredImageCoordinate();
        TemParamEntity temParamEntity = parseInfraredCoordinate(coordinate);
        if (temParamEntity == null) {
            markAnalysisFailed(row, "红外图片坐标无效: " + coordinate);
            return;
        }
        TemResultEntity temResult = temMeasureService.getTemByLocalImagePath(row.getLocalPath(), temParamEntity);
        if (temResult.getError() != null) {
            markAnalysisFailed(row, "红外测温失败：" + temResult.getError());
            return;
        }
        row.setPointVal("最高温度：" + temResult.getMax_tem() + ",最低温度：" + temResult.getMin_tem() + ",平均温度：" + temResult.getAverage_tem());
        row.setPointValUnit("正常");
        row.setAnalysisStatus(ANALYSIS_INFRARED_DONE);
        row.setUpdateTime(new Date());
        cqDockTaskPictureMapper.updateById(row);
        log.info("CQ红外测温完成: taskCode={}, pictureName={}, pointVal={}", row.getBusinessId(), row.getPictureName(), row.getPointVal());
        if (reportToPatrol) {
            reportSingleAnalysisResult(row);
        } else {
            log.info("CQ平台内EUA任务红外测温完成，不上报巡视: taskCode={}, pictureName={}", row.getBusinessId(), row.getPictureName());
        }
    }

    private boolean shouldReportToPatrol(CqDockTaskPictureEntity row) {
        CqDockTaskRecordEntity record = findTaskRecord(row.getEuaTaskId());
        return record == null || record.getReportToPatrol() == null || record.getReportToPatrol() == 1;
    }

    private TemParamEntity parseInfraredCoordinate(String coordinate) {
        if (coordinate == null || coordinate.isEmpty()) {
            return null;
        }
        String trimmed = coordinate.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return null;
        }
        String[] parts = trimmed.substring(1, trimmed.length() - 1).split(",");
        if (parts.length != 4) {
            return null;
        }
        try {
            TemParamEntity temParamEntity = new TemParamEntity();
            temParamEntity.setLeft_top_x(Integer.parseInt(parts[0].trim()));
            temParamEntity.setLeft_top_y(Integer.parseInt(parts[1].trim()));
            temParamEntity.setRight_bottom_x(Integer.parseInt(parts[2].trim()));
            temParamEntity.setRight_bottom_y(Integer.parseInt(parts[3].trim()));
            return temParamEntity;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 可见光：提交智能分析服务，等 /picAnalyseRetNotify 回调。
     */
    private boolean submitVisibleAnalysis(CqDockTaskPictureEntity row, UniPoint uniPoint, String taskName, String euaTaskId) {
        if (!StringUtils.hasText(row.getLocalPath()) || !new File(row.getLocalPath()).exists()) {
            markAnalysisFailed(row, "本地图片不存在，无法送分析: " + row.getLocalPath());
            return false;
        }
        String requestId = UUID.randomUUID().toString();
        // 与 handleUavResult 一致：唯一编码=点位编码+图片类型，objectId 后缀 _2 表示无人机
        String regId = uniPoint.getPointCode() + "V";
        AnalyseParamsReq analyseParamsReq = new AnalyseParamsReq();
        analyseParamsReq.setObjectId(regId + "_2");
        // 分析服务按 FTP 根目录的相对路径读取图片，需去掉 /ftpdir/admin_files 前缀（与 electricInspection 一致）
        analyseParamsReq.setImagePathListByStandard(Lists.newArrayList(toAnalysisRelativePath(row.getLocalPath())), analyseStandardConfig.isDocument57());
        String pointAnalyseType = uniPoint.getPointAnalyseType();
        if (StringUtils.hasText(pointAnalyseType)) {
            analyseParamsReq.setTypeList(java.util.Arrays.asList(pointAnalyseType.split(",")));
        }
        List<AnalyseParamsReq> analyseParamsReqList = new ArrayList<>();
        analyseParamsReqList.add(analyseParamsReq);
        AnalyseImageInfo analyseImageInfo = new AnalyseImageInfo();
        analyseImageInfo.setRequestId(requestId);
        analyseImageInfo.setAnalyseParamsReqList(analyseParamsReqList);
        analyseImageInfo.setTaskInfo(new TaskInfo(uniPoint.getSubCode(), euaTaskId, taskName, null, null));
        sendAnalyse(analyseImageInfo);

        row.setRequestId(requestId);
//      开始图片表都设置为待分析状态
        row.setAnalysisStatus(ANALYSIS_PENDING);
        row.setUpdateTime(new Date());
        cqDockTaskPictureMapper.updateById(row);
        return true;
    }

    private void sendAnalyse(AnalyseImageInfo analyseImageInfo) {
        String analyseUrl = vTaskConfig.getAnalyseUrl();
        String analyseResultIp = vTaskConfig.getLocalIp();
        String analyseResultPort = serverPort;
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("requestId", analyseImageInfo.getRequestId());
        params.put("requestHostIp", analyseResultIp);
        params.put("requestHostPort", analyseResultPort);
        params.put("objectList", analyseImageInfo.getAnalyseParamsReqList());
        try {
            log.info("【CQ_发送智能分析】 请求url：{}，请求：{}", analyseUrl, JSONObject.toJSONString(params));
            httpUtils.sendPostJson(analyseUrl, JSONObject.toJSONString(params));
        } catch (Exception e) {
            log.error("【CQ_发送智能分析请求】异常！ {}", e.getMessage());
        }
    }

    /**
     * 去掉 FTP 根目录前缀（/ftpdir/admin_files），转成分析服务可读取的相对路径。
     */
    private String toAnalysisRelativePath(String localPath) {
        String ftpRoot = fjFileConfig.getRecfilePath();
        if (StringUtils.hasText(ftpRoot) && localPath.startsWith(ftpRoot)) {
            String relative = localPath.substring(ftpRoot.length());
            return relative.startsWith("/") ? relative : "/" + relative;
        }
        return localPath;
    }

    private void markAnalysisFailed(CqDockTaskPictureEntity row, String msg) {
        row.setAnalysisStatus(ANALYSIS_FAILED);
        row.setPointValUnit(msg);
        row.setUpdateTime(new Date());
        cqDockTaskPictureMapper.updateById(row);
    }

    /**
     * 原图先统一上传 FTP 并上报巡视上级；reportStatus 仅标记原图上报状态。
     */
    private synchronized void reportOriginalPictures(String taskCode, String taskName, String euaTaskId) {
        List<CqDockTaskPictureEntity> rows = cqDockTaskPictureMapper.selectList(new LambdaQueryWrapper<CqDockTaskPictureEntity>()
                .eq(CqDockTaskPictureEntity::getBusinessId, taskCode)
                .eq(CqDockTaskPictureEntity::getEuaTaskId, euaTaskId));
        if (rows.isEmpty()) {
            return;
        }
        boolean allReported = rows.stream().allMatch(row -> Objects.equals(row.getReportStatus(), 1));
        if (allReported) {
            return;
        }

        List<PatrolResultItem> items = new ArrayList<>();
        List<CqDockTaskPictureEntity> reportedRows = new ArrayList<>();
        for (CqDockTaskPictureEntity row : rows) {
            if (Objects.equals(row.getReportStatus(), 1)) {
                continue;
            }
            try {
                String fileType = row.getPointType() != null && row.getPointType() == POINT_TYPE_INFRARED ? "1" : "2";
                String originalFtpPath = uploadPicture(taskCode, row.getLocalPath());
                items.add(buildResultItem(taskCode, taskName, euaTaskId, row, fileType, originalFtpPath));
                row.setFtpPath(originalFtpPath);
                reportedRows.add(row);
            } catch (Exception e) {
                row.setReportStatus(2);
                row.setReportMsg("原图上报失败：" + e.getMessage());
                row.setUpdateTime(new Date());
                cqDockTaskPictureMapper.updateById(row);
                log.error("CQ原图上报失败: taskCode={}, pictureName={}", taskCode, row.getPictureName(), e);
            }
        }

        if (items.isEmpty()) {
            log.info("CQ无待上报原图: taskCode={}, euaTaskId={}", taskCode, euaTaskId);
            return;
        }
        sendPatrolResultItems(items);
        log.info("CQ原图统一上报完成: taskCode={}, euaTaskId={}, 图片数={}, item数={}",
                taskCode, euaTaskId, rows.size(), items.size());

        Date now = new Date();
        for (CqDockTaskPictureEntity row : reportedRows) {
            row.setReportStatus(1);
            row.setReportMsg("原图上报成功");
            row.setUpdateTime(now);
            cqDockTaskPictureMapper.updateById(row);
        }
    }

    /**
     * 分析成功后单张增量上报：可见光上报分析图，红外上报原图并携带测温结果。
     */
    private void reportSingleAnalysisResult(CqDockTaskPictureEntity row) {
        try {
            String fileType = row.getPointType() != null && row.getPointType() == POINT_TYPE_INFRARED ? "1" : "2";
            String filePath;
            if (row.getPointType() != null && row.getPointType() == POINT_TYPE_INFRARED) {
                filePath = StringUtils.hasText(row.getFtpPath()) ? row.getFtpPath() : uploadPicture(row.getBusinessId(), row.getLocalPath());
            } else {
                String analysisLocalPath = resolveAnalysisLocalImage(row.getBusinessId(), row);
                filePath = uploadPicture(row.getBusinessId(), analysisLocalPath);
            }
            PatrolResultItem item = buildResultItem(row.getBusinessId(), row.getTaskName(), row.getEuaTaskId(), row, fileType, filePath);
            item.setValue(defaultText(row.getPointVal()));
            item.setValue_unit(defaultText(row.getPointValUnit()));
            sendPatrolResultItems(Lists.newArrayList(item));
            log.info("CQ分析结果增量上报完成: taskCode={}, euaTaskId={}, pictureName={}, filePath={}",
                    row.getBusinessId(), row.getEuaTaskId(), row.getPictureName(), filePath);
        } catch (Exception e) {
            row.setAnalysisStatus(ANALYSIS_FAILED);
            row.setPointValUnit("分析结果图上报失败（" + e.getMessage() + "）");
            row.setReportMsg("原图已上报，分析结果图上报失败：" + e.getMessage());
            row.setUpdateTime(new Date());
            cqDockTaskPictureMapper.updateById(row);
            log.error("CQ分析结果增量上报失败: taskCode={}, pictureName={}", row.getBusinessId(), row.getPictureName(), e);
        }
    }

    private void sendPatrolResultItems(List<PatrolResultItem> items) {
        PatrolHostCommand commandData = patrolHostSocketClient.getBaseCommand("61", "", centerConfig.getStationCode());
        commandData.addItems(items);
        patrolHostSocketClient.sendCommand(commandData, PatrolResultItem.class);
    }

    /**
     * 可见光分析图为 URL 时先下载到本地；为本地/共享路径或 FTP 根目录相对路径时直接使用。
     */
    private String resolveAnalysisLocalImage(String taskCode, CqDockTaskPictureEntity row) throws Exception {
        String urlOrPath = row.getAnalysisImageUrl();
        if (!StringUtils.hasText(urlOrPath)) {
            throw new IllegalArgumentException("分析结果图地址为空");
        }
        File file = new File(urlOrPath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }

        String relativePath = normalizeAnalysisRelativePath(urlOrPath);
        if (StringUtils.hasText(relativePath)) {
            File relativeFile = new File(fjFileConfig.getRecfilePath(), relativePath.startsWith("/") ? relativePath.substring(1) : relativePath);
            if (relativeFile.exists()) {
                return relativeFile.getAbsolutePath();
            }
            throw new IllegalStateException("分析结果图相对路径未找到本地文件: resImageUrl=" + urlOrPath
                    + ", localPath=" + relativeFile.getAbsolutePath());
        }

        Path dir = Paths.get(cqDockProperties.getPictureLocalPath(), taskCode, "analysis");
        Files.createDirectories(dir);
        String name = FileNameUtils.convertChineseToPinyinInitials(
                StringUtils.hasText(row.getPictureName()) ? row.getPictureName() : UUID.randomUUID().toString());
        String analysisName = name.contains(".") ? name.replaceAll("(\\.[^.]+)$", "_analysis$1") : name + "_analysis.jpg";
        Path target = dir.resolve(analysisName);
        try (InputStream inputStream = new URL(urlOrPath).openStream();
             java.io.FileOutputStream outputStream = new FileOutputStream(target.toFile())) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        }
        return target.toAbsolutePath().toString();
    }

    private String normalizeAnalysisRelativePath(String urlOrPath) {
        String value = urlOrPath.trim().replace('\\', '/');
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return null;
        }
        String ftpRoot = fjFileConfig.getRecfilePath();
        if (StringUtils.hasText(ftpRoot)) {
            String normalizedFtpRoot = ftpRoot.replace('\\', '/');
            if (!normalizedFtpRoot.endsWith("/")) {
                normalizedFtpRoot = normalizedFtpRoot + "/";
            }
            if (value.startsWith(normalizedFtpRoot)) {
                return "/" + value.substring(normalizedFtpRoot.length());
            }
        }
        while (value.startsWith("//")) {
            value = value.substring(1);
        }
        return value.startsWith("/") ? value : null;
    }

    private PatrolResultItem buildResultItem(String taskCode, String taskName, String euaTaskId,
                                             CqDockTaskPictureEntity entity, String fileType, String filePath) {
        PatrolResultItem item = new PatrolResultItem();
        item.setPatroldevice_name("EUA平台无人机");
        item.setPatroldevice_code("--");
        item.setTask_name(taskName);
        item.setTask_code(taskCode);
        item.setDevice_name(defaultText(entity.getPointName()));
        item.setDevice_id(defaultText(entity.getPointId()));
        item.setValue(defaultText(entity.getPointVal()));
        item.setValue_unit(defaultText(entity.getPointValUnit()));
        item.setUnit("");
        item.setTime(DateUtils.getNowDateTimeStr());
        item.setRecognition_type("");
        item.setFile_path(filePath);
        item.setFile_type(fileType);
        item.setRectangle("");
        item.setTask_patrolled_id(euaTaskId);
        item.setObj_id("---");
        item.setValid("1");
        return item;
    }

    private List<JSONObject> extractPictures(JSONObject data) {
        if (data == null) {
            return Collections.emptyList();
        }
        JSONArray array = data.getJSONArray("pictureList");
        if (array == null) {
            array = data.getJSONArray("list");
        }
        if (array == null) {
            array = data.getJSONArray("records");
        }
        if (array == null) {
            return Collections.emptyList();
        }
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 落库 + 下载本地（不含上报，上报统一在分析完成后执行）。
     */
    private CqDockTaskPictureEntity savePicture(String taskCode, String taskName, String euaTaskId,
                                                JSONObject picture, String source) {
//      todo ，mqtt主动上报链路上来的，pictureId取的是整个上报json的主键id
        String pictureId = firstText(picture, "pictureId", "photoId", "id");
        String pictureUrl = firstText(picture, "pictureUrl", "url", "fileUrl", "path");
        String pictureName = firstText(picture, "pictureName", "fileName", "name");
        if (!StringUtils.hasText(pictureName)) {
            pictureName = buildPictureName(pictureUrl, pictureId);
        }

        CqDockTaskPictureEntity entity = findPicture(taskCode, euaTaskId, pictureName, pictureId, pictureUrl);
        Date now = new Date();
        boolean isNew = false;

        if (entity == null) {
            entity = new CqDockTaskPictureEntity();
            entity.setBusinessId(taskCode);
            entity.setEuaTaskId(euaTaskId);
            entity.setReportStatus(0);
            entity.setCreateTime(now);
            isNew = true;
        }
        if (StringUtils.hasText(taskName)) {
            entity.setTaskName(taskName);
        }
        if (StringUtils.hasText(pictureId)) {
            entity.setPictureId(pictureId);
        }
        if (StringUtils.hasText(pictureName)) {
            entity.setPictureName(pictureName);
        }
        if (StringUtils.hasText(pictureUrl)) {
            entity.setPictureUrl(pictureUrl);
        }
        String pointId = firstText(picture, "pointId", "deviceId", "photoPointId");
        String pointName = firstText(picture, "pointName", "deviceName", "photoPointName");
        if (StringUtils.hasText(pointId)) {
            entity.setPointId(pointId.trim());
        }
        if (StringUtils.hasText(pointName)) {
            entity.setPointName(pointName);
        }
        // EUA图片列表新增 pointType：0可见光 1红外，缺省按可见光处理
        entity.setPointType(parsePointType(picture));
        entity.setRawData(picture.toJSONString());
        entity.setUpdateTime(now);

        if (isNew) {
            cqDockTaskPictureMapper.insert(entity);
        } else {
            cqDockTaskPictureMapper.updateById(entity);
        }
//      存数据库及保存本地
        try {
            if (!StringUtils.hasText(entity.getLocalPath()) || !new File(entity.getLocalPath()).exists()) {
                entity.setLocalPath(downloadPicture(pictureUrl, taskCode, pictureName));
            }
            if (!Objects.equals(entity.getReportStatus(), 1)) {
                entity.setReportStatus(0);
                entity.setReportMsg(source + "已入库并保存本地，待分析");
            }
        } catch (Exception e) {
            entity.setReportStatus(2);
            entity.setReportMsg(e.getMessage());
            log.error("EUA图片处理失败: source={}, taskCode={}, euaTaskId={}, pictureName={}, pictureUrl={}",
                    source, taskCode, euaTaskId, pictureName, pictureUrl, e);
        }
        entity.setUpdateTime(new Date());
        cqDockTaskPictureMapper.updateById(entity);
        return entity;
    }

    private Integer parsePointType(JSONObject picture) {
        String value = firstText(picture, "pointType", "picType", "pictureType");
        if (!StringUtils.hasText(value)) {
            return POINT_TYPE_VISIBLE;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            log.warn("EUA图片pointType无法解析，按可见光处理: {}", value);
            return POINT_TYPE_VISIBLE;
        }
    }

    private CqDockTaskPictureEntity findPicture(String taskCode, String euaTaskId, String pictureName, String pictureId, String pictureUrl) {
        LambdaQueryWrapper<CqDockTaskPictureEntity> wrapper = new LambdaQueryWrapper<CqDockTaskPictureEntity>()
                .eq(CqDockTaskPictureEntity::getBusinessId, taskCode)
                .eq(CqDockTaskPictureEntity::getEuaTaskId, euaTaskId);
        if (StringUtils.hasText(pictureName)) {
            wrapper.eq(CqDockTaskPictureEntity::getPictureName, pictureName);
        } else if (StringUtils.hasText(pictureId)) {
            wrapper.eq(CqDockTaskPictureEntity::getPictureId, pictureId);
        } else {
            wrapper.eq(CqDockTaskPictureEntity::getPictureUrl, pictureUrl);
        }
        return cqDockTaskPictureMapper.selectOne(wrapper.last("LIMIT 1"));
    }

    private CqDockTaskRecordEntity findTaskRecord(String euaTaskId) {
        if (!StringUtils.hasText(euaTaskId)) {
            return null;
        }
        return cqDockTaskRecordMapper.selectOne(new LambdaQueryWrapper<CqDockTaskRecordEntity>()
                .eq(CqDockTaskRecordEntity::getEuaTaskId, euaTaskId)
                .last("LIMIT 1"));
    }

    private String downloadPicture(String pictureUrl, String taskCode, String pictureName) throws Exception {
        if (!StringUtils.hasText(pictureUrl)) {
            throw new IllegalArgumentException("EUA图片地址为空");
        }
//      设置本地存储路径
        Path dir = Paths.get(cqDockProperties.getPictureLocalPath(), taskCode);
        Files.createDirectories(dir);
        String safeName = FileNameUtils.convertChineseToPinyinInitials(pictureName);
        Path target = dir.resolve(safeName);
        try (InputStream inputStream = new URL(pictureUrl).openStream();
             java.io.FileOutputStream outputStream = new FileOutputStream(target.toFile())) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        }
        log.info("EUA图片下载完成: url={}, localPath={}, size={}", pictureUrl, target.toAbsolutePath(), Files.size(target));
        return target.toAbsolutePath().toString();
    }

    private String uploadPicture(String taskCode, String localPath) {
        String destDir = "/" + taskCode;
        File localFile = new File(localPath);
        if (!localFile.exists()) {
            throw new IllegalStateException("EUA图片本地文件不存在: " + localPath);
        }
        String destName = FileNameUtils.convertChineseToPinyinInitials(localFile.getName());
        // 本地联调开关：无中心FTP环境时跳过真实上传，保证 Type=61 上报链路可验证
        if (Boolean.TRUE.equals(cqDockProperties.getMockSkipFtp())) {
            return String.format("/mockftp/%s/%s", destDir, destName);
        }
        Boolean uploaded = FtpUtils.getInstance().uploadFileToCenterNormal(localFile, destDir, destName);
        if (!Boolean.TRUE.equals(uploaded)) {
            throw new IllegalStateException("EUA图片FTP上传失败: " + localPath);
        }
        return String.format("%s/%s", destDir, destName);
    }

    private String firstText(JSONObject object, String... keys) {
        if (object == null) {
            return null;
        }
        for (String key : keys) {
            String value = object.getString(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private String buildPictureName(String pictureUrl, String pictureId) {
        if (StringUtils.hasText(pictureUrl)) {
            String path = pictureUrl.split("\\?", 2)[0];
            int index = path.lastIndexOf('/');
            if (index >= 0 && index < path.length() - 1) {
                return path.substring(index + 1);
            }
        }
        return StringUtils.hasText(pictureId) ? pictureId + ".jpg" : System.currentTimeMillis() + ".jpg";
    }
}
