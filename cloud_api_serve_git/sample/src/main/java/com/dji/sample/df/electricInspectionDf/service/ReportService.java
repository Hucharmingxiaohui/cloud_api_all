package com.dji.sample.df.electricInspectionDf.service;

import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;

public interface ReportService {

    TaskReportDTO lookReport(HisUniTaskParamsDTO params);

    void genPatrolTaskWordNew(Integer reportId, Integer imageNeed, String fileType);
}
