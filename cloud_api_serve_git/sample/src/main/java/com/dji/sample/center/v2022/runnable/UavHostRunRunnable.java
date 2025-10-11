package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.SpringUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.UavHostRunDataItem;
import com.dji.sample.center.v2022.data.IntervalProtocolData;
import com.dji.sample.common.util.SpringBeanUtilsTest;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.df.manageDf.dao.IUavDeviceMapper;
import com.dji.sample.df.manageDf.model.entity.UavDeviceEntity;
import com.dji.sdk.cloudapi.device.DroneBattery;
import com.dji.sdk.cloudapi.device.OsdDockDrone;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 无人机-巡视设备运行数据上送给上级
 *
 * @author lyc
 * @Time 2022/3/26 15:43
 */
@Slf4j
public class UavHostRunRunnable extends IntervalBaseRunnable {

    private IUavDeviceMapper iUavDeviceMapper = SpringUtils.getBean(IUavDeviceMapper.class);

    public UavHostRunRunnable(IntervalProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                //发送运行数据
                intervalRunData();
                Thread.sleep(intervalSeconds * 1000);
            } catch (Exception e) {
                log.error("【发送无人机巡视设备运行数据给上级】执行间隔上报异常：{}", e.getMessage());
            }
        }
    }

    /**
     * 发送无人机的巡视设备运行数据
     */
    private void intervalRunData() {
        PatrolHostCommand commandData = new PatrolHostCommand();
        List<UavDeviceEntity> list = iUavDeviceMapper.selectByCondition(
                Wrappers.lambdaQuery(UavDeviceEntity.class)
                .eq(UavDeviceEntity::getMainDeviceType, 1)
        );
        if (list != null && list.size() > 0) {
            for (UavDeviceEntity uavDevice : list) {
                //无人机数据
                Optional<OsdDockDrone> deviceOpt = SpringBeanUtilsTest.getBean(IDeviceRedisService.class)
                        .getDeviceOsd(uavDevice.getDeviceSn(), OsdDockDrone.class);
                OsdDockDrone osdDockDrone=new OsdDockDrone();//存储无人机数据
                if(deviceOpt.isPresent())
                {
                    osdDockDrone=deviceOpt.get();
                }
                if(osdDockDrone.getHorizontalSpeed()==null)
                {
                    //不在线置为默认值
                    osdDockDrone.setHorizontalSpeed((float)0.0);
                }
                // 水平速度
                UavHostRunDataItem item1 = createCommonBean(uavDevice);
                //带单位的值
                String valueUnit = Float.toString( osdDockDrone.getHorizontalSpeed())+"m/s";
                item1.setType("1");
                item1.setValue_unit(valueUnit);
                //不带单位的值
                item1.setValue(Float.toString( osdDockDrone.getHorizontalSpeed()));
                item1.setUnit("m/s");
                commandData.addItem(item1);

                // 行驶里程
                if(osdDockDrone.getTotalFlightDistance()==null)
                {
                    //不在线置为默认值
                    osdDockDrone.setTotalFlightDistance(0.0);
                }
                UavHostRunDataItem item2 = createCommonBean(uavDevice);
                valueUnit = Double.toString(osdDockDrone.getTotalFlightDistance())+"km";
                item2.setType("2");
                item2.setValue_unit(valueUnit);
                item2.setValue(Double.toString(osdDockDrone.getTotalFlightDistance()));
                item2.setUnit("km");
                commandData.addItem(item2);

                // 电池电量
                if(osdDockDrone.getBattery()==null)
                {
                    //不在线置为默认值
                    DroneBattery droneBattery=new DroneBattery();
                    droneBattery.setCapacityPercent(0);
                    osdDockDrone.setBattery(droneBattery);
                }
                UavHostRunDataItem item3 = createCommonBean(uavDevice);
                valueUnit = Double.toString(osdDockDrone.getBattery().getCapacityPercent())+"%";
                item3.setType("3");
                item3.setValue_unit(valueUnit);
                item3.setValue(Double.toString(osdDockDrone.getBattery().getCapacityPercent()));
                item3.setUnit("%");
                commandData.addItem(item3);

                // 垂直速度
                if(osdDockDrone.getVerticalSpeed()==null)
                {
                    //不在线置为默认值
                    osdDockDrone.setVerticalSpeed((float)0.0);
                }
                UavHostRunDataItem item4 = createCommonBean(uavDevice);
                valueUnit = Float.toString(osdDockDrone.getVerticalSpeed())+"m/s";
                item4.setType("4");
                item4.setValue_unit(valueUnit);
                item4.setValue(Float.toString(osdDockDrone.getVerticalSpeed()));
                item4.setUnit("m/s");
                commandData.addItem(item4);

                // 飞行距离
                if(osdDockDrone.getHomeDistance()==null)
                {
                    //不在线置为默认值
                    osdDockDrone.setHomeDistance((float)0.0);
                }
                UavHostRunDataItem item5 = createCommonBean(uavDevice);
                valueUnit = Float.toString(osdDockDrone.getHomeDistance())+"km";
                item5.setType("5");
                item5.setValue_unit(valueUnit);
                item5.setValue( Float.toString(osdDockDrone.getHomeDistance()));
                item5.setUnit("km");
                commandData.addItem(item5);

                // 飞行高度
                if(osdDockDrone.getHeight()==null)
                {
                    //不在线置为默认值
                    osdDockDrone.setHeight((float)0.0);
                }
                UavHostRunDataItem item6 = createCommonBean(uavDevice);
                valueUnit = Float.toString(osdDockDrone.getHeight())+"m";
                item6.setType("6");
                item6.setValue_unit(valueUnit);
                item6.setValue(Float.toString(osdDockDrone.getHeight()));
                item6.setUnit("m");
                commandData.addItem(item6);
                //Optional.ofNullable(osdDockDrone.getTotalFlightTime()).orElse((float)0.0);

                // 飞行时长
                if(osdDockDrone.getTotalFlightTime()==null)
                {
                    //不在线置为默认值
                    osdDockDrone.setTotalFlightTime((float)0.0);
                }
                UavHostRunDataItem item7 = createCommonBean(uavDevice);
                valueUnit = Float.toString(osdDockDrone.getTotalFlightTime())+"h";
                item7.setType("7");
                item7.setValue_unit(valueUnit);
                item7.setValue(Float.toString(osdDockDrone.getTotalFlightTime()));
                item7.setUnit("h");
                commandData.addItem(item7);

                // 云台俯仰角，无人机不支持获取当前参数
                UavHostRunDataItem item8 = createCommonBean(uavDevice);
                valueUnit = "";
                item8.setType("8");
                item8.setValue_unit(valueUnit);
                item8.setValue("");
                item8.setUnit("");
                commandData.addItem(item8);

                // 云台横滚角，无人机不支持获取当前参数
                UavHostRunDataItem item9 = createCommonBean(uavDevice);
                valueUnit = "";
                item9.setType("9");
                item9.setValue_unit(valueUnit);
                item9.setValue("");
                item9.setUnit("");
                commandData.addItem(item9);

                // 云台偏航角，无人机不支持获取当前参数
                UavHostRunDataItem item10 = createCommonBean(uavDevice);
                valueUnit = "";
                item10.setType("10");
                item10.setValue_unit(valueUnit);
                item10.setValue("");
                item10.setUnit("");
                commandData.addItem(item10);
            }
        }

        log.info("【发送无人机巡视设备运行数据给巡视上级】间隔发送 ========> ");
        commandData.setSendCode(patrolHostCode);
        commandData.setReceiveCode(centerCode);
        commandData.setType("2");
        patrolHostSocketClient.sendCommand(commandData, UavHostRunDataItem.class);
    }

    /**
     * 无人机公共字段封装
     *
     * @param device
     * @return
     */
    private UavHostRunDataItem createCommonBean(UavDeviceEntity device) {
        UavHostRunDataItem item = new UavHostRunDataItem();
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
