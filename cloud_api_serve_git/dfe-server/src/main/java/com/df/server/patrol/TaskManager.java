package com.df.server.patrol;

import com.df.framework.config.VTaskConfig;
import com.df.server.entity.his.HisUniTaskEntity;
import com.df.server.entity.uni.UniTaskPlanEntity;
import com.df.server.mapper.his.HisUniTaskMapper;
import com.df.server.patrol.control.RobotControlService;
import com.df.server.patrol.control.sendDto.RobotCommandAPIDTO;
import com.df.server.patrol.control.sendDto.RobotParamsConstant;
import com.df.server.patrol.control.sendDto.RobotSyncPointDTO;
import com.df.server.service.his.HisUniTaskItemPointsService;
import com.df.server.service.his.HisUniTaskService;
import com.df.server.service.uni.UniRobotService;
import com.df.server.service.uni.UniTaskPlanService;
import com.df.server.vo.UniRobot.UniRobotVO;
import com.df.server.vo.his.HisUniTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class TaskManager {

    @Autowired
    private VTaskConfig vTaskConfig;
    @Autowired
    private HisUniTaskItemPointsService hisUniTaskItemPointsService;
    @Autowired
    private UniRobotService uniRobotService;
    @Autowired
    private HisUniTaskService hisUniTaskService;
    @Autowired
    private RobotControlService robotControlService;
    @Autowired
    private HisUniTaskMapper hisUniTaskMapper;
    @Autowired
    private UniTaskPlanService uniTaskPlanService;


    /**
     * 定时执行，按变电站分组，进行任务执行表扫描
     * 按变电站分组的目的是区域主机中直接接入型多个变电站并发执行视频任务
     */
    @Scheduled(fixedRate = 5 * 1000, initialDelay = 15 * 1000)
    public void fixedRateJob() {
        if (!vTaskConfig.isEnabled()) {
            return;
        }
        UniRobotVO dogInfo = uniRobotService.getDogInfo();
        if (dogInfo == null || dogInfo.getOnlineState() == null || 1 != dogInfo.getOnlineState()) {
            return;
        }
        HisUniTaskVO runningTask = hisUniTaskService.getRunningTask();
        if (runningTask != null) {
            log.warn("当前有执行任务， {}", runningTask.getTaskName());
            return;
        }
        //查询待执行任务 run_state = 5 最新一个
        HisUniTaskEntity waitRunTask = hisUniTaskService.scanWaitExecuteTask();
        if (waitRunTask != null) {
            String planNo = waitRunTask.getPlanNo();
            Optional<UniTaskPlanEntity> taskPlanOpt = uniTaskPlanService.lambdaQuery().eq(UniTaskPlanEntity::getPlanNo, planNo).oneOpt();
            if (!taskPlanOpt.isPresent()) {
                log.warn("当前有执行任务，其任务计划已不存在，终止该任务执行， {}", waitRunTask.getTaskName());
                hisUniTaskService.stopTask(waitRunTask.getTaskPatrolledId());
                return;
            } else {
                UniTaskPlanEntity uniTaskPlanEntity = taskPlanOpt.get();
                Integer isenable = uniTaskPlanEntity.getIsenable();
                if (isenable != null && isenable == 1) {
                    log.warn("当前任务其计划停用，终止该任务执行， {}", waitRunTask.getTaskName());
                    hisUniTaskService.stopTask(waitRunTask.getTaskPatrolledId());
                    return;
                }
            }
            String waiteTaskName = waitRunTask.getTaskName();
            String waiteTaskPatrolledId = waitRunTask.getTaskPatrolledId();
            if (waitRunTask.getAllPointCnt() == 0) {
                log.warn("任务点位数量为0，等待点位入库... , {}", waiteTaskName);
                return;
            }
            List<String> waiteRobotCodeList = hisUniTaskItemPointsService.listTaskPatroldeviceCode(waiteTaskPatrolledId);
            if (waiteRobotCodeList.isEmpty()) {
                log.warn("任务全部点位均没有关联机器人，终止任务执行 , {}", waiteTaskName);
                hisUniTaskMapper.stopTask(waiteTaskPatrolledId);
                return;
            }
            List<String> distinct = waiteRobotCodeList.stream().distinct().collect(Collectors.toList());
            //检查当前机器人是否在空闲中
            int unRunningRobotNum = uniRobotService.countUnRunningRobot(distinct);
            if (unRunningRobotNum != distinct.size()) {
                log.warn("任务点位关联机器人未空闲中，任务等待执行 {}，机器人：{}", waiteTaskName, distinct);
                return;
            }
            List<Integer> waiteRobotPosList = hisUniTaskItemPointsService.listTaskRobotPosList(waiteTaskPatrolledId);
            //http://192.168.3.10:5001/api/save_point
            try {
                robotControlService.sendTaskPointSync(new RobotSyncPointDTO(waiteRobotPosList));
            } catch (Exception e) {
                log.error("", e);
            }
            //http://192.168.3.10:5001/api/command
            try {
                robotControlService.sendCustomCommand(new RobotCommandAPIDTO(RobotParamsConstant.TASK_RELOAD));
            } catch (Exception e) {
                log.error("", e);
            }
            // http://192.168.3.10:5003/api/command
            try {
                robotControlService.sendCustomReloadCommand(new RobotCommandAPIDTO(RobotParamsConstant.TASK_RELOAD));
            } catch (Exception e) {
                log.error("", e);
            }
            //http://192.168.3.10:5004/api/exit_recharge
            try {
                robotControlService.sendRecharge(new RobotCommandAPIDTO(RobotParamsConstant.TASK_START));
            } catch (Exception e) {
                log.error("", e);
            }
            //http://192.168.3.10:5001/api/command
            try {
                robotControlService.sendCustomCommand(new RobotCommandAPIDTO(RobotParamsConstant.TASK_START));
            } catch (Exception e) {
                log.error("", e);
            }
            //更新任务执行中
            hisUniTaskService.startTask(waiteTaskPatrolledId);
            log.info("任务开始执行，任务：{}", waiteTaskName);
        }
    }
}
