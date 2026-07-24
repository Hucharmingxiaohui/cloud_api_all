package com.dji.sample.df.cqDockDf.model.dto;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class CqApiResponse {

    private Integer code;

    private String msg;

    private Boolean success;

    private JSONObject data;

    @JsonIgnore
    @JSONField(serialize = false)
    private String rawBody;
}
