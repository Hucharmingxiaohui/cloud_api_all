package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.WaypointHeadingParam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WaypointHeadingParam {
    @JsonProperty("waypointHeadingMode")//飞行器偏航角模式
    private String waypointHeadingMode="followWayline";
    @JsonProperty("waypointHeadingAngle")//偏航角度
    private float waypointHeadingAngle=0;
    @JsonProperty("waypointPoiPoint")//兴趣点
    private String waypointPoiPoint="0.000000,0.000000,0.000000";
    @JsonProperty("waypointHeadingPathMode")//飞行器偏航角转动模式
    private String waypointHeadingPathMode="followBadArc";
    @JsonProperty("waypointHeadingPoiIndex")//偏航序号
    private int waypointHeadingPoiIndex=0;
}
