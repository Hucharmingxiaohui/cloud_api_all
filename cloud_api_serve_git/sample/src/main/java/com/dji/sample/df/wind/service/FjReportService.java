package com.dji.sample.df.wind.service;


import com.alibaba.fastjson.JSONArray;
import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.wind.model.entity.AnalysisRequest;
import com.dji.sample.df.wind.model.entity.AnalysisResponse;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface FjReportService {

    /**
     * 查看任务报告
     *
     * @param params
     * @return
     */
    TaskReportDTO lookReport(HisUniTaskParamsDTO params);

    /**
     * 生成巡视报告（风机）
     *

     */
    void genPatrolTaskWordNew(String reportId, String jobId);

    void genNormalPatrolTaskWordNew(String reportId, String jobId);

    String createNewReport(String jobId);

    void processAndAddDefects(AnalysisResponse response,String jobId);

    void downloadDocxFile(String filePath, HttpServletResponse response);

    List<String> generateFileNames(List<MediaFileEntity> mediaFileEntities, JSONArray points);

    AnalysisResponse sendAnalysisRequest(AnalysisRequest request);

}
