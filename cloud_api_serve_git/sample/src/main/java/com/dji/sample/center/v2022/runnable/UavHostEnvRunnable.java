package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.SpringUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.UavHostEnvItem;
import com.dji.sample.center.v2022.data.IntervalProtocolData;
import com.dji.sample.df.manageDf.dao.IUavDeviceMapper;
import com.dji.sample.df.manageDf.model.entity.UavDeviceEntity;
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

        List<UavDeviceEntity> list = iUavDeviceMapper.selectByCondition(
                Wrappers.lambdaQuery(UavDeviceEntity.class)
        );
        if (list != null && list.size() > 0) {
            for (UavDeviceEntity entity : list) {
                UavHostEnvItem itemUav1 = createCommonBeanUav(entity);
                UavHostEnvItem itemUav2 = createCommonBeanUav(entity);
                UavHostEnvItem itemUav3 = createCommonBeanUav(entity);
                UavHostEnvItem itemUav4 = createCommonBeanUav(entity);
                UavHostEnvItem itemUav5 = createCommonBeanUav(entity);
                UavHostEnvItem itemUav6 = createCommonBeanUav(entity);

            /* <1>: = 环境温度 <2>: = 环境湿度 <3>: = 风速 <4>: = 雨量
            <5>: = 风向 <6>: = 气压 */
                itemUav1.setType("1");
                itemUav1.setValue_unit("");
                itemUav1.setValue("");
                itemUav1.setUnit("");

                itemUav2.setType("2");
                itemUav2.setValue_unit("");
                itemUav2.setValue("");
                itemUav2.setUnit("");

                itemUav3.setType("3");
                itemUav3.setValue_unit("");
                itemUav3.setValue("");
                itemUav3.setUnit("");

                itemUav4.setType("4");
                itemUav4.setValue_unit("");
                itemUav4.setValue("");
                itemUav4.setUnit("");

                itemUav5.setType("5");
                itemUav5.setValue_unit("");
                itemUav5.setValue("");
                itemUav5.setUnit("");

                itemUav6.setType("6");
                itemUav6.setValue_unit("");
                itemUav6.setValue("");
                itemUav6.setUnit("");

                commandData.addItem(itemUav1);
                commandData.addItem(itemUav2);
                commandData.addItem(itemUav3);
                commandData.addItem(itemUav4);
                commandData.addItem(itemUav5);
                commandData.addItem(itemUav6);
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
    private UavHostEnvItem createCommonBeanUav(UavDeviceEntity entity) {
        UavHostEnvItem item = new UavHostEnvItem();
        item.setPatroldevice_code(entity.getDeviceSn());
        item.setPatroldevice_name(entity.getDeviceName());
        //time
        item.setTime(DateUtils.getNowDateTimeStr());
        return item;
    }
}
