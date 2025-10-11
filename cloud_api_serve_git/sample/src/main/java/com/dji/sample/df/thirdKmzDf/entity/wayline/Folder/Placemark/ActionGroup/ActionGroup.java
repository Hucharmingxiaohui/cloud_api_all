package com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup;

import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.Action;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.ActionTrigger.ActionTrigger;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ActionGroup {
    @JsonProperty("actionGroupId")//动作id
    private int actionGroupId=0;
    @JsonProperty("actionGroupStartIndex")//动作起始位置,无默认值
    private int actionGroupStartIndex;
    @JsonProperty("actionGroupEndIndex")//动作结束位置，无默认值
    private int actionGroupEndIndex;
    @JsonProperty("actionGroupMode")//动作执行序列
    private String actionGroupMode="sequence";
    @JsonProperty("actionTrigger")//动作触发参数
    private ActionTrigger actionTrigger;
    @JsonProperty("actionList")//动作触发列表
    private List<Action> actionList;

}
