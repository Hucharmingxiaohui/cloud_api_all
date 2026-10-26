package com.dji.sample.df.cqDockDf.model.dto;

import lombok.Data;

@Data
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class CqEuaPlanPageRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String planName;
    private String routeName;
    private String bayName;
}
