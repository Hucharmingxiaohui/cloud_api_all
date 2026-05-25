package com.dji.sample.df.indoor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "indoor-uav")  // 注意：prefix 使用 kebab-case 匹配配置项
@Data
public class IndoorUavConfig {
    private String startTask;      // 对应 indoor-uav.start-task
    private String taskStatus;     // 对应 indoor-uav.task-status
}
