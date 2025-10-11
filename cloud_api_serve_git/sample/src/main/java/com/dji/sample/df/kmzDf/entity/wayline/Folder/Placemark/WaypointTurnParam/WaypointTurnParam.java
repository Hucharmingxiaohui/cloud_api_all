package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.WaypointTurnParam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WaypointTurnParam {
    @JsonProperty("waypointTurnMode")//转弯模式
    private String waypointTurnMode="toPointAndStopWithDiscontinuityCurvature";
    @JsonProperty("waypointTurnDampingDist")//转弯截距
    private double waypointTurnDampingDist=0.200000002980232;
}
