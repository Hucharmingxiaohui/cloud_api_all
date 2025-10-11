package com.dji.sample.center.v2022.handler;

import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.config.SwitchConfig;
import com.dji.sample.center.thread.ExecutorFactory;
import com.dji.sample.center.v2022.command.base.EdgeNodeCommandSimple;
import com.dji.sample.center.v2022.data.CenterProtocolData;
import com.dji.sample.center.v2022.runnable.*;
import com.dji.sample.center.v2022.tool.PatrolHostXmlTool;
import lombok.extern.slf4j.Slf4j;

import java.net.Socket;
import java.util.concurrent.ExecutorService;

/**
 * 无人机系统与巡视上级通信消息处理器
 */
@Slf4j
public class CenterMessageHandler extends CenterTcpMessageHandler {
    //成员变量
    private Socket socket;
    private String centerIp;
    private CenterHandler centerHandler;
    private PatrolHostSocketClient patrolHostSocketClient;

    //引用
    private SwitchConfig switchConfig = AppContext.getBean(SwitchConfig.class);
    private ExecutorFactory executorFactory = AppContext.getBean(ExecutorFactory.class);

    //构造方法
    public CenterMessageHandler(PatrolHostSocketClient patrolHostSocketClient, CenterHandler centerHandler, Socket socket, String centerIp) {
        this.patrolHostSocketClient = patrolHostSocketClient;
        this.centerHandler = centerHandler;
        this.socket = socket;
        this.centerIp = centerIp;
    }

    /**
     * 集中处理分发XML报文
     */
    public void processXmlMessage(String xmlMessage, long requestSerialNum, long responseSerialNum) {
        //报文序列化为简单格式实体类
        EdgeNodeCommandSimple commandSimple = PatrolHostXmlTool.deserialize(xmlMessage);
        String type = commandSimple.getType();
        String command = commandSimple.getCommand();
        String code = commandSimple.getCode();
        String sendCode = commandSimple.getSendCode();
        String receiveCode = commandSimple.getReceiveCode();

        //XML报文打印到日志
        if ("true".equals(switchConfig.getCenterNormalTcpMsgEnable())) {
            this.printCenterXML(xmlMessage, commandSimple, requestSerialNum, responseSerialNum);
        }
        /*try {
            PatrolHostSocketClient.SEND_UNM_MAP.remove(responseSerialNum);
        } catch (Exception e) {
            log.error("", e);
        }*/

        // 组织传递给Runnable的参数
        CenterProtocolData protocolData = new CenterProtocolData();
        protocolData.setCenterCode(sendCode);
        protocolData.setPatrolHostCode(receiveCode);
        protocolData.setPatrolHostSocketClient(this.patrolHostSocketClient);
        protocolData.setCenterHandler(this.centerHandler);
        protocolData.setSocket(this.socket);
        protocolData.setCenterIp(this.centerIp);
        protocolData.setXmlMessage(xmlMessage);
        protocolData.setRequestSerialNum(requestSerialNum);
        protocolData.setResponseSerialNum(responseSerialNum);

        ExecutorService executorService = executorFactory.getExecutorService();
        switch (type) {
            //系统类型消息
            case "251":
                switch (command) {
                    //注册响应
                    case "4":
                        executorService.submit(new CenterRegisterRunnable(protocolData));
                    default:
                        break;
                }
                break;

            //无人机本体控制指令
            case "20001":
                executorService.submit(new CenterUavControlRunnable(protocolData));
                break;

            //无人机飞行控制指令
            case "20002":
                executorService.submit(new CenterUavControlRunnable(protocolData));
                break;

            //无人机云台控制指令
            case "20003":
                executorService.submit(new CenterUavControlRunnable(protocolData));
                break;

            //无人机相机控制指令
            case "20004":
                executorService.submit(new CenterUavControlRunnable(protocolData));
                break;

            //无人机机巢控制指令
            case "20005":
                executorService.submit(new CenterUavControlRunnable(protocolData));
                break;

            //任务控制指令
            case "41":
                executorService.submit(new CenterTaskControlRunnable(protocolData));
                break;

            //任务下发指令
            case "101":
                executorService.submit(new CenterTaskRunnable(protocolData));
                break;

            //检修区域下发指令
            case "81":
                executorService.submit(new CenterOverhaulAreaRunnable(protocolData));
                break;

            //模型同步指令
            case "61":
                executorService.submit(new CenterModelSyncRunnable(protocolData));
                break;

            default:
                break;
        }
    }

