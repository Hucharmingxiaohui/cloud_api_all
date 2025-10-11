package com.dji.sample.center.v2022.handler;

import com.dji.sample.center.config.CenterNormalConfig;
import com.dji.sample.center.config.SwitchConfig;
import com.dji.sample.center.thread.ExecutorFactory;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.StringUtil;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.base.PatrolHostCommandItem;
import com.dji.sample.center.v2022.tool.CenterPacket;
import com.dji.sample.center.v2022.tool.CenterSNTool;
import com.dji.sample.center.v2022.tool.CenterXmlTool;
import com.dji.sample.center.v2022.tool.EmptyItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 巡视主机作为客户端，与中心端建立通讯以及发送报文处理类
 */
@Slf4j
@Component
public class PatrolHostSocketClient implements Runnable {
    @Autowired
    private SwitchConfig switchConfig;
    @Autowired
    private CenterNormalConfig centerConfig;
    @Autowired
    private ExecutorFactory executorFactory;

    //成员变量
    private Socket socket;
    private String centerIp;
    private CenterHandler centerHandler;

    public static ConcurrentHashMap<String, com.dji.sample.center.v2022.runnable.IntervalBaseRunnable>
            V2022PatrolHostRunnableBucket = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<Long, String> SEND_UNM_MAP = new ConcurrentHashMap<>();


    @Override
    public void run() {
        connect();
    }

    public Socket getSocket() {
        return socket;
    }

    //作为客户端连接上级
    public void connect() {
        try {
            // 如果给上级发送指令，超过20次，主站没回消息，
            if (SEND_UNM_MAP.size() > 0) {
                SEND_UNM_MAP.clear();
            }
            stopBucketRunnable();
            if (this.socket != null) {
                if (this.centerHandler != null) {
                    this.centerHandler.setStop(true);
                    this.centerHandler = null;
                }
                try {
                    this.socket.close();
                } catch (IOException e) {
                    log.info("【无人机系统向巡视上级注册】原socket关闭失败->废弃其置为null，继续重连，{} {}", e.getMessage(), e);
                }
                this.socket = null;
            }
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(centerConfig.getServerIp(), centerConfig.getServerPort()), 10000);
            this.socket.setSoTimeout(10000);
            this.centerIp = this.socket.getInetAddress().getHostAddress();
            this.centerHandler = new CenterHandler(this, this.socket);
            this.executorFactory.getExecutorService().submit(this.centerHandler);

            log.info("【无人机系统向巡视上级注册】上级系统IP:" + this.centerIp);
            PatrolHostCommand commandData = this.getBaseCommand("251", "1", null);
            boolean sendFlag = this.sendCommand(commandData, EmptyItem.class);
            if (!sendFlag) {
                log.info("【无人机系统向巡视上级注册】失败，准备尝试再次连接");
                connect();
            }
        } catch (Exception e) {
            log.error("【socket连接主站失败, 间隔10秒再重新连接】 {}, {}", e.getMessage(), e);
            try {
                Thread.sleep(10 * 1000L);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            //注释掉日志，防止一直打印
            //log.info("【无人机系统向巡视上级注册】失败，准备尝试再次连接");
            connect();
        }
    }

