package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.Zoom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
//变焦
@Data
public class Zoom {
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex=0;//负载挂载位置
    @JsonProperty("focalLength")
    private float focalLength=48;//变焦焦距
}
