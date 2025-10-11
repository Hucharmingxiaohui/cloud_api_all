package com.df.server.service.his;


import com.df.server.dto.HisUniTask.ConfirmHisTaskReportParams;
import com.df.server.entity.his.HisUniTaskReportEntity;

/**
 * Created by lianyc on 2025-05-23
 */
public interface HisUniTaskReportService {

    Integer createNewReport(ConfirmHisTaskReportParams params);

    HisUniTaskReportEntity getByTaskId(String taskPatrolledId);
}

