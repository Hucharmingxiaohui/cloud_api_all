package com.dji.sample.df.uavCommonHandleDf.service;


import com.alibaba.fastjson.JSONArray;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.uavCommonHandleDf.model.entity.AnalysisRequest;
import com.dji.sample.df.uavCommonHandleDf.model.entity.AnalysisResponse;

import java.util.List;

public interface uavReportService {
//  生成光伏报告
    void genGfPatrolTaskWordNew(String reportId, String jobId);
//  生成风机报告
    void genFjPatrolTaskWordNew(String reportId, String jobId);
//  生成航点航线任务报告
    void genNormalPatrolTaskWordNew(String reportId, String jobId);
//  生成报告数据库记录
    String createNewReport(String jobId);
//  处理风机分析结果并生成缺陷数据
    void processAndAddDefects(AnalysisResponse response,String jobId);
//  生成风机图片名称
    List<String> generateFjFileNames(List<MediaFileEntity> mediaFileEntities, JSONArray points);
//  发送风机分析服务
    AnalysisResponse sendFjAnalysisRequest(AnalysisRequest request);

}
