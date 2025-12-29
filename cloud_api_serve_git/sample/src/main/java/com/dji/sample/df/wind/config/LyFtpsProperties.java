package com.dji.sample.df.wind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lyftps")
public class LyFtpsProperties {

    /**
     * FTP服务器地址
     */
    private String ftpIp;

    /**
     * FTP端口号
     */
    private Integer ftpPort;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 隐式模式 (true=隐式, false=显式)
     */
    private Boolean implicit;
}
