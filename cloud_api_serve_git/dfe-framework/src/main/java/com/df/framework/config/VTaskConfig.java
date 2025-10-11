package com.df.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 视频巡视任务抓拍分析配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "vtask")
public class VTaskConfig {

    private boolean enabled;
    /**
     * 请求抓拍图片接口地址
     */
    //private String captureUrl;
    /**
     * 标定图片获取地址
     */
    // private String resourceImageUrl;
    /**
     * 请求智能分析接口地址
     */
    private String analyseUrl;
    /**
     * 本地部署IP（配置的IP要求分析服务能访问到）
     */
    private String localIp;
    /**
     * 单个站最大允许抓拍数量
     */
    //private Integer maxCaptureNum;
    /**
     * captureServerMaxNum
     */
    //private Integer captureServerMaxNum;
}
