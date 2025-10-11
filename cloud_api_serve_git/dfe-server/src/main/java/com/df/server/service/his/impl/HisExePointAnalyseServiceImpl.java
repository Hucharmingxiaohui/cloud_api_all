package com.df.server.service.his.impl;

import com.alibaba.fastjson.JSONObject;
import com.df.framework.config.FileConfig;
import com.df.framework.config.VTaskConfig;
import com.df.framework.thread.CustomExecutorFactory;
import com.df.framework.utils.HttpUtils;
import com.df.framework.utils.file.ImageHelper;
import com.df.server.dto.robotDog.*;
import com.df.server.entity.his.HisUniTaskItemFileEntity;
import com.df.server.entity.his.HisUniTaskItemPointsEntity;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.mapper.his.HisUniTaskItemFileMapper;
import com.df.server.mapper.his.HisUniTaskItemPointsMapper;
import com.df.server.mapper.uni.UniPointMapper;
import com.df.server.service.his.HisExePointAnalyseService;
import com.df.server.service.his.HisUniTaskItemAlarmService;
import com.df.server.service.his.HisUniTaskService;
import com.df.server.service.uni.UniPointThresholdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@Slf4j
public class HisExePointAnalyseServiceImpl implements HisExePointAnalyseService {

    @Autowired
    private HttpUtils httpUtils;
    @Autowired
    private VTaskConfig vTaskConfig;
    @Autowired
    private HisUniTaskItemFileMapper hisUniTaskItemFileMapper;
    @Autowired
    private UniPointMapper uniPointMapper;
    @Autowired
    private HisUniTaskItemPointsMapper hisUniTaskItemPointsMapper;
    @Value("${server.port}")
    private String serverPort;
    @Autowired
    private UniPointThresholdService uniPointThresholdService;
    @Autowired
    private FileConfig fileConfig;
    @Autowired
    private HisUniTaskService hisUniTaskService;
    @Autowired
    private HisUniTaskItemAlarmService hisUniTaskItemAlarmService;

    /****************** ExeThread执行 *******************/

    /**
     * 发送智能分析请求
     *
     * @param analyseImageInfo
     */
    @Override
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
            hisUniTaskService.updateProgressAndPointNum(taskPatrolledId);
            log.error("【发送智能分析请求】异常！ {}", e.getMessage());
        }
    }

    /**
     * 点位分析完成处理
     *
     * @param analyseParamsRecReq
     */
    @Override
    /**
     * 机器人智能分析请求结果处理
     *
     * @param analyseParamsRecReq
     */
    public void analyseFinish(AnalyseParamsRecReq analyseParamsRecReq) {
        log.info("【收到智能分析结果】 ：{}", JSONObject.toJSONString(analyseParamsRecReq));
        String requestId = analyseParamsRecReq.getRequestId();
        //查询信息
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
            HashMap<String, String> valeResultMap = this.updatePointVal(analyseParamsRecReq, pointAnalyseCategory);
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
            updateRecgFilePathPress(resImageUrl, hisFile);
        }
        hisPoint.setPointVal(pointVal);
        hisPoint.setPointValUnit(pointValUnit);
        hisPoint.setValid(valid);
        hisPoint.setIsAlarm(alarmLevel != null ? 1 : 0);
        hisPoint.setIsFinished(1);
        hisPoint.setFinishedTime(new Date());
        hisPoint.setPointUnit("");
        hisUniTaskItemPointsMapper.finishResult(hisPoint);
        //更新任务进度
        hisUniTaskService.updateProgressAndPointNum(taskPatrolledId);

        if (alarmLevel != null) {
            hisUniTaskItemAlarmService.createAlarm(hisPoint, alarmLevel);
        }
    }

    public void updateRecgFilePathPress(String resImageUrl, HisUniTaskItemFileEntity hisUniTaskItemFile) {
        CustomExecutorFactory.ScaleImagePool.execute(() -> {
            try {
                String recgFilePathPress = uploadRecgFilePathPress(resImageUrl);
                hisUniTaskItemFile.setRecgFilePathPress(recgFilePathPress);
                hisUniTaskItemFileMapper.updateById(hisUniTaskItemFile);
            } catch (Exception e) {
                log.error("根据智能分析图生成缩略图异常，但不影响任务执行，异常信息：{}", e.getMessage());
            }
        });
    }

    public String uploadRecgFilePathPress(String resImageUrl) {
        String recgFilePathPress = null;
        String suffix = "jpg";
        if (StringUtils.isNotBlank(resImageUrl)) {
            String[] split = resImageUrl.split("\\.");
            String filePath = split[0];
            suffix = split[1];
            recgFilePathPress = String.format("%s%s%s%s", filePath, "_compress", ".", suffix);
        }
        double scale = 0.4;

        Boolean result = ImageHelper.scaleImage(
                String.format("%s/%s", fileConfig.getFileSavePath(), resImageUrl),
                String.format("%s/%s", fileConfig.getFileSavePath(), recgFilePathPress),
                scale, suffix);
        if (!result) {
            return null;
        }
        return recgFilePathPress;
    }


    public HashMap<String, String> updatePointVal(AnalyseParamsRecReq analyseParamsRecReq, Integer pointAnalyseCategory) {
        HashMap<String, String> valeResult = new HashMap<>();

        //从返回值中找到识别值
        AnalyseParamsRecReq.ResultList resultList = analyseParamsRecReq.getResultsList().get(0);
        List<AnalyseParamsRecReq.ResultList.Result> results = resultList.getResults();
        //1.默认认为结果只返回一个值
        AnalyseParamsRecReq.ResultList.Result result = results.get(0);
        String valid = "1"; //识别正常，默认给1
        String point_val = result.getValue();
        String point_val_unit = result.getDesc();
        String resImageUrl = result.getResImageUrl();
        String conf = result.getConf();

        //2.如果结果返回多个值，且点位是缺陷类识别，需要特殊处理
        if (pointAnalyseCategory == 2) {
            //默认正常
            valid = "1";
            point_val_unit = "正常";
            //逐个判断，只要发现一个value=1就是异常,还需要将异常描述用逗号分割拼接
            StringBuilder desBuffer = new StringBuilder();
            for (AnalyseParamsRecReq.ResultList.Result resultTemp : results) {
                String value = resultTemp.getValue();
                String desc = resultTemp.getDesc();
                //List<String> listByDictType = sysDictDataMapper.getListByDictType("qxsb_type");
                if ("1".equals(value)) {
                    valid = "2";//判别异常
                    point_val = "1"; //只要发现一个value=1，识别结果就是1
                    desBuffer.append(desc).append(",");
                }
            }
            if (desBuffer.length() > 0) {
                point_val_unit = desBuffer.toString();
            }
        }
        //3.如果点位是判别类型（只返回一个结果）,value 0是正常 1是异常
        if (pointAnalyseCategory == 3) {
            if ("0".equals(result.getValue())) {
                valid = "1";//正常
            } else {
                valid = "2";//判别异常
            }
        }
        if ("0 -".equals(point_val_unit) || "-".equals(point_val_unit)) {
            valid = "2";
            point_val_unit = "算法识别失败";
        }


        //4.返回结果，如果point_val_unit以逗号结尾就去掉
        if (point_val_unit.endsWith(",")) {
            point_val_unit = point_val_unit.substring(0, point_val_unit.length() - 1);
        }
        valeResult.put("valid", valid);
        valeResult.put("point_val", point_val);
        valeResult.put("point_val_unit", point_val_unit);
        valeResult.put("resImageUrl", resImageUrl);
        valeResult.put("conf", conf);
        return valeResult;
    }
}
