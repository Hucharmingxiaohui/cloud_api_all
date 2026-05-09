package com.dji.sample.df.solar.service;

import com.dji.sample.df.wind.model.entity.AnalysisRequest;
import com.dji.sample.df.wind.model.entity.AnalysisResponse;

public interface GfReportService {

//  发送光伏分析服务
    AnalysisResponse sendGfAnalysisRequest(AnalysisRequest request);
//  处理光伏分析结果并生成缺陷数据
    void processAndAddDefects(AnalysisResponse response,String jobId);
}
