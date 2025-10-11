package com.dji.sample.center.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

//中心端的FTPS相关配置 - 国网规范
@Data
@Component
@ConfigurationProperties(prefix = "centerftpsnormal")
public class CenterFtpsNormalConfig {

    private String ftpIp;
    private Integer ftpPort;
    private String userName;
    private String password;
    private Boolean implicit;
    private String fileSavePath;
}
