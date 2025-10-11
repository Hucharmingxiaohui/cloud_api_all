package com.df.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

//视频服务相关配置
@Data
@Component
@ConfigurationProperties(prefix = "video")
public class VideoConfig {

    private String sipserverPath;
    private String sipserverTcpPath;
}
