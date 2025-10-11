package com.dji.sample.df.kmzDf.entity.wayline.Folder;

import com.dji.sample.df.kmzDf.entity.wayline.Folder.GlobalWaypointHeadingParam.GlobalWaypointHeadingParam;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.PayloadParam.PayloadParam;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.Placemark;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.WaylineCoordinateSysParam.WaylineCoordinateSysParam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Folder {
    @JsonProperty("templateType")//航线类型
    private String templateType="waypoint";

    @JsonProperty ("templateId")//航线编号
    private int templateId=0;

    @JsonProperty("waylineId")//航线id
    private int waylineId=0;

    @JsonProperty("executeHeightMode")//高度模式
    private String executeHeightMode="relativeToStartPoint";

    @JsonProperty("waylineCoordinateSysParam")//坐标系，有默认值
    private WaylineCoordinateSysParam waylineCoordinateSysParam;

    @JsonProperty("autoFlightSpeed")//全局飞行速度,无默认
    private float autoFlightSpeed;

    @JsonProperty("globalHeight")//全局飞行高度，无默认
    private float globalHeight;

    @JsonProperty("caliFlightEnable")//是否开启标定飞行m300.m350rtk
    private int caliFlightEnable=0;

    @JsonProperty("gimbalPitchMode")//云台府仰角控制模式,manual：手动控制。 usePointSetting：依照每个航点设置。
    private String gimbalPitchMode="usePointSetting";

    @JsonProperty("globalWaypointHeadingParam")//全局偏航角
    private GlobalWaypointHeadingParam globalWaypointHeadingParam;

    @JsonProperty("globalWaypointTurnMode")//全局航点转弯类型
    private String globalWaypointTurnMode="toPointAndStopWithDiscontinuityCurvature";

    @JsonProperty("globalUseStraightLine")//全局是否贴合航线
    private int globalUseStraightLine = 1;

    @JsonProperty("placeMarks")
    private List<Placemark> placeMarks;//航点列表
    @JsonProperty("payloadParam")
    private PayloadParam payloadParam;//负载参数

}
