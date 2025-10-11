package com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.Hover;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Hover {
    @JsonProperty("hoverTime")
    private float hoverTime;//飞行器悬停等待时间
}

