package com.dji.sample.df.supControlDf.service;

import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointResult;

import java.sql.SQLException;

public interface TaskcontrolService  {
    //上级创建计划并下发
    String SuperiorCreatePlanJob(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) throws SQLException;
    //结果模拟调用
    PointResult getResultByJobId(String job_id) throws Exception;
    //定时查询结果上报给上级
    String timedQueryByJobId(String job_id,String workspace_id,String wayline_id,String sub_code);
    //上级任务2.暂停、3.继续、4.取消
    String  superiorTaskHandling(String job_id,String cmd_type,String  command);
}
