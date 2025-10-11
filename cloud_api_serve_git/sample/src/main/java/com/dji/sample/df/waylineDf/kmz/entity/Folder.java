package com.dji.sample.df.waylineDf.kmz.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class Folder {
    @JsonProperty("autoFlightSpeed")
    private float autoFlightSpeed;
    @JsonProperty("globalHeight")
    private float globalHeight;
    @JsonProperty("globalWaypointTurnMode")
    private String globalWaypointTurnMode;
    @JsonProperty("placeMarks")
    private List<Placemark> placeMarks;

}

