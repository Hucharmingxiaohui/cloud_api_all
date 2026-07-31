package com.dji.sample.df.cqDockDf.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CqTaskIdRequest {

    @JsonAlias({"taskId", "task_id"})
    private String taskId;
}
