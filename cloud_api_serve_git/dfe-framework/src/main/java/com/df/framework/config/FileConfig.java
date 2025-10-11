package com.df.framework.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

//本地文件相关配置
@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileConfig {
    private String fileUrl;
    private String fileSavePath;
    private String taskReportTempFile;
    private String taskReportMoreTempFile;
    private String taskReportExcelTempFile;
    private String taskReportExcelMoreTempFile;


    public String getFileVisitUrl(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return fileUrl + "ftl/default.png";
        }
        return fileUrl + filePath;
    }

    public String getFileLocalPath(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return "";
        }
        if (filePath.contains("images")) {
            filePath = filePath.substring(filePath.lastIndexOf("images") + 6);
        }
        return Paths.get(fileSavePath, filePath).toString();
    }


}
