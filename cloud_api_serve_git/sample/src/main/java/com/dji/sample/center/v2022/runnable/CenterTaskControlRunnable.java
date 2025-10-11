package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.dao.ICenterToUavPlanMapper;
import com.dji.sample.center.model.entity.CenterToUavPlanEntity;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.StringUtil;
import com.dji.sample.center.utils.TaskUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.base.PatrolHostCommandSimple;
import com.dji.sample.center.v2022.command.control.CenterTaskControlItem;
import com.dji.sample.center.v2022.command.upload.PatrolStatusItem;
import com.dji.sample.center.v2022.data.CenterProtocolData;
import com.dji.sample.center.v2022.handler.CenterMsgPushHandler;
import com.dji.sample.center.v2022.tool.BaseItem;
import com.dji.sample.center.v2022.tool.CenterXmlTool;
import com.dji.sample.df.electricInspectionDf.dao.PubSubstationDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.manageDf.dao.IUniTaskPlanMapper;
import com.dji.sample.df.manageDf.model.entity.UniTaskPlanEntity;
import com.dji.sample.df.supControlDf.service.TaskcontrolService;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 巡视上级任务控制指令处理
 *
 * @author lyc
 * @date 2022/4/1
 */
@Slf4j
public class CenterTaskControlRunnable extends CenterMessageBaseRunnable {

    private ICenterToUavPlanMapper iCenterToUavPlanMapper = AppContext.getBean(ICenterToUavPlanMapper.class);
    private IUniTaskPlanMapper iUniTaskPlanMapper = AppContext.getBean(IUniTaskPlanMapper.class);
    //private PubWaylineJobPlanDfService pubWaylineJobPlanDfService = AppContext.getBean(PubWaylineJobPlanDfService.class);
    private  TaskcontrolService pubWaylineJobPlanDfService = AppContext.getBean( TaskcontrolService.class);

    private CenterMsgPushHandler centerMsgPushHandler = AppContext.getBean(CenterMsgPushHandler.class);
    //场站mapper
    private PubSubstationDfMapper pubSubstationDfMapper=AppContext.getBean(PubSubstationDfMapper.class);

    public CenterTaskControlRunnable(CenterProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        try {
            PatrolHostCommandSimple commandSimple = CenterXmlTool.deserialize(xmlMessage);
            log.info("【接收巡视上级无人机任务控制指令】正在处理 ========> ");
            centerTaskHandle(commandSimple);
            log.info("【接收巡视上级无人机任务控制指令】处理完毕 ========> ");
        } catch (Exception e) {
            log.error("【巡视上级任务控制处理】处理异常，巡视上级IP：{}，异常信息：{}", this.centerIp, e.getMessage());
        }
    }

    /**
     * 处理任务控制指令
     */
    private void centerTaskHandle(PatrolHostCommandSimple commandSimple) {
        if (commandSimple.getCommand().equals("1"))
        {
            String centerPlanId = commandSimple.getCode();
            UniTaskPlanEntity entity = iUniTaskPlanMapper.selectOne(
                    Wrappers.lambdaQuery(UniTaskPlanEntity.class)
                            .eq(UniTaskPlanEntity::getPlanNo, centerPlanId)
            );

            if (entity == null) {
                this.sendTaskControlResponse("", "500");
                return;
            }

            PubWaylineJobPlanDfEntity job = new PubWaylineJobPlanDfEntity();
            job.setPlanId(entity.getPlanNo());
            job.setSubCode(entity.getSubCode());
            job.setFileId(entity.getWaylineList());
            job.setName(entity.getPlanName());
            String isTaskPatrolled = null;
            try {
                isTaskPatrolled = pubWaylineJobPlanDfService.SuperiorCreatePlanJob(job);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            if (!isTaskPatrolled.equals("false")) {
                CenterToUavPlanEntity center = new CenterToUavPlanEntity();
                center.setSubCode(entity.getSubCode());
                center.setCenterPlanCode(centerPlanId);
                center.setUavPlanCode(isTaskPatrolled);
                center.setCenterTaskPatrolledId(TaskUtils.genTaskPatrolledId(entity.getPlanNo()));
                center.setWayline(entity.getWaylineList());
                center.setStatus(2);
                center.setStartTime(DateUtils.getNowDate());
                this.sendTaskControlResponse(center.getCenterTaskPatrolledId(), "200");
                center.setStartTime(DateUtils.getNowDate());
                iCenterToUavPlanMapper.insert(center);
                //添加任务表中的任务执行历史时间，用以下次执行时的循环判断
                entity.setHistaskInsertTime(DateUtils.getNowDate());
                iUniTaskPlanMapper.updateById(entity);
                this.sendTaskControlResponse(center.getCenterTaskPatrolledId(), "200");
                //任务正常执行，存储任务相关编码的映射关系
                List<BaseItem> items = new ArrayList<>();
                PatrolStatusItem item = new PatrolStatusItem();
                item.setTask_patrolled_id(center.getCenterTaskPatrolledId());
                item.setTask_name(entity.getPlanName());
                item.setTask_code(entity.getPlanNo());
                item.setTask_state("2");
                item.setStart_time(DateUtils.getNowDateTimeStr());
                item.setTask_progress("0%");
                item.setTask_estimated_time("");
                item.setDescription("");
                item.setPlan_start_time(DateUtils.getNowDateTimeStr());
                items.add(item);
                centerMsgPushHandler.pushPatrolStatusStart(items);
            } else {
                this.sendTaskControlResponse("", "500");
            }
        } else
        {
            //调用无人机任务控制接口
            String centerTaskPatrolledId = commandSimple.getCode();
            CenterToUavPlanEntity center = iCenterToUavPlanMapper.selectOne(
                    Wrappers.lambdaQuery(CenterToUavPlanEntity.class)
                            .eq(CenterToUavPlanEntity::getCenterTaskPatrolledId, centerTaskPatrolledId)
            );

            String isControl = pubWaylineJobPlanDfService.superiorTaskHandling(center.getUavPlanCode(),commandSimple.getType(),commandSimple.getCommand());
            if (isControl.equals("true")) {
                if (commandSimple.getCommand().equals("2")) {
                    //任务控制、暂停
                    //下发任务暂停指令
                    center.setStatus(3);
                }
                if (commandSimple.getCommand().equals("3")) {
                    //任务继续
                    center.setStatus(2);
                }
                if (commandSimple.getCommand().equals("4")) {
                    //任务停止
                    center.setStatus(4);
                }
                iCenterToUavPlanMapper.updateById(center);
                this.sendTaskControlResponse(centerTaskPatrolledId, "200");
            } else {
                this.sendTaskControlResponse(centerTaskPatrolledId, "500");
            }
        }
    }


    /**
     * 给上级发送任务控制响应
     */
    private void sendTaskControlResponse(String task_patrolled_id, String code) {
        if (StringUtil.isEmpty(task_patrolled_id)) {
            task_patrolled_id = "";
        }

        List<BaseItem> items = new ArrayList<>();
        CenterTaskControlItem centerTaskControlItem = new CenterTaskControlItem();
        centerTaskControlItem.setTask_patrolled_id(task_patrolled_id);
        items.add(centerTaskControlItem);

        PatrolHostCommand patrolHostCommand = patrolHostSocketClient.getBaseCommand("251", "4", code);
        patrolHostCommand.addItems(items);
        patrolHostSocketClient.responseCommand(patrolHostCommand, CenterTaskControlItem.class, requestSerialNum);
    }
}
