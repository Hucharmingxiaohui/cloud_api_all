package com.df.server.mapper.his;


import com.df.server.dto.HisUniTask.HisUniTaskPageDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisPointsHandleDTO;
import com.df.server.entity.his.HisUniTaskEntity;
import com.df.server.vo.his.HisUniTaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 视频巡检任务记录Mapper接口
 *
 * @author ruoyi
 * @date 2021-01-08
 */
@Mapper
public interface HisUniTaskMapper {
    /**
     * 查询最新一个正在执行的任务
     *
     * @return
     */
    HisUniTaskVO getRunningTask();

    void insertHisUniTask(HisUniTaskEntity hisUniTaskEntity);

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
     * 查询任务信息
     *
     * @param taskPatrolledId
     * @return
     */
    HisUniTaskEntity getByTaskPatrolledId(String taskPatrolledId);

    /**
     * 更新统计任务进度等信息
     */
    void updateProgressAndPointNum(@Param("warnNum") Integer warnNum,
                                   @Param("failNum") Integer failNum,
                                   @Param("normalNum") Integer normalNum,
                                   @Param("exceptionNum") Integer exceptionNum,
                                   @Param("vTaskProgress") String vTaskProgress,
                                   @Param("runstate") Integer runstate,
                                   @Param("id") Integer id);

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

    List<HisUniTaskVO> getPageList(HisUniTaskPageDTO pageDTO);

    Integer getPageTotal(HisUniTaskPageDTO pageDTO);

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

    HisUniTaskVO getLastTask();
}
