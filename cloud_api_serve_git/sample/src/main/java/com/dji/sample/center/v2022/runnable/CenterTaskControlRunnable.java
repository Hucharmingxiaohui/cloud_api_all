package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.config.SwitchConfig;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.StringUtil;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.base.PatrolHostCommandSimple;
import com.dji.sample.center.v2022.command.control.CenterTaskControlItem;
import com.dji.sample.center.v2022.command.upload.PatrolStatusItem;
import com.dji.sample.center.v2022.data.CenterProtocolData;
import com.dji.sample.center.v2022.handler.CenterMsgPushHandler;
import com.dji.sample.center.v2022.tool.BaseItem;
import com.dji.sample.center.v2022.tool.CenterXmlTool;
import com.dji.sample.df.manageDf.dao.IUniTaskPlanMapper;
import com.dji.sample.df.manageDf.model.entity.UniTaskPlanEntity;
import com.dji.sample.df.uavHandlerDf.CenterTaskHandler;
import com.dji.sample.df.uavHandlerDf.CenterTaskHandler.TaskExecutionResult;
import com.dji.sample.df.wind.dao.WindTurbineMapper;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 巡视上级任务控制指令处理
 */
@Slf4j
public class CenterTaskControlRunnable extends CenterMessageBaseRunnable {

    private IUniTaskPlanMapper iUniTaskPlanMapper = AppContext.getBean(IUniTaskPlanMapper.class);
    private CenterMsgPushHandler centerMsgPushHandler = AppContext.getBean(CenterMsgPushHandler.class);
    private CenterTaskHandler centerTaskHandler = AppContext.getBean(CenterTaskHandler.class);
    private WindTurbineMapper windTurbineMapper = AppContext.getBean(WindTurbineMapper.class);
    private SwitchConfig switchConfig = AppContext.getBean(SwitchConfig.class);

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
            log.error("【巡视上级任务控制处理】处理异常，巡视上级IP：{}，异常信息：{}", this.centerIp, e.getMessage(), e);
        }
    }

    /**
     * 处理任务控制指令。Command=1为立即执行；暂停、恢复、终止目前暂不支持。
     */
    private void centerTaskHandle(PatrolHostCommandSimple commandSimple) {
        if ("1".equals(commandSimple.getCommand())) {
//          立即执行
            handleImmediateStart(commandSimple);
            return;
        }
//      任务暂停、恢复、终止等操作，目前暂不实现，旧逻辑暂时停用。
//      handleRunningTaskControl(commandSimple);
        log.warn("上级任务控制暂不支持: taskPatrolledId={}, command={}", commandSimple.getCode(), commandSimple.getCommand());
        this.sendTaskControlResponse(commandSimple.getCode(), "500");
    }

    /**
     * 立即执行上级任务：从已保存的任务方案取设备信息，复用任务下发到点后的真实执行链路。
     */
    private void handleImmediateStart(PatrolHostCommandSimple commandSimple) {
        String centerPlanId = commandSimple.getCode();
        UniTaskPlanEntity entity = iUniTaskPlanMapper.selectOne(
                Wrappers.lambdaQuery(UniTaskPlanEntity.class)
                        .eq(UniTaskPlanEntity::getPlanNo, centerPlanId)
        );
        if (entity == null) {
            log.warn("上级立即任务执行失败，未找到任务方案: centerPlanId={}", centerPlanId);
            this.sendTaskControlResponse("", "500");
            return;
        }

        ImmediateTask immediateTask = resolveImmediateTask(entity);
        if (immediateTask == null) {
            log.warn("上级立即任务执行失败，任务方案无法匹配当前无人机平台逻辑: planNo={}, deviceLevel={}, deviceList={}",
                    entity.getPlanNo(), entity.getDeviceLevel(), entity.getDeviceList());
            this.sendTaskControlResponse("", "500");
            return;
        }

        TaskExecutionResult result = centerTaskHandler.executeImmediateTask(immediateTask.planType, immediateTask.deviceId,
                entity.getPlanNo(), entity.getPlanName());
        if (result.isSuccess()) {
            String taskPatrolledId = result.getTaskId();
//          取消redis定时任务
            centerTaskHandler.cancelScheduledTaskAfterImmediateExecution(entity.getPlanNo());
//          当前EUA任务状态上送直接使用EUA返回的任务ID，不依赖center_to_uav_plan_df映射，旧映射保存先停用。
//          saveImmediateTaskMapping(entity, immediateTask, taskPatrolledId);
            entity.setHistaskInsertTime(DateUtils.getNowDate());
            iUniTaskPlanMapper.updateById(entity);
            this.sendTaskControlResponse(taskPatrolledId, "200");
            pushTaskStart(entity, taskPatrolledId);
        } else {
            this.sendTaskControlResponse("", "500");
        }
    }

    private ImmediateTask resolveImmediateTask(UniTaskPlanEntity entity) {
        if (entity.getDeviceLevel() == null || StringUtil.isEmpty(entity.getDeviceList())) {
            return null;
        }
        String[] deviceIds = entity.getDeviceList().split(",");
        if (deviceIds.length != 1) {
            return null;
        }
        String deviceId = deviceIds[0].trim();
        if (StringUtil.isEmpty(deviceId)) {
            return null;
        }

        if (entity.getDeviceLevel() == 2) {
            List<WindTurbine> windTurbines = windTurbineMapper.selectList(new HashMap<>());
            boolean matched = windTurbines.stream().anyMatch(wt -> deviceId.equals(wt.getId()));
            return matched ? new ImmediateTask(1, deviceId) : null;
        }

        if (entity.getDeviceLevel() == 1) {
            return new ImmediateTask(isCqDockTaskEnabled() ? 5 : 0, deviceId);
        }
        return null;
    }

