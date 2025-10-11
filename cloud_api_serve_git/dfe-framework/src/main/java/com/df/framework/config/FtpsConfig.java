package com.df.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

//巡视主机的FTPS相关配置
@Data
@Component
@ConfigurationProperties(prefix = "ftps")
public class FtpsConfig {
    private String ftpIp;
    private Integer ftpPort;
    private String userName;
    private String password;
    private Boolean implicit;
}
