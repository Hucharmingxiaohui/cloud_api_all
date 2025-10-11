package com.df.server.mapper.his;


import com.df.server.dto.HisUniTaskItemPoints.HisPointsHandleDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisUniTaskItemFileReport;
import com.df.server.dto.HisUniTaskItemPoints.HisUniTaskItemPointsPageDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisUniTaskItemPointsParamsDTO;
import com.df.server.entity.his.HisUniTaskItemPointsEntity;
import com.df.server.vo.UniPoint.UniPointVO;
import com.df.server.vo.his.HisUniTaskItemPointsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface HisUniTaskItemPointsMapper {

    /**
     * 创建任务时，从任务计划获取点位信息
     *
     * @param planNo
     * @return
     */
    List<UniPointVO> listPointsToHisTask(@Param("planNo") String planNo);

    /**
     * 查询任务的巡视设备编码
     *
     * @param waiteTaskPatrolledId
     * @return
     */
    List<String> listTaskPatroldeviceCode(String waiteTaskPatrolledId);

    HisUniTaskItemPointsEntity getHisPointByRobotResult(@Param("taskPatrolledId") String taskPatrolledId,
                                                        @Param("robotPos") String robotPos);

    HisUniTaskItemPointsEntity getHisPointByUavResult(@Param("jobId") String jobId,
                                                        @Param("pointPos") String pointPos);

    void updateRunTime(@Param("taskPatrolledId") String taskPatrolledId,
                       @Param("robotPos") String robotPos);


    void finishResult(HisUniTaskItemPointsEntity failParam);

    HisUniTaskItemPointsEntity getByRequestId(String requestId);

    /**
     * 统计任务告警数量
     *
     * @param taskPatrolledId
     * @return
     */
    Integer getStatisticsPointAlarmNum(String taskPatrolledId);

    /**
     * 统计任务各个结果数量
     *
     * @param taskPatrolledId
     * @param valid
     * @return
     */
    Integer getStatisticsPointNum(@Param("taskPatrolledId") String taskPatrolledId, @Param("valid") int valid);

    /**
     * 已经完成的点位数量
     *
     * @param taskPatrolledId
     * @return
     */
    Integer countFinishNum(String taskPatrolledId);

    List<HisUniTaskItemPointsVO> getPageList(HisUniTaskItemPointsPageDTO hisUniPointsDTO);

    Integer getPageTotal(HisUniTaskItemPointsPageDTO hisUniPointsDTO);

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
     */
    void confirm(@Param("taskPatrolledId") String taskPatrolledId,
                 @Param("pointCode") String pointCode);

    /**
     * 统计任务未确认结果的数量
     *
     * @param taskPatrolledId
     * @return
     */
    int getUnConfirmNum(String taskPatrolledId);

    /**
     * 查看任务报告 封装结果和图片
     *
     * @param taskPatrolledId
     * @return
     */
    List<HisUniTaskItemFileReport> selectPointAndFile(String taskPatrolledId);

    /**
     * 查询单个点位最新的巡视结果
     *
     * @param subCode
     * @param pointCode
     * @return
     */
    HisUniTaskItemPointsEntity getLastItems(@Param("subCode") String subCode,
                                            @Param("pointCode") String pointCode);

    /**
     * 查询机器狗的预置位
     *
     * @param waiteTaskPatrolledId
     * @return
     */
    List<Integer> listTaskRobotPosList(@Param("waiteTaskPatrolledId") String waiteTaskPatrolledId);
}
