package com.dji.sample.df.electricInspectionDf.service;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;

import java.sql.SQLException;
import java.util.Map;


public interface PubWaylineJobPlanDfService {
    //创建计划
    boolean createWaylineJObPlan(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity);
    //  根据点位获取航线及航点信息
    Map<Integer,String> getWaylineByPoint(Integer deviceLevel, String deviceList);
//    根据航线预置位获取航线id(逗号隔开）
    Map<String, String> getWaylineIdByPos(Map<Integer, String> waylineByPoint);
    //按场站查询计划
    PaginationData<PubWaylineJobPlanDfEntity> getPlanBySubCode(String sub_code, long page, long pageSize);
    //执行计划生成任务&&更新计划
    HttpResultResponse expressPlan(CustomClaim customClaim, PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) throws SQLException;
    //取消任务
    HttpResultResponse cancelPlan(CustomClaim customClaim, WaylineJobEntity waylineJobEntity) throws SQLException;
    //按id删除计划
    boolean deletePlanById(Integer id);
    //按job_id删除任务
    boolean deleteJobByBobId(String job_id);
}
