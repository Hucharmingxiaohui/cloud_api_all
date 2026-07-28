package com.dji.sample.df.cqDockDf.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CqPictureReportTestRequest {

    @JsonAlias({"taskCode", "task_code"})
    private String taskCode;

    @JsonAlias({"taskName", "task_name"})
    private String taskName;

    @JsonAlias({"euaTaskId", "eua_task_id"})
    private String euaTaskId;
}
