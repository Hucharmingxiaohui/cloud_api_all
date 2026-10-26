package com.dji.sample.df.cqDockDf.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class CqEuaPlanExecuteRequest {
    @JsonAlias({"plan_id", "id"})
    private Long planId;
}
