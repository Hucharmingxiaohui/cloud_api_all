package com.dji.sample.df.cqDockDf.model.dto;

import lombok.Data;

@Data
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class CqEuaPlanUpdateRequest {
    private Long id;
    private String planName;
    private String bayId;
    private java.util.List<Long> pointIds;
    private String remark;
    private Integer enabled;
}
