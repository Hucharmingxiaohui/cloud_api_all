package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.SpringUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.UavHostStatusDataItem;
import com.dji.sample.center.v2022.data.IntervalProtocolData;
import com.dji.sample.common.util.SpringBeanUtilsTest;
import com.dji.sample.component.mqtt.model.EventsReceiver;
import com.dji.sample.df.manageDf.dao.IUavDeviceMapper;
import com.dji.sample.df.wind.dao.DroneMonitoringEntityMapper;
import com.dji.sample.df.wind.model.entity.DroneMonitoringEntity;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sample.wayline.service.IWaylineRedisService;
import com.dji.sdk.cloudapi.wayline.FlighttaskProgress;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 无人机-巡视设备状态数据上送给上级
 *
 * @author lyc
 * @Time 2022/3/26 15:43
 */
@Slf4j
public class UavHostStatusRunnable extends IntervalBaseRunnable {

    private IUavDeviceMapper iUavDeviceMapper = SpringUtils.getBean(IUavDeviceMapper.class);
    private IDeviceMapper iDeviceMapper = SpringUtils.getBean(IDeviceMapper.class);
    private DroneMonitoringEntityMapper droneMonitoringEntityMapper = SpringUtils.getBean(DroneMonitoringEntityMapper.class);

    public UavHostStatusRunnable(IntervalProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                //发送状态数据
                intervalRunData();
                Thread.sleep(intervalSeconds * 1000);
            } catch (Exception e) {
                log.error("【发送无人机巡视设备状态数据给上级】执行间隔上报异常：{}", e.getMessage());
            }
        }
    }

    /**
     * 发送无人机的巡视设备状态数据
     */
    private void intervalRunData() {
        PatrolHostCommand commandData = new PatrolHostCommand();
//        List<UavDeviceEntity> list = iUavDeviceMapper.selectByCondition(
//                Wrappers.lambdaQuery(UavDeviceEntity.class)
//                .eq(UavDeviceEntity::getMainDeviceType, 1)
//        );

        List<DeviceEntity> list = iDeviceMapper.selectList(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 0));
        DroneMonitoringEntity droneMonitoringEntity = droneMonitoringEntityMapper.selectOne(
                new LambdaQueryWrapper<DroneMonitoringEntity>()
                        .orderByDesc(DroneMonitoringEntity::getId)
                        .last("limit 1")
        );
        String batteryLevel = droneMonitoringEntity.getBatteryLevel();
        String batteryLevel1 ="0";
        if (batteryLevel != null) {
            int i = Integer.parseInt(batteryLevel);
            if (i < 30) {
                batteryLevel1 ="1";
            }else {
                batteryLevel1 ="0";
            }
        }
        if (list != null && list.size() > 0) {
            for (DeviceEntity uavDevice : list) {
                //无人机数据
                //判断是否飞行需要获取机巢sn,目前一个，之后多机巢需要改
                DeviceEntity deviceEntity = iDeviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));
                Optional<EventsReceiver<FlighttaskProgress>> runningWaylineJob = SpringBeanUtilsTest.getBean(IWaylineRedisService.class)
                        .getRunningWaylineJob(deviceEntity.getDeviceSn());
                boolean present = runningWaylineJob.isPresent();
                String runState = "1";
                if (present) {
                    runState = "2";
                }
                // 电池电量
                UavHostStatusDataItem item1 = createCommonBean(uavDevice);
                String valueUnit = "";
                item1.setType("1");
                item1.setValue_unit(valueUnit);
                item1.setValue(batteryLevel1);
                item1.setUnit("");
                commandData.addItem(item1);

                // 通信状态
                UavHostStatusDataItem item2 = createCommonBean(uavDevice);
                valueUnit = "";
                item2.setType("2");
                item2.setValue_unit(valueUnit);
                item2.setValue("0");
                item2.setUnit("");
                commandData.addItem(item2);

                // 故障报警(默认无告警)
                UavHostStatusDataItem item3 = createCommonBean(uavDevice);
                valueUnit = "";
                item3.setType("5");
                item3.setValue_unit(valueUnit);
                item3.setValue("0");
                item3.setUnit("");
                commandData.addItem(item3);

                // 运行状态
                UavHostStatusDataItem item4 = createCommonBean(uavDevice);
                valueUnit = "";
                item4.setType("6");
                item4.setValue_unit(valueUnit);
                item4.setValue(runState);
                item4.setUnit("");
                commandData.addItem(item4);
            }
        }

        log.info("【发送无人机巡视设备状态数据给巡视上级】间隔发送 ========> ");
        commandData.setSendCode(patrolHostCode);
        commandData.setReceiveCode(centerCode);
        commandData.setType("1");
        patrolHostSocketClient.sendCommand(commandData, UavHostStatusDataItem.class);
    }

    /**
     * 无人机公共字段封装
     *
     * @param device
     * @return
     */
    private UavHostStatusDataItem createCommonBean(DeviceEntity device) {
        UavHostStatusDataItem item = new UavHostStatusDataItem();
        item.setPatroldevice_code(device.getDeviceSn());
        item.setPatroldevice_name(device.getDeviceName());
        //time
        Date run_update_time = DateUtils.getNowDate();
        String timeStr = DateUtils.parseDateToStr(new Date());
        if (run_update_time != null) {
            timeStr = DateUtils.parseDateToStr(run_update_time);
        }
        item.setTime(timeStr);
        return item;
    }
}
