package com.dji.sample.df.windDf.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fjfile")
public class FjFileConfig {

    private String filePictrueUrl;
    private String fileReportPath;
    private String recfilePath;
    private String recfileNativePath;
}
