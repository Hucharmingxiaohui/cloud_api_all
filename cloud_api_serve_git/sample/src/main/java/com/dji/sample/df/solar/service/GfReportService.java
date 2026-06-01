package com.dji.sample.df.solar.service;

import com.dji.sample.df.solar.model.entity.GfPositionRequest;
import com.dji.sample.df.solar.model.entity.GfPositionResponse;
import com.dji.sample.df.wind.model.entity.AnalysisRequest;
import com.dji.sample.df.wind.model.entity.AnalysisResponse;
import com.dji.sample.df.wind.model.entity.DefectEntity;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface GfReportService {

//  发送光伏分析服务
    AnalysisResponse sendGfAnalysisRequest(AnalysisRequest request);
//  发送光伏定位接口服务
    GfPositionResponse sendGfPositionRequest(GfPositionRequest request);
//  处理光伏分析结果并生成缺陷数据
    void processAndAddDefects(AnalysisResponse response,String jobId) throws JsonProcessingException;
//  处理光伏定位接口结果并更新缺陷数据
    void processAndUptDefects(GfPositionResponse response,String jobId);

    List<GfPositionRequest.Image> producePositionParam (List<DefectEntity> defectEntities);

}
