package com.dji.sample.center.v2022.runnable;

import com.dji.sample.center.v2022.data.IntervalProtocolData;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import lombok.extern.slf4j.Slf4j;

/**
 * 间隔上报线程基类
 *
 * @author
 * @Time 2022/3/26 15:15
 */
@Slf4j
public class IntervalBaseRunnable implements Runnable {
    public int intervalSeconds;
    public PatrolHostSocketClient patrolHostSocketClient;
    public String patrolHostCode;
    public String centerCode;
    public String patrolDeviceName;
    public String subCode;
    public Boolean stop;//线程开关，只要心跳断了，所有间隔都不上报了

    public IntervalBaseRunnable(IntervalProtocolData protocolData) {
        this.stop = false;
        this.intervalSeconds = protocolData.getIntervalSeconds();
        this.patrolHostSocketClient = protocolData.getPatrolHostSocketClient();
        this.patrolHostCode = protocolData.getPatrolHostCode();
        this.centerCode = protocolData.getCenterCode();
        this.patrolDeviceName = "巡视主机"; //巡视主机名称
    }


    @Override
    public void run() {
    }

    public Boolean getStop() {
        return stop;
    }

    public void setStop(Boolean stop) {
        this.stop = stop;
    }

    /**
     * 获取字符串中的数字
     *
     * @param valueUnit
     * @return
     */
    protected String getValue(String valueUnit) {
        try {
            String patternUnit = "([^0-9|\\.])+";
            String value = valueUnit.split(patternUnit)[0];
            return value;
        } catch (Exception e) {
            log.error("getValue error: {}", valueUnit);
            return "";
        }
    }

    /**
     * 获取字符串中的单位
     *
     * @param valueUnit
     * @return
     */
    protected String getUnit(String valueUnit) {
        try {
            String patternValue = "([^0-9|\\.\\-\\+])+";
            String strs[] = valueUnit.split(patternValue);
            String unit = strs.length > 1 ? strs[1] : "";
            return unit;
        } catch (Exception e) {
            log.error("getUnit error: {}", valueUnit);
            return "";
        }
    }
}
