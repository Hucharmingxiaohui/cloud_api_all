package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.dji.sample.center.utils.SpringUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.UavNestDataItem;
import com.dji.sample.center.v2022.data.IntervalProtocolData;
import com.dji.sample.df.manageDf.dao.IUavDeviceMapper;
import com.dji.sample.df.manageDf.model.entity.UavDeviceEntity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 无人机机巢运行数据上送给巡视上级
 *
 * @author lyc
 * @Time 2022/3/26 15:14
 */
@Slf4j
public class UavNestRunRunnable extends IntervalBaseRunnable {

    private IUavDeviceMapper iUavDeviceMapper = SpringUtils.getBean(IUavDeviceMapper.class);

    public UavNestRunRunnable(IntervalProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                //发送机巢运行数据
                intervalNestRunData();
                Thread.sleep(intervalSeconds * 1000);
            } catch (Exception e) {
                log.error("【发送无人机机巢运行数据给中心端】执行间隔上报异常：{}", e.getMessage());
            }
        }
    }

    /**
     * 发送机巢运行数据
     *
     * @return
     */
    private void intervalNestRunData() {
        PatrolHostCommand commandData = new PatrolHostCommand();
        List<UavDeviceEntity> list = iUavDeviceMapper.selectByCondition(
                Wrappers.lambdaQuery(UavDeviceEntity.class)
                        .eq(UavDeviceEntity::getMainDeviceType, 3)
        );

        if (list == null) {
            list = new ArrayList<>();
        }

        for (UavDeviceEntity entity : list) {
            UavNestDataItem item1 = createCommonBean(entity);
            UavNestDataItem item2 = createCommonBean(entity);
            UavNestDataItem item3 = createCommonBean(entity);
            UavNestDataItem item4 = createCommonBean(entity);
            UavNestDataItem item5 = createCommonBean(entity);
            UavNestDataItem item6 = createCommonBean(entity);

            /*<1>: = 电池电量 <2>: = 电池使用状态 <3>: = 电池状态
            <4>: = 电池电压 <5>: = 舱内温度 <6>: = 舱内湿度*/

            //加Optional.ofNullable目的是：如果这个字段值是null，传递的xml报文里就没有这个字段了，不符合规范
            item1.setType("1");
            String valueUnit = "";
            item1.setValue_unit(valueUnit);
            item1.setValue("");
            item1.setUnit("");

            item2.setType("2");
            valueUnit = "";
            item2.setValue_unit(valueUnit);
            item2.setValue("");
            item2.setUnit("");

            item3.setType("3");
            valueUnit = "";
            item3.setValue_unit(valueUnit);
            item3.setValue("");
            item3.setUnit("");

            item4.setType("4");
            valueUnit = "";
            item4.setValue_unit(valueUnit);
            item4.setValue("");
            item4.setUnit("");

            item5.setType("5");
            valueUnit = "";
            item5.setValue_unit(valueUnit);
            item5.setValue("");
            item5.setUnit("");

            item6.setType("6");
            valueUnit = "";
            item6.setValue_unit(valueUnit);
            item6.setValue("");
            item6.setUnit("");

            commandData.addItem(item1);
            commandData.addItem(item2);
            commandData.addItem(item3);
            commandData.addItem(item4);
            commandData.addItem(item5);
            commandData.addItem(item6);
        }

        log.info("【发送无人机机巢运行数据给巡视上级】间隔发送 ========> ");
        commandData.setSendCode(patrolHostCode);
        commandData.setReceiveCode(centerCode);
        commandData.setType("10004");
        patrolHostSocketClient.sendCommand(commandData, UavNestDataItem.class);
    }

    /**
     * 公共数据处理
     *
     * @param entity
     * @return
     */
    private UavNestDataItem createCommonBean(UavDeviceEntity entity) {
        UavNestDataItem item = new UavNestDataItem();

        item.setNest_code(entity.getDeviceSn());
        item.setNest_name(entity.getDeviceName());
        item.setModule_no("");
        return item;
    }
}
