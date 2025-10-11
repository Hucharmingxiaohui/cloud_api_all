package com.dji.sample.df.waylineDf.kmz.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DroneInfo {
    @JsonProperty("droneEnumValue")
    private int droneEnumValue;//无人机型号
    @JsonProperty("droneSubEnumValue")
    private int droneSubEnumValue;//无人机子型号

}

