package com.df.server.service.his;

import com.baomidou.mybatisplus.extension.service.IService;
import com.df.framework.vo.PageVO;
import com.df.server.dto.HisUniTask.HisUniTaskPageDTO;
import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisPointsHandleDTO;
import com.df.server.entity.his.HisUniTaskEntity;
import com.df.server.entity.uni.UniTaskPlanEntity;
import com.df.server.vo.his.HisUniTaskVO;

import java.util.List;

/**
 * @author 平善闯
 * @date 2025-04-22 12:17
 */
public interface HisUniTaskService {
    PageVO<HisUniTaskVO> getPageList(HisUniTaskPageDTO hisUniTaskDTO);

    /**
     * 任务执行
     *
     * @param uniTaskPlanEntity
     * @param isImmediately
     */
    void executePlan(UniTaskPlanEntity uniTaskPlanEntity, int isImmediately);

    /**
     * 查询待执行任务 run_state = 5 最新一个
     *
     * @return
     */
    HisUniTaskEntity scanWaitExecuteTask();

    /**
     * 更新任务执行中
     *
     * @param waiteTaskPatrolledId
     */
    void startTask(String waiteTaskPatrolledId);

    /**
     * 统计任务进度等信息
     *
     * @param taskPatrolledId
     */
    void updateProgressAndPointNum(String taskPatrolledId);

    /**
     * 更新任务暂停
     *
     * @param taskPatrolledId
     */
    void pauseTask(String taskPatrolledId);

    /**
     * 更新任务执行中
     *
     * @param taskPatrolledId
     */
    void recoverTask(String taskPatrolledId);

    /**
     * 更新状态终止任务
     *
     * @param taskPatrolledId
     */
    void stopTask(String taskPatrolledId);

    /**
     * 查询任务详情
     *
     * @param taskPatrolledId
     * @return
     */
    HisUniTaskVO getInfo(String taskPatrolledId);

    /**
     * 任务确认完毕
     *
     * @param params
     */
    void confirmTask(HisPointsHandleDTO params);

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
     * @param reportId
     * @param imageNeed
     * @param fileType
     */
    void genPatrolTaskWordNew(Integer reportId, Integer imageNeed, String fileType);

    /**
     * 查询最新一个正在执行的任务
     *
     * @return
     */
    HisUniTaskVO getRunningTask();

    HisUniTaskVO getShowTask();

}
