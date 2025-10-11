package com.dji.sample.center.v2022.handler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dji.sample.center.config.CenterNormalConfig;
import com.dji.sample.center.dao.ICenterToUavPlanMapper;
import com.dji.sample.center.model.entity.CenterToUavPlanEntity;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.*;
import com.dji.sample.center.v2022.tool.BaseItem;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointCount.PointCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 无人机系统主动向对应巡视上级推送接口
 * @author 贾彬
 * @Time 2024/11/21 14:04
 */
@Slf4j
@Component
public class CenterMsgPushHandler {

    @Autowired
    private PatrolHostSocketClient patrolHostSocketClient;
    @Autowired
    private CenterNormalConfig centerNormalConfig;
    @Autowired
    private ICenterToUavPlanMapper iCenterToUavPlanMapper;


    /**
     * J.6.6 巡视设备坐标
     */
    public void pushCoordinate(List<UavCoordinateItem> list, String deviceCode) {
        if (!isConnectCenter()) {
            log.info("当前主站连接异常，巡视设备坐标上送失败");
            return;
        }
        if (list != null && list.size() > 0) {
            PatrolHostCommand commandData = new PatrolHostCommand();
            commandData.addItems(list);
            commandData.setSendCode(centerNormalConfig.getServerCode());
            commandData.setReceiveCode(centerNormalConfig.getStationCode());
            commandData.setType("3");
            commandData.setCode(deviceCode);
            patrolHostSocketClient.sendCommand(commandData, UavHostRunDataItem.class);
        }
    }



    /**
     * J.6.7 巡视路线
     */
    public void pushRoute(List<UavPatrolLineItem> list, String deviceCode) {
        if (!isConnectCenter()) {
            log.info("当前主站连接异常，巡视路线上送失败");
            return;
        }
        if (list != null && list.size() > 0) {
            PatrolHostCommand commandData = new PatrolHostCommand();
            commandData.addItems(list);
            commandData.setSendCode(centerNormalConfig.getServerCode());
            commandData.setReceiveCode(centerNormalConfig.getStationCode());
            commandData.setType("4");
            commandData.setCode(deviceCode);
            patrolHostSocketClient.sendCommand(commandData, UavHostRunDataItem.class);
        }
    }


    /**
     * J.6.8 巡视设备异常告警
     */
    public void pushDeviceAlarm(List<UavDeviceAlarmItem> list) {
        if (!isConnectCenter()) {
            log.info("当前主站连接异常，巡视设备异常告警上送失败");
            return;
        }
        if (list != null && list.size() > 0) {
            PatrolHostCommand commandData = new PatrolHostCommand();
            commandData.addItems(list);
            commandData.setSendCode(centerNormalConfig.getServerCode());
            commandData.setReceiveCode(centerNormalConfig.getStationCode());
            commandData.setType("5");
            patrolHostSocketClient.sendCommand(commandData, UavHostRunDataItem.class);
        }
    }



    /**
     * J.6.10 任务状态数据 - 起始
     */
    public void pushPatrolStatusStart(List<BaseItem> list) {
        if (!isConnectCenter()) {
            log.info("当前主站连接异常，任务状态上送失败");
            return;
        }
        if (list != null && list.size() > 0) {
            PatrolHostCommand commandData = new PatrolHostCommand();
            commandData.addItems(list);
            commandData.setSendCode(centerNormalConfig.getServerCode());
            commandData.setReceiveCode(centerNormalConfig.getStationCode());
            commandData.setType("41");
            patrolHostSocketClient.sendCommand(commandData, PatrolStatusItem.class);
        }
    }

    /**
     * J.6.10 任务状态数据 - 非起始，暂时只考虑一个任务一条航线的情况
     */
    public void pushPatrolStatusOther(String uavTaskPatrolledId, String planName, String state, String taskProgress) {
        if (!isConnectCenter()) {
            log.info("当前主站连接异常，任务状态上送失败");
            return;
        }
        CenterToUavPlanEntity center = iCenterToUavPlanMapper.selectOne(
                Wrappers.lambdaQuery(CenterToUavPlanEntity.class)
                .eq(CenterToUavPlanEntity::getUavPlanCode, uavTaskPatrolledId)
        );

        List<PatrolStatusItem> patrolStatusItems = new ArrayList<>();
        PatrolStatusItem item = new PatrolStatusItem();
        item.setTask_patrolled_id(center.getCenterTaskPatrolledId());
        item.setTask_name(planName);
        item.setTask_code(center.getCenterPlanCode());
        item.setTask_state(state);
        item.setStart_time(DateUtils.parseDateToStr(center.getStartTime()));
        item.setPlan_start_time(DateUtils.parseDateToStr(center.getStartTime()));
        item.setTask_progress(taskProgress);
        item.setTask_estimated_time("");
        item.setDescription("");
        patrolStatusItems.add(item);

        PatrolHostCommand commandData = new PatrolHostCommand();
        commandData.addItems(patrolStatusItems);
        commandData.setSendCode(centerNormalConfig.getServerCode());
        commandData.setReceiveCode(centerNormalConfig.getStationCode());
        commandData.setType("41");
        patrolHostSocketClient.sendCommand(commandData, PatrolStatusItem.class);

        center.setStatus(Integer.valueOf(state));
        if (state.equals("2")) {
            center.setEndTime(DateUtils.getNowDate());
        }
        iCenterToUavPlanMapper.updateById(center);
    }



    /**
     * J.6.11 巡视结果
     */
    public void pushPatrolResult(List<PointCount> list, String uavTaskPatrolledId, String subCode, String deviceName, String deviceCode) {
        if (!isConnectCenter()) {
            log.info("当前主站连接异常，巡视结果上送失败");
            return;
        }
        com.dji.sample.center.v2022.upload.PatrolHostTaskResultUpload taskPointResultUpload =
                com.dji.sample.center.v2022.upload.PatrolHostTaskResultUpload.getSingletonHandler();
        taskPointResultUpload.uploadToCenter(list, uavTaskPatrolledId, subCode, deviceName, deviceCode);
    }

    /**
     * 判断主站对接是否存在
     */
    public boolean isConnectCenter() {
        if (patrolHostSocketClient.getSocket() == null) {
            return false;
        }
        return true;
    }
}
