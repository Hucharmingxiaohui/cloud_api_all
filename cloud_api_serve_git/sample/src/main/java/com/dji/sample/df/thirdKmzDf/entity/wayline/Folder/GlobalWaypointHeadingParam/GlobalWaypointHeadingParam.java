package com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.GlobalWaypointHeadingParam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

//全局偏航角
@Data
public class GlobalWaypointHeadingParam {
    private String waypointHeadingMode="followWayline";//飞行器偏航模式
    @JsonProperty("waypointHeadingAngle")
    private float waypointHeadingAngle=0;//偏航角度数
    @JsonProperty("waypointPoiPoint")
    private String waypointPoiPoint="0.000000,0.000000,0.000000";//兴趣点
    @JsonProperty("waypointHeadingPathMode")
    private String waypointHeadingPathMode="followBadArc";
    @JsonProperty("waypointHeadingPoiIndex")
    private int waypointHeadingPoiIndex=0;//编号

}
