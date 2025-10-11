package com.dji.sample.center.v2022.timer;

import com.dji.sample.center.dao.ICenterToUavPlanMapper;
import com.dji.sample.center.model.entity.CenterToUavPlanEntity;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.TaskUtils;
import com.dji.sample.center.v2022.command.upload.PatrolStatusItem;
import com.dji.sample.center.v2022.handler.CenterMsgPushHandler;
import com.dji.sample.center.v2022.tool.BaseItem;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.manageDf.dao.IUniTaskPlanMapper;
import com.dji.sample.df.manageDf.model.entity.UniTaskPlanEntity;
import com.dji.sample.df.supControlDf.service.TaskcontrolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 定时巡检需要执行的任务，并创建对应的立即执行任务，发送给无人机
 * @author 贾彬
 * @Time 2024/11/25 10:42
 */
@Slf4j
@Component
public class TaskControlTimer {

    @Autowired
    private IUniTaskPlanMapper iUniTaskPlanMapper;
    @Autowired
    private ICenterToUavPlanMapper iCenterToUavPlanMapper;
    @Autowired
    private CenterMsgPushHandler centerMsgPushHandler;
    @Autowired
    private TaskcontrolService pubWaylineJobPlanDfService;

    /**
     * 扫描需要执行的巡视任务
     */
    @Scheduled(fixedDelay = 5 * 1000L)
    public void scanPatrolPlan() {
        //获取此时应该执行的任务，包括周期任务、定时任务、间隔任务
        List<UniTaskPlanEntity> list = iUniTaskPlanMapper.getScheduledPlan();
        if (list != null && list.size() > 0) {
            for (UniTaskPlanEntity plan : list) {
                log.info("【无人机巡视方案开始执行】方案名称：" + plan.getPlanName() + "，方案编码：" + plan.getPlanNo());
                String[] split = plan.getWaylineList().split(",");
                if (split != null && split.length > 0) {
                    //当一个任务包含了多个航线时，需要循环执行
                    List<String> waylines = Arrays.asList(split);
                    String uuid = TaskUtils.genTaskPatrolledId(plan.getPlanNo());
                    boolean isRunning = true;
                    for (String wayline : waylines) {
                        //将当前任务转换为无人机定时任务，插入无人机任务库表，调用立即执行方法，并在当前中心端任务表内更新历史任务执行时间
                        PubWaylineJobPlanDfEntity job = new PubWaylineJobPlanDfEntity();
                        job.setPlanId(plan.getPlanNo());
                        job.setSubCode(plan.getSubCode());
                        job.setFileId(plan.getWaylineList());
                        job.setName(plan.getPlanName());
                        String isTaskPatrolledId = null;
                        try {
                            isTaskPatrolledId = pubWaylineJobPlanDfService.SuperiorCreatePlanJob(job);
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                        if (!isTaskPatrolledId.equals("false")) {
                            //下发给无人机的任务和上级任务需要映射
                            CenterToUavPlanEntity center = new CenterToUavPlanEntity();
                            center.setSubCode(plan.getSubCode());
                            center.setCenterPlanCode(plan.getPlanNo());
                            center.setUavPlanCode(isTaskPatrolledId);
                            center.setWayline(wayline);
                            //使用一个总的uuid，用来指代正在执行的大任务下的所有小任务
                            center.setCenterTaskPatrolledId(uuid);
                            center.setStatus(2);
                            center.setStartTime(DateUtils.getNowDate());
                            iCenterToUavPlanMapper.insert(center);
                        } else {
                            isRunning = false;
                        }
                    }
                    if (isRunning) {
                        //任务正常执行，存储任务相关编码的映射关系
                        List<BaseItem> items = new ArrayList<>();
                        PatrolStatusItem item = new PatrolStatusItem();
                        item.setTask_patrolled_id(uuid);
                        item.setTask_name(plan.getPlanName());
                        item.setTask_code(plan.getPlanNo());
                        item.setTask_state("2");
                        item.setStart_time(DateUtils.getNowDateTimeStr());
                        item.setTask_progress("0%");
                        item.setTask_estimated_time("");
                        item.setDescription("");
                        item.setPlan_start_time(DateUtils.getNowDateTimeStr());
                        items.add(item);
                        centerMsgPushHandler.pushPatrolStatusStart(items);
                        //添加任务表中的任务执行历史时间，用以下次执行时的循环判断
                        plan.setHistaskInsertTime(DateUtils.getNowDate());
                        iUniTaskPlanMapper.updateById(plan);
                    }
                }
            }
        }
    }
}
