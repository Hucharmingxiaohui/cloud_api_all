package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.SpringUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.UavHostEnvItem;
import com.dji.sample.center.v2022.data.IntervalProtocolData;
import com.dji.sample.df.manageDf.dao.IUavDeviceMapper;
import com.dji.sample.df.wind.dao.DroneMonitoringEntityMapper;
import com.dji.sample.df.wind.model.entity.DroneMonitoringEntity;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 无人机环境数据上送给巡视上级
 *
 * @author lyc
 * @Time 2022/3/26 15:13
 */
@Slf4j
public class UavHostEnvRunnable extends IntervalBaseRunnable {

    private IUavDeviceMapper iUavDeviceMapper = SpringUtils.getBean(IUavDeviceMapper.class);
    private IDeviceMapper iDeviceMapper = SpringUtils.getBean(IDeviceMapper.class);
    private DroneMonitoringEntityMapper droneMonitoringEntityMapper = SpringUtils.getBean(DroneMonitoringEntityMapper.class);

    public UavHostEnvRunnable(IntervalProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                //发送环境数据
                intervalWeatherData();
                Thread.sleep(intervalSeconds * 1000);
            } catch (Exception e) {
                log.error("【发送无人机系统环境数据给巡视上级】执行间隔上报异常：{}", e.getMessage());
            }
        }
    }

    /**
     * 发送环境数据
     *
     * @return
     */
    private void intervalWeatherData() {
        PatrolHostCommand commandData = new PatrolHostCommand();
//
//        List<UavDeviceEntity> list = iUavDeviceMapper.selectByCondition(
//                Wrappers.lambdaQuery(UavDeviceEntity.class)
//        );
        List<DeviceEntity> list = iDeviceMapper.selectList(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));
        DroneMonitoringEntity droneMonitoringEntity = droneMonitoringEntityMapper.selectOne(
                new LambdaQueryWrapper<DroneMonitoringEntity>()
                        .orderByDesc(DroneMonitoringEntity::getId)
                        .last("limit 1")
        );
        if (list != null && list.size() > 0) {
            for (DeviceEntity entity : list) {
                UavHostEnvItem itemUav1 = createCommonBeanUav(entity);
                UavHostEnvItem itemUav2 = createCommonBeanUav(entity);
                UavHostEnvItem itemUav3 = createCommonBeanUav(entity);

            /* <1>: = 环境温度  <3>: = 风速 <4>: = 雨量 */
                itemUav1.setType("1");
                itemUav1.setValue_unit(droneMonitoringEntity.getAmbientTemperature()+"°C");
                itemUav1.setValue(droneMonitoringEntity.getAmbientTemperature());
                itemUav1.setUnit("°C");

                itemUav2.setType("3");
                itemUav2.setValue_unit(droneMonitoringEntity.getWindSpeed()+"m/s");
                itemUav2.setValue(droneMonitoringEntity.getWindSpeed());
                itemUav2.setUnit("m/s");

                itemUav3.setType("4");
                itemUav3.setValue_unit("");
                itemUav3.setValue(droneMonitoringEntity.getRainfall());
                itemUav3.setUnit("");

                commandData.addItem(itemUav1);
                commandData.addItem(itemUav2);
                commandData.addItem(itemUav3);
            }
        }

        log.info("【发送环境数据给巡视上级】间隔发送 ========> ");
        commandData.setSendCode(patrolHostCode);
        commandData.setReceiveCode(centerCode);
        commandData.setType("21");
        patrolHostSocketClient.sendCommand(commandData, UavHostEnvItem.class);
    }

    /**
     * 无人机公共数据封装
     */
    private UavHostEnvItem createCommonBeanUav(DeviceEntity entity) {
        UavHostEnvItem item = new UavHostEnvItem();
        item.setPatroldevice_code(entity.getDeviceSn());
        item.setPatroldevice_name(entity.getDeviceName());
        //time
        item.setTime(DateUtils.getNowDateTimeStr());
        return item;
    }
}
