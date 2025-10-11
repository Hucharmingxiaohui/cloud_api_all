package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.SpringUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.UavHostStatusDataItem;
import com.dji.sample.center.v2022.data.IntervalProtocolData;
import com.dji.sample.df.manageDf.dao.IUavDeviceMapper;
import com.dji.sample.df.manageDf.model.entity.UavDeviceEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

/**
 * 无人机-巡视设备状态数据上送给上级
 *
 * @author lyc
 * @Time 2022/3/26 15:43
 */
@Slf4j
public class UavHostStatusRunnable extends IntervalBaseRunnable {

    private IUavDeviceMapper iUavDeviceMapper = SpringUtils.getBean(IUavDeviceMapper.class);

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
                //数据库查询填充数据
                // 水平速度
                UavHostStatusDataItem item1 = createCommonBean(uavDevice);
                String valueUnit = "";
                item1.setType("1");
                item1.setValue_unit(valueUnit);
                item1.setValue("");
                item1.setUnit("");
                commandData.addItem(item1);

                // 行驶里程
                UavHostStatusDataItem item2 = createCommonBean(uavDevice);
                valueUnit = "";
                item2.setType("2");
                item2.setValue_unit(valueUnit);
                item2.setValue("");
                item2.setUnit("");
                commandData.addItem(item2);

                // 电池电量
                UavHostStatusDataItem item3 = createCommonBean(uavDevice);
                valueUnit = "";
                item3.setType("3");
                item3.setValue_unit(valueUnit);
                item3.setValue("");
                item3.setUnit("");
                commandData.addItem(item3);

                // 垂直速度
                UavHostStatusDataItem item4 = createCommonBean(uavDevice);
                valueUnit = "";
                item4.setType("4");
                item4.setValue_unit(valueUnit);
                item4.setValue("");
                item4.setUnit("");
                commandData.addItem(item4);

                // 飞行距离
                UavHostStatusDataItem item5 = createCommonBean(uavDevice);
                valueUnit = "";
                item5.setType("5");
                item5.setValue_unit(valueUnit);
                item5.setValue("");
                item5.setUnit("");
                commandData.addItem(item5);

                // 飞行高度
                UavHostStatusDataItem item6 = createCommonBean(uavDevice);
                valueUnit = "";
                item6.setType("6");
                item6.setValue_unit(valueUnit);
                item6.setValue("");
                item6.setUnit("");
                commandData.addItem(item6);

                // 飞行时长
                UavHostStatusDataItem item7 = createCommonBean(uavDevice);
                valueUnit = "";
                item7.setType("7");
                item7.setValue_unit(valueUnit);
                item7.setValue("");
                item7.setUnit("");
                commandData.addItem(item7);

                // 云台俯仰角
                UavHostStatusDataItem item8 = createCommonBean(uavDevice);
                valueUnit = "";
                item8.setType("8");
                item8.setValue_unit(valueUnit);
                item8.setValue("");
                item8.setUnit("");
                commandData.addItem(item8);

                // 云台横滚角
                UavHostStatusDataItem item9 = createCommonBean(uavDevice);
                valueUnit = "";
                item9.setType("9");
                item9.setValue_unit(valueUnit);
                item9.setValue("");
                item9.setUnit("");
                commandData.addItem(item9);

                // 云台偏航角
                UavHostStatusDataItem item10 = createCommonBean(uavDevice);
                valueUnit = "";
                item10.setType("10");
                item10.setValue_unit(valueUnit);
                item10.setValue("");
                item10.setUnit("");
                commandData.addItem(item10);
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
    private UavHostStatusDataItem createCommonBean(UavDeviceEntity device) {
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
