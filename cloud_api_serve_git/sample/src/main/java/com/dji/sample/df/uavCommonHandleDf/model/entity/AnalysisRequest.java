package com.dji.sample.df.uavCommonHandleDf.model.entity;

import lombok.Data;

import java.util.List;
@Data
public class AnalysisRequest {
    private String function;
    private String file_path;
    private List<String> file_name;
}
