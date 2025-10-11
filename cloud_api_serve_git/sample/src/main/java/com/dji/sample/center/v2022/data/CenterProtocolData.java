package com.dji.sample.center.v2022.data;


import com.dji.sample.center.v2022.handler.CenterHandler;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import lombok.Data;

import java.net.Socket;

/**
 * 无人机系统与上级TCP通信相关数据
 *
 * @author lyc
 * @date 2022/4/1
 */
@Data
public class CenterProtocolData {
    //巡视主机客户端连接类实例
    private PatrolHostSocketClient patrolHostSocketClient;
    //中心端报文处理类实例
    private CenterHandler centerHandler;
    //本次连接socket
    private Socket socket;
    //上级IP地址
    private String centerIp;
    //巡视主机编码
    private String patrolHostCode;
    //上级系统编码
    private String centerCode;
    //xml报文
    private String xmlMessage;
    //请求报文序列号
    private long requestSerialNum;
    //响应报文序号
    private long responseSerialNum;
}
