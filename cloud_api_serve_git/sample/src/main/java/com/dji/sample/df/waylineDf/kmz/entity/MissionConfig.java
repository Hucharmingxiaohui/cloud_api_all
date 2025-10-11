package com.dji.sample.df.waylineDf.kmz.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MissionConfig {
    @JsonProperty("fileName")
    private String fileName;//航线名称
    @JsonProperty("finishAction")
    private String finishAction;//完成航线动作
    @JsonProperty("takeOffSecurityHeight")
    private float takeOffSecurityHeight;//安全起飞高度
    @JsonProperty("globalTransitionalSpeed")
    private float globalTransitionalSpeed;//起飞速度，飞向首航点速度
    @JsonProperty("droneInfo")
    private DroneInfo droneInfo;//
    @JsonProperty("payloadInfo")
    private PayloadInfo payloadInfo;

}

