package com.dji.sample.df.electricInspectionDf.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.config.FileConfig;
import com.df.framework.config.VTaskConfig;
import com.df.framework.utils.CustomStringUtils;
import com.df.framework.utils.HttpUtils;
import com.df.server.dto.robotDog.AnalyseImageInfo;
import com.df.server.dto.robotDog.AnalyseParamsRecReq;
import com.df.server.dto.robotDog.AnalyseParamsReq;
import com.df.server.dto.robotDog.TaskInfo;
import com.df.server.entity.his.HisUniTaskItemFileEntity;
import com.df.server.entity.his.HisUniTaskItemPointsEntity;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.mapper.his.HisUniTaskItemFileMapper;
import com.df.server.mapper.his.HisUniTaskItemPointsMapper;
import com.df.server.mapper.uni.UniPointMapper;
import com.df.server.service.his.HisExePointAnalyseService;
import com.df.server.service.his.HisUniTaskItemAlarmService;
import com.df.server.service.his.HisUniTaskItemPointsService;
import com.df.server.service.his.impl.HisExePointAnalyseServiceImpl;
import com.df.server.service.robotDog.impl.RobotDogPatrolServiceImpl;
import com.df.server.service.uni.UniPointThresholdService;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.ResultService;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.mediaDf.service.IFileServiceDf;
import com.dji.sample.media.dao.IFileMapper;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

@Service
@Slf4j
public class ResultServiceImpl implements ResultService {

    @Autowired
    IFileServiceDf iFileServiceDf;
    @Autowired
    UniPointMapper uniPointMapper;
    @Autowired
    HisExePointAnalyseService hisExePointAnalyseService;
    @Autowired
    IFileService fileService;
    @Autowired
    RobotDogPatrolServiceImpl robotDogPatrolService;
    @Autowired
    HisUniTaskItemFileMapper hisUniTaskItemFileMapper;
    @Autowired
    FileConfig fileConfig;
    @Autowired
    private IWaylineJobMapper waylineJobMapper;
    @Autowired
    private PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;
    @Autowired
    IFileMapper iFileMapper;
    @Autowired
    HisExePointAnalyseServiceImpl hisExePointAnalyseServiceImpl;
    @Autowired
    private UniPointThresholdService uniPointThresholdService;
    @Autowired
    private HisUniTaskItemPointsMapper hisUniTaskItemPointsMapper;
    @Autowired
    private HisUniTaskItemAlarmService hisUniTaskItemAlarmService;
    @Autowired
    private HisUniTaskItemPointsService hisUniTaskItemPointsService;
    @Autowired
    private HttpUtils httpUtils;
    @Autowired
    private VTaskConfig vTaskConfig;
    @Value("${server.port}")
    private String serverPort;

    //      只针对一个航点对应一个点位（因为点位导入的时候就是点位与一个航点预置位号绑定）
    @Override
    public void handleUavResult(String workspaceId,String jobId, HttpServletResponse response) throws Exception {

        List<MediaFileDTO> mediaFileDTOList = iFileServiceDf.getFilesByJobId(jobId);
//      用job_id找到plan_id,再用plan_id找到航点
        WaylineJobEntity waylineJobEntity = waylineJobMapper
                .selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, jobId));
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper
                .selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                        .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
        String waylinePointPos = pubWaylineJobPlanDfEntity.getWaylinePointPos();
        String[] numbers = waylinePointPos.split(",");

        for (MediaFileDTO mediaFileDTO : mediaFileDTOList) {
            String fileId = mediaFileDTO.getFileId();
            URL url = fileService.getObjectUrl(workspaceId, fileId);
            String base64 = convertImageToBase64(url.toString());
            String fileName = mediaFileDTO.getFileName();
            if(!fileName.contains("_V_航点")){
                continue;
            }
            int pointPos = extractWaypointNumber(fileName);
            if(!Arrays.asList(numbers).contains(String.valueOf(pointPos))){
                continue;
            }
            HisUniTaskItemPointsEntity hisUniTaskItemPoint =
                    hisUniTaskItemPointsMapper.getHisPointByUavResult(jobId,String.valueOf(pointPos));

            String subCode = hisUniTaskItemPoint.getSubCode();
            String pointCode = hisUniTaskItemPoint.getPointCode();
            String requestId = hisUniTaskItemPoint.getRequestId();
            //处理item_file表
            HisUniTaskItemFileEntity fileEntity = hisUniTaskItemFileMapper.getByRequestId(requestId);
            UniPointEntity uniPoint = uniPointMapper.getPointEntityByCode(subCode, pointCode);
            String filePath = robotDogPatrolService.handleImage(base64, fileConfig.getFileSavePath(), subCode, pointCode);
            fileEntity.setFileType(uniPoint.getSaveTypeList());
            fileEntity.setFilePath(filePath);
            fileEntity.setRequestId(requestId);
            hisUniTaskItemFileMapper.updateById(fileEntity);

            //发送智能分析
            AnalyseParamsReq analyseParamsReq = new AnalyseParamsReq();

            analyseParamsReq.setObjectId(pointCode + "_1");
            analyseParamsReq.setImageUrlList(Lists.newArrayList(filePath));
            //typeList
            String pointAnalyseType = uniPoint.getPointAnalyseType();
            if (CustomStringUtils.isNotEmpty(pointAnalyseType)) {
                List<String> typeList = Arrays.asList(pointAnalyseType.split(","));
                analyseParamsReq.setTypeList(typeList);
            }

            List<AnalyseParamsReq> analyseParamsReqList = new ArrayList<>();
            analyseParamsReqList.add(analyseParamsReq);
            AnalyseImageInfo analyseImageInfo = new AnalyseImageInfo();
            analyseImageInfo.setRequestId(requestId);
            analyseImageInfo.setAnalyseParamsReqList(analyseParamsReqList);
//          taskPatrolledId用job_id（含义应该一样）
            analyseImageInfo.setTaskInfo(new TaskInfo(subCode,jobId, "", "", null));
            sendAnalyse(analyseImageInfo);
        }
    }
