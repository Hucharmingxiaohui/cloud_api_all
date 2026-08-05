package com.dji.sample.df.windDf.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wayline")
public class WaylineUrlConfig {

    private BuildKmzUrl buildKmzUrl;
    private String waylineStateUrl;
    private String analysisUrl;
    private String gfAnalysisUrl;
    private String detectAreaGenUrl;
    private String startSegmentationUrl;
    private String gfDefectLocalizationUrl;

    @Data
    public static class BuildKmzUrl {
        private String topWayline;
        private String singleWayline;
        private String workingWayline;
        private String stopWayline;
        private String interestPointWayline;
        private String solarPanelWayline;
    }
}
