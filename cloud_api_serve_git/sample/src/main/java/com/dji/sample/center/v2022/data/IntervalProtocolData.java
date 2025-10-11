package com.dji.sample.center.v2022.data;


import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import lombok.Data;

/**
 * 间隔上报协议数据
 */
@Data
public class IntervalProtocolData {
    //间隔秒数
    private Integer intervalSeconds;
    //巡视主机客户端连接类实例
    private PatrolHostSocketClient patrolHostSocketClient;
    //上级IP地址
    private String centerIp;
    //巡视主机编码
    private String patrolHostCode;
    //上级系统编码
    private String centerCode;
}