    private void stopBucketRunnable() {
        if (V2022PatrolHostRunnableBucket.size() > 0) {
            Iterator<Map.Entry<String, com.dji.sample.center.v2022.runnable.IntervalBaseRunnable>>
                    iterator = V2022PatrolHostRunnableBucket.entrySet().iterator();
            while (iterator.hasNext()) {
                try {
                    iterator.next().getValue().setStop(true);
                    iterator.remove();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    //---------------------------发送指令处理方法---------------------------

    /**
     * 生成发送报文的基础Command
     */
    public PatrolHostCommand getBaseCommand(String type, String command, String code) {
        PatrolHostCommand commandData = new PatrolHostCommand();
        commandData.setSendCode(centerConfig.getStationCode());
        commandData.setReceiveCode(centerConfig.getServerCode());
        commandData.setType(type);
        commandData.setCommand(command);
        commandData.setCode(code);
        commandData.setTime(DateUtils.getNowDateTimeStr());
        commandData.setItems(new PatrolHostCommandItem());
        return commandData;
    }

    /**
     * 发送指令给上级
     *
     * @param commandData
     * @param cls         注意：不能使用此函数发送响应报文，切响应报文必须使用responsePacket
     *                    因为二者setSessionType时值不同，Ox00 标识会话请求，0x01 标识会话响应，其它异常会话。
     */
    public boolean sendCommand(PatrolHostCommand commandData, Class<?> cls) {
        try {
            this.checkCommand(commandData);
            String xmlData = CenterXmlTool.serialize(commandData, cls);
            boolean result = sendPacket(xmlData);
            return result;
        } catch (Exception e) {
            log.error("【无人机系统发送命令】发送失败:{} {} ", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送报文
     * 注意，不能使用此函数发送响应报文，切响应报文必须使用responsePacket
     * 因为二者setSessionType时值不同，Ox00 标识会话请求，0x01 标识会话响应，其它异常会话。
     *
     * @Synchronized 保证同一时间只能有同一线程调用它，防止多线程下SN计数bug。
     */
    private synchronized boolean sendPacket(String xmlData) {
        //自增一个发送序号，赋值给serialNumber
        CenterSNTool centerSNTool = CenterSNTool.getInstance();
        long sendSN = centerSNTool.increaseSN();
        try {
            byte[] data = xmlData.getBytes("utf-8");
            CenterPacket packet = new CenterPacket();
            packet.serialNumber = sendSN;
            packet.responseSerialNumber = 0;    //主动发起，故无响应序列号
            packet.setSessionType((byte) 0);
            packet.setData(data);
            packet.setDataLength(data.length);
            OutputStream outputStream = this.socket.getOutputStream();
            outputStream.write(packet.getBytes());
            outputStream.flush();
            if ("true".equals(switchConfig.getCenterNormalTcpMsgEnable())) {
                log.info("【无人机系统发送命令】上级IP：{}，报文序号【{}】【{}】\n{}", this.centerIp, packet.serialNumber, packet.responseSerialNumber, xmlData);
            }
//            SEND_UNM_MAP.put(sendSN, "");
            if (SEND_UNM_MAP.size() > 0) {
                SEND_UNM_MAP.clear();
            }
            return true;
        } catch (SocketException socketException) {
            log.error("【无人机系统发送命令】发送命令失败, socket连接异常：{} {} ", socketException.getMessage(), socketException);
            SEND_UNM_MAP.put(sendSN, "");
            // 如果给上级发送指令，超过20次，主站没回消息，重新建立连接
            if (SEND_UNM_MAP.size() > 20) {
                log.error("【无人机系统发送命令】发送命令失败, socket连接异常，超过20次，开始重连：{} {} ", socketException.getMessage(), socketException);
                connect();
            }
            return false;
        } catch (Exception e) {
            log.error("【无人机系统发送命令】发送命令失败：{} {} ", e.getMessage(), e);
            return false;
        }
    }


    //---------------------------发送指令处理方法End---------------------------
    //---------------------------响应指令处理方法---------------------------

    /**
     * 响应命令给上级
     *
     * @param commandData
     * @param cls
     * @param serialNumber
     * @return
     */
    public boolean responseCommand(PatrolHostCommand commandData, Class<?> cls, long serialNumber) {
        try {
            this.checkCommand(commandData);
            String xmlData = CenterXmlTool.serialize(commandData, cls);
            boolean result = responsePacket(xmlData, serialNumber);
            return result;
        } catch (Exception e) {
            log.error("【无人机系统发送响应】发送失败:{} {} ", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 响应报文
     *
     * @param xmlData
     * @param serialNumber
     * @return
     */
    private synchronized boolean responsePacket(String xmlData, long serialNumber) {
        try {
            byte[] data = xmlData.getBytes(StandardCharsets.UTF_8);
            CenterPacket packet = new CenterPacket();
            packet.serialNumber = 0;
            packet.responseSerialNumber = serialNumber;
            packet.setSessionType((byte) 1);
            packet.setData(data);
            packet.setDataLength(data.length);
            this.socket.setSoTimeout(5000);
            OutputStream outputStream = this.socket.getOutputStream();
            outputStream.write(packet.getBytes());
            outputStream.flush();
            if ("true".equals(switchConfig.getCenterNormalTcpMsgEnable())) {
                log.info("【无人机系统发送响应】上级IP：{}，报文序号【{}】【{}】\n{}", this.centerIp, packet.serialNumber, packet.responseSerialNumber, xmlData);
            }
            return true;
        } catch (Exception e) {
            log.error("【无人机系统发送响应】响应命令失败：{} {} ", e.getMessage(), e);
            return false;
        }
    }

    public boolean testSendPacket() {
        try {
            OutputStream outputStream = this.socket.getOutputStream();
            outputStream.write("test".getBytes());
            outputStream.flush();
            return true;
        } catch (Exception e) {
            log.error("【无人机系统发送测试】失败：{} {} ", e.getMessage(), e);
            return false;
        }
    }

    public boolean checkSendNum() {
        if(SEND_UNM_MAP.size() > 100){
            log.info("【未收到上级响应序号】 {}",SEND_UNM_MAP.keySet());
            SEND_UNM_MAP.clear();
            return false;
        }
        return true;
    }

    //---------------------------响应指令处理方法End---------------------------

    /**
     * 发送前检查指令
     *
     * @param commandData
     */
    private void checkCommand(PatrolHostCommand commandData) {
        if (StringUtil.isEmpty(commandData.getCode())) {
            commandData.setCode("");
        }
        if (StringUtil.isEmpty(commandData.getCommand())) {
            commandData.setCommand("");
        }
        if (StringUtil.isEmpty(commandData.getTime())) {
            commandData.setTime(DateUtils.getNowDateTimeStr());
        }
    }

}


