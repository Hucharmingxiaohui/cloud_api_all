package com.dji.sample.df.cqDockDf.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cq-dock")
public class CqDockProperties {

    /**
     * 下级无人机平台 HTTP 根地址，例如 http://ip:port
     */
    private String baseUrl = "";

    private String accessKey = "";

    private String accessSecret = "";

    private String assignTaskPath = "/machineNest/noauth/third/task/assign_task";

    private String pictureListPath = "/machineNest/noauth/third/picture/list";

    private String taskStatusPath = "/machineNest/noauth/third/task/status";
}
