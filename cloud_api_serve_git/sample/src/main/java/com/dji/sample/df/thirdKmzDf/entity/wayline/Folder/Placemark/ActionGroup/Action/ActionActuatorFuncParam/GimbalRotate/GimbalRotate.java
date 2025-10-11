package com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.GimbalRotate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

//转动云台
@Data
public class GimbalRotate {
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex=0; //负载挂载位置
    @JsonProperty("gimbalHeadingYawBase")
    private String gimbalHeadingYawBase="north"; //云台偏航角转动坐标系
    @JsonProperty("gimbalRotateMode")
    private String gimbalRotateMode="absoluteAngle"; //云台转动模式
    @JsonProperty("gimbalPitchRotateEnable")
    private int gimbalPitchRotateEnable=0; //是否使能云台Pitch转动
    @JsonProperty("gimbalPitchRotateAngle")
    private float gimbalPitchRotateAngle=0; //云台Pitch转动角度
    @JsonProperty("gimbalRollRotateEnable")
    private int gimbalRollRotateEnable=0; //是否使能云台Roll转动
    @JsonProperty("gimbalRollRotateAngle")
    private float gimbalRollRotateAngle=0; //云台Roll转动角度
    @JsonProperty("gimbalYawRotateEnable")
    private int gimbalYawRotateEnable=0; //是否使能云台Yaw转动
    @JsonProperty("gimbalYawRotateAngle")
    private float gimbalYawRotateAngle=0; //云台Yaw转动角度
    @JsonProperty("gimbalRotateTimeEnable")
    private int gimbalRotateTimeEnable=0; //是否使能云台转动时间
    @JsonProperty("gimbalRotateTime")
    private float gimbalRotateTime=0;//云台完成转动用时
}
