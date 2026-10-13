package com.dji.sample.df.cqDockDf.model.dto;

import lombok.Data;

@Data
public class CqEuaPlanUpdateRequest {
    private Long id;
    private String planName;
    private String bayId;
    private String remark;
    private Integer enabled;
}
