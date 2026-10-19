package com.dji.sample.df.cqDockDf.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class CqEuaTaskPictureListRequest {
    @JsonAlias({"business_id", "taskCode", "task_code"})
    private String businessId;
    @JsonAlias({"eua_task_id", "taskId", "task_id"})
    private String euaTaskId;
}