//  todo 待验证
    @Override
    public void analyseFinish(AnalyseParamsRecReq analyseParamsRecReq) {
        log.info("【收到智能分析结果】 ：{}", JSONObject.toJSONString(analyseParamsRecReq));
        String requestId = analyseParamsRecReq.getRequestId();
        HisUniTaskItemFileEntity hisFile = hisUniTaskItemFileMapper.getByRequestId(requestId);
        HisUniTaskItemPointsEntity hisPoint = hisUniTaskItemPointsMapper.getByRequestId(requestId);
        String taskPatrolledId = hisPoint.getTaskPatrolledId();
        String subCode = hisPoint.getSubCode();
        String pointCode = hisPoint.getPointCode();
        UniPointEntity uniPoint = uniPointMapper.getPointEntityByCode(subCode, pointCode);
        //智能分析大类，详见字典表类型point_analyse_category
        Integer pointAnalyseCategory = uniPoint.getPointAnalyseCategory();

        String code = "2002";
        String pointValUnit = "算法分析失败（没有分析结果）";
        String pointVal = "";
        String resImageUrl = "";
        int valid = 0;
        Integer alarmLevel = null;

        AnalyseParamsRecReq.ResultList.Result result = null;
        List<AnalyseParamsRecReq.ResultList> resultsList = analyseParamsRecReq.getResultsList();
        if (resultsList != null && !resultsList.isEmpty()) {
            AnalyseParamsRecReq.ResultList resultList = resultsList.get(0);
            if (resultList != null && resultList.getResults() != null) {
                List<AnalyseParamsRecReq.ResultList.Result> results = resultList.getResults();
                if (!results.isEmpty()) {
                    result = results.get(0);
                    code = result.getCode();
                    resImageUrl = result.getResImageUrl();
                    pointValUnit = result.getDesc();
                    if ("2002".equals(code)) {
                        if (StringUtils.isNotBlank(pointValUnit)) {
                            pointValUnit = "算法分析失败（" + pointValUnit + "）";
                        } else {
                            pointValUnit = "算法分析失败";
                        }
                    }
                    if ("2001".equals(code)) {
                        pointValUnit = "图像数据错误";
                    }
                }
            }
        }
        if ("2000".equals(code)) {
            //解析分析结果
            HashMap<String, String> valeResultMap = hisExePointAnalyseServiceImpl.updatePointVal(analyseParamsRecReq, pointAnalyseCategory);
            pointVal = valeResultMap.get("point_val");
            pointValUnit = valeResultMap.get("point_val_unit");
            valid = Integer.valueOf(valeResultMap.get("valid"));
            resImageUrl = valeResultMap.get("resImageUrl");
            //判断告警
            alarmLevel = uniPointThresholdService.isAlarmByThreshold(uniPoint, pointVal);
            if (alarmLevel != null) {
                valid = 2;
            }
            //更新图片
            hisFile.setRecgFilePath(resImageUrl);
            hisUniTaskItemFileMapper.updateById(hisFile);
            //异步压缩图片 //2.将原图压缩一份成缩略图存储
            hisExePointAnalyseServiceImpl.updateRecgFilePathPress(resImageUrl, hisFile);
        }
        hisPoint.setPointVal(pointVal);
        hisPoint.setPointValUnit(pointValUnit);
        hisPoint.setValid(valid);
        hisPoint.setIsAlarm(alarmLevel != null ? 1 : 0);
        hisPoint.setIsFinished(1);
        hisPoint.setFinishedTime(new Date());
        hisPoint.setPointUnit("");
        hisUniTaskItemPointsMapper.finishResult(hisPoint);
        updatePointNum(taskPatrolledId);
        if (alarmLevel != null) {
            hisUniTaskItemAlarmService.createAlarm(hisPoint, alarmLevel);
        }
    }

    public void sendAnalyse(AnalyseImageInfo analyseImageInfo) {
        String analyseUrl = vTaskConfig.getAnalyseUrl();
        //回结果的IP和端口
        String analyseResultIp = vTaskConfig.getLocalIp();
        String analyseResultPort = serverPort;
        String requestId = analyseImageInfo.getRequestId();
        String taskPatrolledId = analyseImageInfo.getTaskInfo().getTaskPatrolledId();
        List<AnalyseParamsReq> analyseParamsReqList = analyseImageInfo.getAnalyseParamsReqList();

        //请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("requestId", requestId);
        params.put("requestHostIp", analyseResultIp);
        params.put("requestHostPort", analyseResultPort);
        params.put("objectList", analyseParamsReqList);

        //发送请求
        try {
            log.info("【任务_发送智能分析】 请求url：{}，请求：{}", analyseUrl, JSONObject.toJSONString(params));
            httpUtils.sendPostJson(analyseUrl, JSONObject.toJSONString(params));
        } catch (Exception e) {
            HisUniTaskItemPointsEntity failParam = new HisUniTaskItemPointsEntity();
            failParam.setRequestId(requestId);
            failParam.setIsFinished(1);
            failParam.setFinishedTime(new Date());
            failParam.setValid(0);
            failParam.setPointVal("");
            failParam.setPointUnit("");
            failParam.setIsAlarm(0);
            failParam.setPointValUnit("算法服务通讯异常");
            hisUniTaskItemPointsMapper.finishResult(failParam);
            //更新任务进度
            updatePointNum(taskPatrolledId);
            log.error("【发送智能分析请求】异常！ {}", e.getMessage());
        }
    }

    @Async
    @Override
    public void updatePointNum(String taskPatrolledId) {
        synchronized (this) {
            WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                    .eq(WaylineJobEntity::getJobId, taskPatrolledId));
            Integer warnNum = 0;
            Integer failNum = 0;
            Integer normalNum = 0;
            Integer exceptionNum = 0;
            //告警点位数
            warnNum = hisUniTaskItemPointsService.getStatisticsPointAlarmNum(taskPatrolledId);
            //失败点位数
            failNum = hisUniTaskItemPointsService.getStatisticsPointNum(taskPatrolledId, 0);
            //正常点位数
            normalNum = hisUniTaskItemPointsService.getStatisticsPointNum(taskPatrolledId, 1);
            //异常点位数
            exceptionNum = hisUniTaskItemPointsService.getStatisticsPointNum(taskPatrolledId, 2);

            waylineJobMapper.updatePointNum(warnNum, failNum, normalNum, exceptionNum, waylineJobEntity.getId());
        }
    }

    public static String convertImageToBase64(String imageUrl) throws Exception {
        URL url = new URL(imageUrl);
        try (InputStream in = url.openStream()) {
            byte[] imageBytes = IOUtils.toByteArray(in);
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }
    /**
     * 从文件名中提取"航点"后面的数字
     * @param fileName 包含航点信息的文件名
     * @return 航点编号，如未找到返回-1
     */
    public static int extractWaypointNumber(String fileName) {
        // 查找"航点"字符串的位置
        int waypointIndex = fileName.indexOf("航点");
        if (waypointIndex == -1) {
            return -1;
        }

        // 从"航点"后开始提取数字
        int start = waypointIndex + 2;
        int end = start;

        // 找到数字结束的位置
        while (end < fileName.length() && Character.isDigit(fileName.charAt(end))) {
            end++;
        }

        try {
            return Integer.parseInt(fileName.substring(start, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

}
