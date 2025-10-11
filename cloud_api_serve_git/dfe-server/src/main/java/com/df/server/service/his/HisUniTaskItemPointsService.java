package com.df.server.service.his;

import com.df.framework.vo.PageVO;
import com.df.server.dto.HisUniTaskItemPoints.HisPointsHandleDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisUniTaskItemPointsPageDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisUniTaskItemPointsParamsDTO;
import com.df.server.entity.his.HisUniTaskItemPointsEntity;
import com.df.server.entity.uni.UniTaskPlanEntity;
import com.df.server.vo.his.HisUniTaskItemPointsVO;

import java.util.List;

/**
 * @date 2025-04-22 15:37
 */
public interface HisUniTaskItemPointsService {

    PageVO<HisUniTaskItemPointsVO> getHisPoints(HisUniTaskItemPointsPageDTO hisUniPointsDTO);

    /**
     * 查询单个点位最新的巡视结果
     *
     * @param subCode
     * @param pointCode
     * @return
     */
    HisUniTaskItemPointsEntity getLastItems(String subCode, String pointCode);

    /**
     * 新增
     *
     * @param uniTaskPlanEntity
     * @param taskPatrolledId
     */
    void createHisPoint(UniTaskPlanEntity uniTaskPlanEntity, String taskPatrolledId);

    /**
     * 查询任务的巡视设备编码
     *
     * @param waiteTaskPatrolledId
     * @return
     */
    List<String> listTaskPatroldeviceCode(String waiteTaskPatrolledId);

    /**
     * 统计任务告警数量
     *
     * @param taskPatrolledId
     * @return
     */
    Integer getStatisticsPointAlarmNum(String taskPatrolledId);

    /**
     * 统计任务各个结果数量 按照valid 统计
     *
     * @param taskPatrolledId
     * @param valid
     * @return
     */
    Integer getStatisticsPointNum(String taskPatrolledId, int valid);

    /**
     * 计算任务进度百分比
     *
     * @param taskPatrolledId
     * @param pointNum        总点位数量
     * @return
     */
    String getStatisticsPointFinishNum(String taskPatrolledId, Integer pointNum);

    /**
     * 查询点位详情
     *
     * @param paramsDTO "taskPatrolledId", "pointCode"
     * @return
     */
    HisUniTaskItemPointsVO getHisPointDetail(HisUniTaskItemPointsParamsDTO paramsDTO);

    /**
     * 人工修正
     *
     * @param params "taskPatrolledId", "pointCode","setVal"
     */
    void abnormalHandle(HisPointsHandleDTO params);

    /**
     * 结果确认
     *
     * @param params 结果确认 一键确认只传taskPatrolledId，单个结果确认 加pointCode
     */
    void confirm(HisPointsHandleDTO params);

    /**
     * 告警确认"taskPatrolledId", "pointCode", "confirmResult"， "alarmConfirmComment";
     *
     * @param params
     */
    void confirmAlarm(HisPointsHandleDTO params);

    int countFinishNum(String taskPatrolledId);

    /**
     * 查询机器狗的预置位
     *
     * @param waiteTaskPatrolledId
     * @return
     */
    List<Integer> listTaskRobotPosList(String waiteTaskPatrolledId);
}
