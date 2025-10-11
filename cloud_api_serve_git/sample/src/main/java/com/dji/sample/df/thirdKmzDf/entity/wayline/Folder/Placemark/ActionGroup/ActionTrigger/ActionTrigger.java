package com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.ActionTrigger;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ActionTrigger {
    @JsonProperty("actionTriggerType")//动作触发类型
    private String actionTriggerType="reachPoint";
    @JsonProperty("actionTriggerParam")//动作触发参数
    private float actionTriggerParam=3;
}
