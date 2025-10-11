package com.dji.sample.df.waylineDf.kmz.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Wayline {
    @JsonProperty("missionConfig")
    private MissionConfig missionConfig;
    @JsonProperty("folder")
    private Folder folder;

}