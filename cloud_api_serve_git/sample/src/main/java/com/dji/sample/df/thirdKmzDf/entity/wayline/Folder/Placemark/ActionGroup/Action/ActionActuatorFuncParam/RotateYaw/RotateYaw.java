package com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.RotateYaw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RotateYaw {
    @JsonProperty("aircraftHeading")
    private float aircraftHeading=0;//飞行器目标偏航角（相对于地理北）
    @JsonProperty("aircraftPathMod")
    private String aircraftPathMod="counterClockwise";//飞行器偏航角转动模式
}
