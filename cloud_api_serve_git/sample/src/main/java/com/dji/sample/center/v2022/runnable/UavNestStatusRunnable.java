package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.utils.SpringUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.UavNestStatusDataItem;
import com.dji.sample.center.v2022.data.IntervalProtocolData;
import com.dji.sample.df.manageDf.dao.IUavDeviceMapper;
import com.dji.sample.df.uavCommonHandleDf.dao.DroneMonitoringEntityMapper;
import com.dji.sample.df.uavCommonHandleDf.model.entity.DroneMonitoringEntity;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 无人机机巢状态数据上送给巡视上级
 *
 * @author lyc
 * @Time 2022/3/26 15:14
 */
@Slf4j
public class UavNestStatusRunnable extends IntervalBaseRunnable {

    private IUavDeviceMapper iUavDeviceMapper = SpringUtils.getBean(IUavDeviceMapper.class);
    private IDeviceMapper iDeviceMapper = SpringUtils.getBean(IDeviceMapper.class);
    private DroneMonitoringEntityMapper droneMonitoringEntityMapper = SpringUtils.getBean(DroneMonitoringEntityMapper.class);

    public UavNestStatusRunnable(IntervalProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                //发送机巢状态数据
                intervalNestRunData();
                Thread.sleep(intervalSeconds * 1000);
            } catch (Exception e) {
                log.error("【发送无人机机巢状态数据给中心端】执行间隔上报异常：{}", e.getMessage());
            }
        }
    }

    /**
     * 发送机巢状态数据
     *
     * @return
     */
    private void intervalNestRunData() {
        PatrolHostCommand commandData = new PatrolHostCommand();
//        List<UavDeviceEntity> list = iUavDeviceMapper.selectByCondition(
//                Wrappers.lambdaQuery(UavDeviceEntity.class)
//                        .eq(UavDeviceEntity::getMainDeviceType, 3)
//        );
        List<DeviceEntity> list = iDeviceMapper.selectList(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));
        if (list == null) {
            list = new ArrayList<>();
        }
        DroneMonitoringEntity droneMonitoringEntity = droneMonitoringEntityMapper.selectOne(
                new LambdaQueryWrapper<DroneMonitoringEntity>()
                        .orderByDesc(DroneMonitoringEntity::getId)
                        .last("limit 1")
        );
        for (DeviceEntity entity : list) {
            UavNestStatusDataItem item1 = createCommonBean(entity);
            UavNestStatusDataItem item2 = createCommonBean(entity);
            UavNestStatusDataItem item3 = createCommonBean(entity);


            /*<2>: = 舱门状态 <3>: = 平台状态 <4>: = 充电状态
             */

            //加Optional.ofNullable目的是：如果这个字段值是null，传递的xml报文里就没有这个字段了，不符合规范
            item1.setType("2");
            String valueUnit = "";
            item1.setValue_unit(valueUnit);
            item1.setValue(droneMonitoringEntity.getNestDoorStatus());
            item1.setUnit("");
//          平台默认一直开启
            item2.setType("3");
            valueUnit = "";
            item2.setValue_unit(valueUnit);
            item2.setValue("1");
            item2.setUnit("");
//          机巢默认一直充电中
            item3.setType("4");
            valueUnit = "";
            item3.setValue_unit(valueUnit);
            item3.setValue("1");
            item3.setUnit("");

            commandData.addItem(item1);
            commandData.addItem(item2);
            commandData.addItem(item3);
        }

        log.info("【发送无人机机巢状态数据给巡视上级】间隔发送 ========> ");
        commandData.setSendCode(patrolHostCode);
        commandData.setReceiveCode(centerCode);
        commandData.setType("20001");
        patrolHostSocketClient.sendCommand(commandData, UavNestStatusDataItem.class);
    }

    /**
     * 公共数据处理
     *
     * @param entity
     * @return
     */
    private UavNestStatusDataItem createCommonBean(DeviceEntity entity) {
        UavNestStatusDataItem item = new UavNestStatusDataItem();

        item.setNest_code(entity.getDeviceSn());
        item.setNest_name(entity.getDeviceName());
        item.setModule_no("");
        return item;
    }
}
