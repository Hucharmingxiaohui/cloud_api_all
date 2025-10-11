package com.dji.sample.center.utils.ftp;

import lombok.Data;
import org.springframework.stereotype.Component;

//FTPS相关配置
@Data
@Component
public class FtpsCommonConfig {

    private String ftpIp;
    private Integer ftpPort;
    private String userName;
    private String password;
    private Boolean implicit;
}
