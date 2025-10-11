package com.dji.sample.center.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

//巡视主机与集中监控系统TCP通信配置 - 2022国网规范
@Data
@Component
@ConfigurationProperties(prefix = "centernormal")
public class CenterNormalConfig {

    /** 集中监控系统IP */
    private String serverIp;
    /** 集中监控系统端口 */
    private Integer serverPort;
    /** 巡视主机唯一标识，对应无人机系统给上级中心端发送报文中的SendCode */
    private String stationCode;
    /** 上级系统唯一标识，对应无人机系统给上级中心端发送报文中的ReceiveCode */
    private String serverCode;
}
