package com.dji.sample.df.cqDockDf.model.dto;

import lombok.Data;

@Data
public class CqEuaPlanPageRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String planName;
    private String bayName;
}
