package com.dji.sample.df.waylineDf.kmz.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PayloadInfo {
    @JsonProperty("payloadEnumValue")
    private int payloadEnumValue;//负载型号
    @JsonProperty("payloadSubEnumValue")
    private int payloadSubEnumValue;//负载子型号
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex;//挂载位置

}
