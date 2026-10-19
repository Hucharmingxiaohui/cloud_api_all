package com.dji.sample.df.cqDockDf.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CqEuaPlanExecuteRequest {
    @JsonAlias({"plan_id", "id"})
    private Long planId;
}
