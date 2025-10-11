package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.CustomDirName;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CustomDirName {
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex=0;//负载挂载位置
    @JsonProperty("directoryName")
    private String directoryName="xxx";//新文件夹的名称
}
