package com.df.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "analyse.standard")
public class AnalyseStandardConfig {

    /**
     * true: 57号文标准; false: 一般标准.
     */
    private Boolean document57 = false;

    public boolean isDocument57() {
        return Boolean.TRUE.equals(document57);
    }
}
