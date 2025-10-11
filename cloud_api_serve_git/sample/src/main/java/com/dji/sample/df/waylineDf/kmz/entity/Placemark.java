package com.dji.sample.df.waylineDf.kmz.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Placemark {
    @JsonProperty("coordinates")
    private String coordinates;
    @JsonProperty("height")
    private float height;
    @JsonProperty("ellipsoidHeight")
    private Float ellipsoidHeight;
    @JsonProperty("waypointHeadingAngle")
    private int waypointHeadingAngle;
    @JsonProperty("gimbalPitchAngle")
    private int gimbalPitchAngle;
    @JsonProperty("waypointSpeed")
    private float waypointSpeed;
    @JsonProperty("actionGroup")
    private List<Action> actionGroup;


}

