package com.dji.sample.df.electricInspectionDf.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import com.dji.sample.center.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.ResultService;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.mediaDf.service.IFileServiceDf;
import com.dji.sample.df.wind.config.FjFileConfig;
import com.dji.sample.df.wind.dao.RecgFileEntityMapper;
import com.dji.sample.df.wind.dao.RecgPointsEntityMapper;
import com.dji.sample.df.wind.model.entity.RecgFileEntity;
import com.dji.sample.df.wind.model.entity.RecgPointsEntity;
import com.dji.sample.media.dao.IFileMapper;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.service.impl.WaylineJobServiceImpl;
import com.google.common.collect.Lists;
import com.sun.xml.bind.v2.TODO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private FjFileConfig fjFileConfig;
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
    @Autowired
    UniPointMapper2 uniPointMapper2;
    @Value("${server.port}")
    private String serverPort;
    @Autowired
    RecgPointsEntityMapper recgPointsEntityMapper;
    @Autowired
    RecgFileEntityMapper recgFileEntityMapper;
    @Autowired
    WaylineJobServiceImpl waylineJobServiceimpl;


    //      只针对一个航点对应一个点位（因为点位导入的时候就是点位与一个航点预置位号绑定）
    @Override
    public void handleUavResult(Map<String,String> map,String workspaceId,String jobId) throws Exception {

        List<MediaFileDTO> mediaFileDTOList = iFileServiceDf.getFilesByJobId(jobId);
//       每次发一个照片
        for (MediaFileDTO mediaFileDTO : mediaFileDTOList) {
            String fileName = mediaFileDTO.getFileName();

            int pointPos = extractWaypointNumber(fileName);
            String picType = extractTOrV(fileName);
            Integer picType1 = 0;
            if(picType.equals("V")){
                picType1 = 0;
            }else if(picType.equals("T")){
                picType1 = 1;
            }
            WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                    .eq(WaylineJobEntity::getJobId, jobId));
            UniPoint uniPoint = uniPointMapper2.selectOne(new LambdaQueryWrapper<UniPoint>()
                    .eq(UniPoint::getWaylineId, waylineJobEntity.getFileId())
                    .eq(UniPoint::getWaylinePointPos, pointPos)
                    .eq(UniPoint::getPicType,picType1));
            String subCode = uniPoint.getSubCode();
            String pointCode = uniPoint.getPointCode();
            String pointName = uniPoint.getPointName();
            String requestId = UUID.randomUUID().toString();
//          仿照巡视新建任务点位表和点位文件表，后续优化
            RecgPointsEntity recgPointsEntity=new RecgPointsEntity();
            recgPointsEntity.setRequestId(requestId);
            recgPointsEntity.setSubCode(subCode);
            recgPointsEntity.setPointCode(pointCode);
            recgPointsEntity.setPointName(pointName);
            recgPointsEntity.setTaskPatrolledId(jobId);
            recgPointsEntity.setPresetNo(pointPos);
            recgPointsEntity.setPicType(picType1);
            UpdateWrapper<RecgPointsEntity> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("task_patrolled_id", recgPointsEntity.getTaskPatrolledId());
            updateWrapper.eq("preset_no", recgPointsEntity.getPresetNo());
            updateWrapper.eq("pic_type", recgPointsEntity.getPicType());
            RecgPointsEntity recgPointsEntity1 = recgPointsEntityMapper.selectOne(updateWrapper);
            if(recgPointsEntity1==null){
                recgPointsEntityMapper.insert(recgPointsEntity);
            }else {
                recgPointsEntityMapper.update(recgPointsEntity, updateWrapper);
            }

            RecgFileEntity recgFileEntity=new RecgFileEntity();
            recgFileEntity.setRequestId(requestId);
            recgFileEntity.setPointCode(pointCode);
            recgFileEntity.setTaskPatrolledId(jobId);
            String filePath = map.get(fileName);
            recgFileEntity.setFilePath(fjFileConfig.getRecfilePath()+filePath);
            recgFileEntity.setPresetNo(pointPos);
            recgFileEntity.setPicType(picType1);

            UpdateWrapper<RecgFileEntity> updateWrapper1 = new UpdateWrapper<>();
//            updateWrapper.eq("point_code", recgFileEntity.getPointCode());
            updateWrapper1.eq("task_patrolled_id", recgFileEntity.getTaskPatrolledId());
            updateWrapper1.eq("preset_no", recgFileEntity.getPresetNo());
            updateWrapper1.eq("pic_type", recgFileEntity.getPicType());
            RecgFileEntity recgFileEntity1 = recgFileEntityMapper.selectOne(updateWrapper1);
            if(recgFileEntity1==null){
                recgFileEntityMapper.insert(recgFileEntity);
            }else {
                recgFileEntityMapper.update(recgFileEntity, updateWrapper1);
            }

            //发送智能分析
            AnalyseParamsReq analyseParamsReq = new AnalyseParamsReq();
            String regId=uniPoint.getPointCode()+picType;
//          唯一编码是regId,ObjectId是点位编码+2,1是视频2是机器人
            analyseParamsReq.setObjectId(regId + "_2");
            analyseParamsReq.setImagePathList(Lists.newArrayList(filePath));
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
//          taskPatrolledId用job_id（含义应该一样）,任务点位数的必须的吗
            analyseImageInfo.setTaskInfo(new TaskInfo(subCode,jobId, waylineJobEntity.getName(), waylineJobEntity.getPlanId(), null));

            sendAnalyse(analyseImageInfo);
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


//  todo 待验证
    @Override
    public void analyseFinish(AnalyseParamsRecReq analyseParamsRecReq) {

//      每次返回一个照片,全部分析完是分析完成,在job表加个分析张数，全部完成才分析完成
//        TODO

        log.info("【收到智能分析结果】 ：{}", JSONObject.toJSONString(analyseParamsRecReq));
        String requestId = analyseParamsRecReq.getRequestId();

        RecgFileEntity recgFileEntity = recgFileEntityMapper.selectOne(new LambdaQueryWrapper<RecgFileEntity>().eq(RecgFileEntity::getRequestId, requestId));
        RecgPointsEntity recgPointsEntity = recgPointsEntityMapper.selectOne(new LambdaQueryWrapper<RecgPointsEntity>().eq(RecgPointsEntity::getRequestId, requestId));
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>().eq(WaylineJobEntity::getJobId, recgPointsEntity.getTaskPatrolledId()));
        Integer analyzedNum = waylineJobEntity.getAnalyzedNum();
        if(analyzedNum==null){
            waylineJobEntity.setAnalyzedNum(1);
            waylineJobMapper.updateById(waylineJobEntity);
        }else {
            waylineJobEntity.setAnalyzedNum(analyzedNum + 1);
            waylineJobMapper.updateById(waylineJobEntity);
        }
        Integer analyzedNum1 = waylineJobEntity.getAnalyzedNum();
//        WaylineJobDTO waylineJobDTO = waylineJobServiceimpl.entity2Dto(waylineJobEntity);
//        Integer mediaCount = waylineJobDTO.getMediaCount();
//        uniPointMapper2
        Map map=new HashMap();
        map.put("waylineId",waylineJobEntity.getFileId());
        Integer mediaCount = uniPointMapper2.selectListCount(map);
        if(mediaCount == analyzedNum1){
            waylineJobEntity.setIsAnalyzed(1);
            waylineJobMapper.updateById(waylineJobEntity);
            log.info("智能分析结束----------");
        }else {
            log.info("仍在智能分析中，分析照片数----"+analyzedNum1);
        }
        String subCode = recgPointsEntity.getSubCode();
        String pointCode = recgPointsEntity.getPointCode();

        UniPoint uniPoint = uniPointMapper2.selectOne(new LambdaQueryWrapper<UniPoint>()
                .eq(UniPoint::getSubCode, subCode)
                .eq(UniPoint::getPointCode, pointCode));
        //智能分析大类，详见字典表类型point_analyse_category
        Integer pointAnalyseCategory = uniPoint.getPointAnalyseCategory();

        String code = "2002";
        String pointValUnit = "算法分析失败（没有分析结果）";
        String pointVal = "";
        String resImageUrl = "";
        int valid = 0;
        Integer alarmLevel = null;

        AnalyseParamsRecReq.ResultList.Result result = null;
        List<AnalyseParamsRecReq.ResultList> resultsList = analyseParamsRecReq.getResultList();
        if (resultsList != null && !resultsList.isEmpty()) {
            AnalyseParamsRecReq.ResultList resultList = resultsList.get(0);
            if (resultList != null && resultList.getResults() != null) {
                List<AnalyseParamsRecReq.ResultList.Result> results = resultList.getResults();
                if (!results.isEmpty()) {
                    result = results.get(0);
                    code = result.getCode();
                    resImageUrl = result.getResImagePath();
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
            //判断告警（暂时不弄）
//            alarmLevel = uniPointThresholdService.isAlarmByThreshold(uniPoint, pointVal);
//            if (alarmLevel != null) {
//                valid = 2;
//            }
            //更新图片
//            hisFile.setRecgFilePath(resImageUrl);
//            hisUniTaskItemFileMapper.updateById(hisFile);
//            //异步压缩图片 //2.将原图压缩一份成缩略图存储
//            hisExePointAnalyseServiceImpl.updateRecgFilePathPress(resImageUrl, hisFile);

            recgFileEntity.setRecgFilePath(resImageUrl);
            UpdateWrapper<RecgFileEntity> updateWrapper = new UpdateWrapper<>();
//            updateWrapper.eq("point_code", recgFileEntity.getPointCode());
            updateWrapper.eq("task_patrolled_id", recgFileEntity.getTaskPatrolledId());
            updateWrapper.eq("preset_no", recgFileEntity.getPresetNo());
            updateWrapper.eq("pic_type", recgFileEntity.getPicType());
            recgFileEntityMapper.update(recgFileEntity, updateWrapper);
//          缩略图先不弄
//            hisExePointAnalyseServiceImpl.updateRecgFilePathPress(resImageUrl, hisFile);

        }

        recgPointsEntity.setPointVal(pointVal);
        recgPointsEntity.setPointValUnit(pointValUnit);
        recgPointsEntity.setValid(valid);
        recgPointsEntity.setIsAlarm(alarmLevel != null ? 1 : 0);
        recgPointsEntity.setIsFinished(1);
        recgPointsEntity.setFinishedTime(new Date());
        recgPointsEntity.setPointUnit("");

        UpdateWrapper<RecgPointsEntity> updateWrapper = new UpdateWrapper<>();
//        updateWrapper.eq("point_code", recgPointsEntity.getPointCode());
        updateWrapper.eq("task_patrolled_id", recgPointsEntity.getTaskPatrolledId());
        updateWrapper.eq("preset_no", recgPointsEntity.getPresetNo());
        updateWrapper.eq("pic_type", recgPointsEntity.getPicType());
        recgPointsEntityMapper.update(recgPointsEntity, updateWrapper);
//      先不更新点位数
//        updatePointNum(taskPatrolledId);

//        if (alarmLevel != null) {
//            hisUniTaskItemAlarmService.createAlarm(hisPoint, alarmLevel);
//        }
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
//            HisUniTaskItemPointsEntity failParam = new HisUniTaskItemPointsEntity();
//            failParam.setRequestId(requestId);
//            failParam.setIsFinished(1);
//            failParam.setFinishedTime(new Date());
//            failParam.setValid(0);
//            failParam.setPointVal("");
//            failParam.setPointUnit("");
//            failParam.setIsAlarm(0);
//            failParam.setPointValUnit("算法服务通讯异常");
//            hisUniTaskItemPointsMapper.finishResult(failParam);
//            //更新任务进度
//            updatePointNum(taskPatrolledId);
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


}
