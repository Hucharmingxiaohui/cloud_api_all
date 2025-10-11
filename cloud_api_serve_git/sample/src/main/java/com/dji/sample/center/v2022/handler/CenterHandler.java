package com.dji.sample.center.v2022.handler;

import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.config.CenterNormalConfig;
import com.dji.sample.center.utils.StringUtil;
import com.dji.sample.center.utils.TCPPackageUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 巡视主机与上级系统建立Socket通信的处理类
 */
@Slf4j
public class CenterHandler implements Runnable {
    private static final int HEADER_LEN = 23;

    //yxm 2022.11.22 根据对接上级的参数做定义
    private CenterNormalConfig centerConfig = AppContext.getBean(CenterNormalConfig.class);

    //成员变量
    private Boolean stop;
    private Socket socket;
    private String centerIp;
    private PatrolHostSocketClient patrolHostSocketClient;


    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    //构造方法
    public CenterHandler(PatrolHostSocketClient patrolHostSocketClient, Socket socket) throws SocketException {
        this.stop = false;
        this.patrolHostSocketClient = patrolHostSocketClient;
        this.socket = socket;
        this.socket.setSoTimeout(5000);
    }

    @SneakyThrows
    @Override
    public void run() {
        InputStream inputStream = null;
        CenterTcpMessageHandler centerMessageHandler = null;
        try {
            // 获取巡视主机ip
            this.centerIp = this.socket.getInetAddress().getHostAddress();
            //获取输入流
            inputStream = this.socket.getInputStream();
            //线程启用同时New巡视消息处理器
            centerMessageHandler = new CenterMessageHandler(this.patrolHostSocketClient, this, socket, centerIp);

        } catch (Exception e) {
            log.error("【巡视上级TCP通信处理】从socket中取流异常，巡视上级IP:{}，异常信息:{}", this.centerIp, e);
            return;
        }

        while (!this.stop) {
            byte[] headerBytes = new byte[HEADER_LEN];
            while (!this.stop && this.socket.isConnected()) {
                try {
                    //先判断是否有数据，如果直接inputStream.read，没有数据时会抛异常
                    if (inputStream.available() <= 0) {
                        Thread.sleep(50);
                        continue;
                    }

                    //读取23位报文放入bytes[]中
                    int len = inputStream.read(headerBytes, 0, HEADER_LEN);
                    if (len != HEADER_LEN) {
                        if (len <= 0) {
                            Thread.sleep(50);
                        }
                        continue;
                    }

                    //起始标志符,EB90(小头字节序)
                    ByteBuffer buffer = ByteBuffer.wrap(headerBytes, 0, 2);
                    byte[] head = buffer.array();
                    // -21为byte eb，-112为byte 90
                    if (head[0] != (byte) 0xeb || head[1] != (byte) 0x90) {
                        continue;
                    }

                    ///从header中读取如下信息
                    long requestSerialNum;
                    long responseSerialNum;
                    int sessionType;
                    int xmlLength;

                    //发送会话序列号,long(八字节小头字节序)
                    buffer = ByteBuffer.wrap(headerBytes, 2, 8);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    requestSerialNum = buffer.getLong();

                    //响应会话序列号,long(八字节小头字节序)
                    buffer = ByteBuffer.wrap(headerBytes, 10, 8);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    responseSerialNum = buffer.getLong();

                    //会话源标识,0x00（一个字节）
                    buffer = ByteBuffer.wrap(headerBytes, 18, 1);
                    sessionType = buffer.get();

                    //XML的字节长度,int（四字节小头字节序）
                    buffer = ByteBuffer.wrap(headerBytes, 19, 4);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    xmlLength = buffer.asIntBuffer().get();

                    if (xmlLength == 0) {
                        log.error("【巡视上级TCP通信处理】XML报文空数据，巡视上级IP:{}", this.centerIp);
                        continue;
                    }

                    String xmlStr = TCPPackageUtils.readXmlString(inputStream, xmlLength);
                    if (StringUtil.isEmpty(xmlStr)) {
                        log.error("【巡视上级TCP通信处理】XMLString空，巡视上级IP:{}", this.centerIp);
                    }

                    //校验结束符
                    if (TCPPackageUtils.readAndCheckEndFlag(inputStream) == false) {
                        continue;
                    }

                    //读到完整报文，进行处理
                    centerMessageHandler.processXmlMessage(xmlStr, requestSerialNum, responseSerialNum);
                } catch (Exception e) {
                    log.error("【巡视上级TCP通信处理】处理TCP报文发生异常，巡视上级IP:{}，异常信息：{}", this.centerIp, e);
                }
            }
        }
    }
}