package com.dji.sample.center.v2022.runnable;

import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.base.PatrolHostCommandItem;
import com.dji.sample.center.v2022.command.system.CenterRegisterItem;
import com.dji.sample.center.v2022.data.IntervalProtocolData;
import lombok.extern.slf4j.Slf4j;

/**
 * 给巡视上级间隔发送心跳处理线程
 *
 * @author lyc
 * @Time 2022/3/26 15:26
 */
@Slf4j
public class PatrolHostHeartRunnable extends IntervalBaseRunnable {

    public PatrolHostHeartRunnable(IntervalProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                //发送心跳指令
                intervalHeartBeat();
                //收到上级的心跳指令后，先发送一次，再去间隔发送
                Thread.sleep(intervalSeconds * 1000);
            } catch (Exception e) {
                log.error("【发送心跳给巡视上级】执行间隔上报异常：{}", e.getMessage());
            }
        }
    }

    /**
     * 发送心跳指令
     */
    private void intervalHeartBeat() {
        try {
            PatrolHostCommand commandData = new PatrolHostCommand();
            commandData.setSendCode(patrolHostCode);
            commandData.setReceiveCode(centerCode);
            commandData.setType("251");
            commandData.setCommand("2");
            commandData.setCode("");
            commandData.setTime(DateUtils.getNowDateTimeStr());
            commandData.setItems(new PatrolHostCommandItem());
            log.info("【发送心跳给巡视上级】心跳指令 ===>");
            boolean sendFlag = patrolHostSocketClient.sendCommand(commandData, CenterRegisterItem.class);
            if (!sendFlag) {
               this.secondConnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.secondConnect();
        }
    }

    /**
     * 发送心跳失败时从新连接巡视上级
     */
    private void secondConnect() {
        log.error("【发送心跳给巡视上级】发送失败，重新发送注册指令");
        this.setStop(true);
        patrolHostSocketClient.connect();
    }
}
