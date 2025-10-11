package com.dji.sample.df.thirdKmzDf.entity.wayline.MissionConfig.DroneInfo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DroneInfo {
    @JsonProperty("droneEnumValue")
    private int droneEnumValue;//无人机型号
    @JsonProperty("droneSubEnumValue")
    private int droneSubEnumValue;//无人机子型号
}
