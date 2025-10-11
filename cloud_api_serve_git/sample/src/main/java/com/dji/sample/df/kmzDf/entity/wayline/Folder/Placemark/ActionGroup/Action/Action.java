package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action;

import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.ActionActuatorFuncParam;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
//动作
@Data
public class Action {
    @JsonProperty("actionActuatorFunc")//动作类型
    private String actionActuatorFunc;
    @JsonProperty("actionActuatorFuncParam")
    private ActionActuatorFuncParam actionActuatorFuncParam;//动作参数
}
