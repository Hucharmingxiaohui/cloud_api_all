package com.dji.sample.center.v2022.runnable;

import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.config.SwitchConfig;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.data.CenterProtocolData;
import com.dji.sample.center.v2022.handler.CenterHandler;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;

import java.net.Socket;

/**
 * 无人机系统处理巡视上级消息线程基类
 *
 * @date 2022/4/1
 */
public class CenterMessageBaseRunnable implements Runnable {

    //成员变量
    public PatrolHostSocketClient patrolHostSocketClient;
    public CenterHandler centerHandler;
    public String centerIp;
    public Socket socket;
    public String centerCode;
    public String patrolHostCode;
    public String xmlMessage;
    public long requestSerialNum;
    public long responseSerialNum;

    //引用
    public SwitchConfig switchConfig = AppContext.getBean(SwitchConfig.class);

    public CenterMessageBaseRunnable(CenterProtocolData patrolHostProtocolData) {
        this.patrolHostSocketClient = patrolHostProtocolData.getPatrolHostSocketClient();
        this.centerHandler = patrolHostProtocolData.getCenterHandler();
        this.socket = patrolHostProtocolData.getSocket();
        this.centerIp = patrolHostProtocolData.getCenterIp();
        this.centerCode = patrolHostProtocolData.getCenterCode();
        this.patrolHostCode = patrolHostProtocolData.getPatrolHostCode();
        this.xmlMessage = patrolHostProtocolData.getXmlMessage();
        this.requestSerialNum = patrolHostProtocolData.getRequestSerialNum();
        this.responseSerialNum = patrolHostProtocolData.getResponseSerialNum();
    }

    @Override
    public void run() {
    }

    /**
     * 响应消息给中心端正常
     */
    public void responseMessage() {
        PatrolHostCommand responseCommand = patrolHostSocketClient.getBaseCommand("251", "3", "200");
        patrolHostSocketClient.responseCommand(responseCommand, PatrolHostCommand.class, requestSerialNum);
    }

    /**
     * 响应消息给中心端正常 - 4响应
     */
    public void responseMessageOtherCommand() {
        PatrolHostCommand responseCommand = patrolHostSocketClient.getBaseCommand("251", "4", "200");
        patrolHostSocketClient.responseCommand(responseCommand, PatrolHostCommand.class, requestSerialNum);
    }

    /**
     * 响应消息给中心端异常
     */
    public void responseExceptionMessage() {
        PatrolHostCommand responseCommand = patrolHostSocketClient.getBaseCommand("251", "3", "500");
        patrolHostSocketClient.responseCommand(responseCommand, PatrolHostCommand.class, requestSerialNum);
    }
}
