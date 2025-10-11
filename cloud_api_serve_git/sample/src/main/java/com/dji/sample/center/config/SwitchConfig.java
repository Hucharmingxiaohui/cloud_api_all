package com.dji.sample.center.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

//系统开关类配置
@Data
@Component
@ConfigurationProperties(prefix = "switch")
public class SwitchConfig {

    /**
     * 是否开启与中心端通讯 - 国网规范
     */
    private String centerNormalTcpEnable;

    /**
     * 是否开启与中心端通讯 - 第三方规范
     */
    private String centerOtherTcpEnable;

    /**
     * 是否开启与中心端通讯日志打印 - 国网规范
     */
    private String centerNormalTcpMsgEnable;

    /**
     * 是否开启与中心端通讯日志打印 - 第三方规范
     */
    private String centerOtherTcpMsgEnable;

}
