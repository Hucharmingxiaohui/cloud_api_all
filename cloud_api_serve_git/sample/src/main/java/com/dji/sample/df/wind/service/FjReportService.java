package com.dji.sample.df.wind.service;


import com.df.framework.vo.Result;
import com.df.server.dto.HisUniTask.ConfirmHisTaskReportParams;
import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;
import com.dji.sample.df.wind.model.entity.AnalysisResponse;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.CompletableFuture;

public interface FjReportService {

    /**
     * 查看任务报告
     *
     * @param params
     * @return
     */
    TaskReportDTO lookReport(HisUniTaskParamsDTO params);

    /**
     * 生成巡视报告
     *

     */
    void genPatrolTaskWordNew(String reportId, String jobId);

    String createNewReport(String jobId);

    void processAndAddDefects(AnalysisResponse response,String jobId);

    void downloadDocxFile(String filePath, HttpServletResponse response);

//     CompletableFuture<Void> processPictureAsync(String jobId);

}
