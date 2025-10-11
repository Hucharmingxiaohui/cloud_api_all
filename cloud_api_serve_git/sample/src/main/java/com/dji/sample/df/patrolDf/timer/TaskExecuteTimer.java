package com.dji.sample.df.patrolDf.timer;

import com.df.framework.config.VTaskConfig;
import com.df.server.entity.uni.UniTaskPlanEntity;
import com.df.server.service.his.HisUniTaskService;
import com.df.server.service.uni.UniTaskPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class TaskExecuteTimer {

    @Autowired
    private UniTaskPlanService uniTaskPlanService;
    @Autowired
    private HisUniTaskService hisUniTaskService;
    @Autowired
    private VTaskConfig vTaskConfig;

    /**
     * 扫描需要执行的巡视任务
     */
    @Scheduled(fixedDelay = 5 * 1000L)
    public void scanPatrolPlan() {
        if (!vTaskConfig.isEnabled()) {
            return;
        }
        List<UniTaskPlanEntity> list = uniTaskPlanService.getScheduledPlan();
        for (UniTaskPlanEntity uniTaskPlanEntity : list) {
            hisUniTaskService.executePlan(uniTaskPlanEntity, 0);
        }
    }


}
