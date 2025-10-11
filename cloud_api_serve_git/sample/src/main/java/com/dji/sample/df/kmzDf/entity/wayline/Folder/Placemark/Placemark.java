package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark;

import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.ActionGroup;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.Point.Point;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.WaypointHeadingParam.WaypointHeadingParam;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.WaypointTurnParam.WaypointTurnParam;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Placemark {
    @JsonProperty("point")//经纬度坐标
    private Point point;

    @JsonProperty("index")//航点序号
    private int index=0;

    @JsonProperty("ellipsoidHeight")//航点高度（WGS84椭球高度）
    private float ellipsoidHeight;

    @JsonProperty("height")//航点高度（EGM96海拔高度/相对起飞点高度/AGL相对地面高度）
    private float height;

    @JsonProperty("useGlobalHeight")//是否使用全局高度
    private int useGlobalHeight=0;

    @JsonProperty("useGlobalSpeed")//是否使用全局速度
    private int useGlobalSpeed=0;

    @JsonProperty("useGlobalHeadingParam")//是否启用全局偏航角度
    private int useGlobalHeadingParam=0;

    @JsonProperty("useGlobalTurnParam")//是否启用全局转弯
    private int useGlobalTurnParam=0;

    @JsonProperty("waypointSpeed")//航点速度
    private float waypointSpeed;

    @JsonProperty("waypointHeadingParam")//偏航角参数
    private WaypointHeadingParam waypointHeadingParam;

    @JsonProperty("waypointTurnParam")//转弯模式参数
    private WaypointTurnParam waypointTurnParam;

    @JsonProperty("gimbalPitchAngle")//云台府仰角度数
    private int gimbalPitchAngle=0;

    @JsonProperty("useStraightLine")//是否使用贴合直线
    private int useStraightLine=1;

    @JsonProperty("actionGroup")
    private ActionGroup actionGroup;//动作

    @JsonProperty("isRisky")//是否是风险点
    private int isRisky=0;



}
