package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.GimbalEvenlyRotate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GimbalEvenlyRotate {
    @JsonProperty("gimbalPitchRotateAngle")
    private float gimbalPitchRotateAngle;//云台Pitch转动角度
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex;//负载挂载位置
}
