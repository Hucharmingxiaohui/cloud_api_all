package com.dji.sample.df.thirdKmzDf.entity.thirdPoints.thirdActions;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ThirdAction {
    @JsonProperty("actionActuatorFuncParam")
    private String actionActuatorFuncParam; //动作名称
}