    /**
     * 开发测试用日志打印
     */
    private void printCenterXML(String xmlMessage, EdgeNodeCommandSimple commandSimple, long requestSerialNum, long responseSerialNum) {
        String type = commandSimple.getType();
        String command = commandSimple.getCommand();
        String sendCode = commandSimple.getSendCode();
        String logInfo = String.format("巡视上级SendCode：%s，IP：%s，报文序号【%s】【%s】\n%s", sendCode, this.centerIp, requestSerialNum, responseSerialNum, xmlMessage);

        switch (type) {
            //系统类型消息
            case "251":
                switch (command) {
                    //注册指令
                    case "1":
                        log.info("无人机系统收到上级系统【注册响应】{}", logInfo);
                        break;
                    //心跳指令
                    case "2":
                        log.info("无人机系统收到上级系统【心跳响应】{}", logInfo);
                        break;
                    //响应命令
                    case "3":
                        log.info("无人机系统收到上级系统【响应消息】{}", logInfo);
                        break;
                    default:
                        log.info("无人机系统收到上级系统【其他响应消息】{}", logInfo);
                        break;
                }
                break;
            case "20001":
                switch (command) {
                    case "1":
                        log.info("无人机系统收到上级系统【无人机保留】{}", logInfo);
                        break;
                    case "2":
                        log.info("无人机系统收到上级系统【无人机系统自检】{}", logInfo);
                        break;
                    case "3":
                        log.info("无人机系统收到上级系统【无人机一键返航】{}", logInfo);
                        break;
                    case "4":
                        log.info("无人机系统收到上级系统【无人机自动降落】{}", logInfo);
                        break;
                    case "5":
                        log.info("无人机系统收到上级系统【无人机控制模式切换】{}", logInfo);
                        break;
                    case "6":
                        log.info("无人机系统收到上级系统【无人机控制权获得】{}", logInfo);
                        break;
                    case "7":
                        log.info("无人机系统收到上级系统【无人机控制权释放】{}", logInfo);
                        break;
                    case "8":
                        log.info("无人机系统收到上级系统【无人机电源管理】{}", logInfo);
                        break;
                    default:
                        log.info("【上级系统无人机监视类消息-无人机机巢状态数据】{}", logInfo);
                        break;
                }
                break;
            case "20002":
                switch (command) {
                    case "1":
                        log.info("无人机系统收到上级系统【无人机飞行控制保留】{}", logInfo);
                        break;
                    case "2":
                        log.info("无人机系统收到上级系统【无人机飞行控制保留】{}", logInfo);
                        break;
                    case "3":
                        log.info("无人机系统收到上级系统【无人机飞行控制保留】{}", logInfo);
                        break;
                    case "4":
                        log.info("无人机系统收到上级系统【无人机飞行控制保留】{}", logInfo);
                        break;
                    case "5":
                        log.info("无人机系统收到上级系统【无人机飞行控制保留】{}", logInfo);
                        break;
                    case "6":
                        log.info("无人机系统收到上级系统【无人机飞行控制保留】{}", logInfo);
                        break;
                    case "7":
                        log.info("无人机系统收到上级系统【无人机飞行控制急停】{}", logInfo);
                        break;
                }
                break;
            case "20003":
                switch (command) {
                    case "1":
                        log.info("无人机系统收到上级系统【无人机云台保留】{}", logInfo);
                        break;
                    case "2":
                        log.info("无人机系统收到上级系统【无人机云台保留】{}", logInfo);
                        break;
                    case "3":
                        log.info("无人机系统收到上级系统【无人机云台保留】{}", logInfo);
                        break;
                    case "4":
                        log.info("无人机系统收到上级系统【无人机云台保留】{}", logInfo);
                        break;
                    case "5":
                        log.info("无人机系统收到上级系统【无人机云台重置】{}", logInfo);
                        break;
                    case "6":
                        log.info("无人机系统收到上级系统【无人机云台云台俯仰角】{}", logInfo);
                        break;
                }
                break;
            case "20004":
                switch (command) {
                    case "1":
                        log.info("无人机系统收到上级系统【无人机相机设置图传模式】{}", logInfo);
                        break;
                }
                break;
            case "20005":
                switch (command) {
                    case "1":
                        log.info("无人机系统收到上级系统【无人机机巢机巢】{}", logInfo);
                        break;
                    case "2":
                        log.info("无人机系统收到上级系统【无人机机巢急停】{}", logInfo);
                        break;
                    case "3":
                        log.info("无人机系统收到上级系统【无人机机巢舱门】{}", logInfo);
                        break;
                }
            case "41":
                switch (command) {
                    case "1":
                        log.info("无人机系统收到上级系统【任务控制-任务启动】{}", logInfo);
                        break;
                    case "2":
                        log.info("无人机系统收到上级系统【任务控制-任务暂停】{}", logInfo);
                        break;
                    case "3":
                        log.info("无人机系统收到上级系统【任务控制-任务继续】{}", logInfo);
                        break;
                    case "4":
                        log.info("无人机系统收到上级系统【任务控制-任务停止】{}", logInfo);
                        break;
                    default:
                        log.info("无人机系统收到上级系统【监视类消息-任务状态数据】{}", logInfo);
                        break;
                }
                break;
            case "101":
                switch (command) {
                    case "1":
                        log.info("无人机系统收到上级系统【任务配置-任务下发】{}", logInfo);
                        break;
                }
                break;
            case "81":
                switch (command) {
                    case "4":
                        log.info("无人机系统收到上级系统【检修区域下发】{}", logInfo);
                        break;
                }
                break;
            case "61":
                switch (command) {
                    case "1":
                        log.info("无人机系统收到上级系统【模型同步指令-无人机巡视系统模型】{}", logInfo);
                        break;
                }
                break;
            default:
                log.info("无人机系统收到上级系统【消息】{}", logInfo);
                break;
        }
    }
}
