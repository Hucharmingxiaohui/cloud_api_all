package com.dji.sample.df.cqDockDf.model.dto;

import lombok.Data;

@Data
public class CqEuaTaskRecordPageRequest {
    private Integer page;
    private Integer pageSize;
    private Long planId;
    private String taskName;
    private String euaTaskId;
}
