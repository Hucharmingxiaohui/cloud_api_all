package com.dji.sample.df.cqDockDf.model.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CqDockReadQuery {
    private long current = 1;
    private long size = 20;
    private String unitCode;
    private String dockSn;
    private String droneSn;
    private String keyword;
    private Integer level;
    private String gatewayType;
    private Integer module;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