//    private void saveImmediateTaskMapping(UniTaskPlanEntity entity, ImmediateTask immediateTask, String actualTaskId) {
//        CenterToUavPlanEntity center = new CenterToUavPlanEntity();
//        center.setSubCode(entity.getSubCode());
//        center.setCenterPlanCode(entity.getPlanNo());
//        center.setUavPlanCode(actualTaskId);
//        center.setCenterTaskPatrolledId(actualTaskId);
//        center.setWayline(immediateTask.deviceId);
//        center.setStatus(2);
//        center.setStartTime(DateUtils.getNowDate());
//        iCenterToUavPlanMapper.insert(center);
//    }

    private void pushTaskStart(UniTaskPlanEntity entity, String taskPatrolledId) {
        List<BaseItem> items = new ArrayList<>();
        PatrolStatusItem item = new PatrolStatusItem();
        item.setTask_patrolled_id(taskPatrolledId);
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
    }

    private boolean isCqDockTaskEnabled() {
        String enable = switchConfig.getCenterCqDockTaskEnable();
        return "true".equalsIgnoreCase(enable) || "1".equals(enable);
    }

//    private void handleRunningTaskControl(PatrolHostCommandSimple commandSimple) {
//        String centerTaskPatrolledId = commandSimple.getCode();
//        CenterToUavPlanEntity center = iCenterToUavPlanMapper.selectOne(
//                Wrappers.lambdaQuery(CenterToUavPlanEntity.class)
//                        .eq(CenterToUavPlanEntity::getCenterTaskPatrolledId, centerTaskPatrolledId)
//        );
//        if (center == null) {
//            log.warn("上级任务控制失败，未找到任务映射: taskPatrolledId={}, command={}",
//                    centerTaskPatrolledId, commandSimple.getCommand());
//            this.sendTaskControlResponse(centerTaskPatrolledId, "500");
//            return;
//        }
//
//        String isControl = pubWaylineJobPlanDfService.superiorTaskHandling(center.getUavPlanCode(), commandSimple.getType(), commandSimple.getCommand());
//        if ("true".equals(isControl)) {
//            if ("2".equals(commandSimple.getCommand())) {
//                center.setStatus(3);
//            }
//            if ("3".equals(commandSimple.getCommand())) {
//                center.setStatus(2);
//            }
//            if ("4".equals(commandSimple.getCommand())) {
//                center.setStatus(4);
//            }
//            iCenterToUavPlanMapper.updateById(center);
//            this.sendTaskControlResponse(centerTaskPatrolledId, "200");
//        } else {
//            this.sendTaskControlResponse(centerTaskPatrolledId, "500");
//        }
//    }

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

    private static class ImmediateTask {
        private final Integer planType;
        private final String deviceId;

        private ImmediateTask(Integer planType, String deviceId) {
            this.planType = planType;
            this.deviceId = deviceId;
        }
    }
}
