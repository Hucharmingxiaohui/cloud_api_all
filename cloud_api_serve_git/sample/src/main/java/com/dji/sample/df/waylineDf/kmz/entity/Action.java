package com.dji.sample.df.waylineDf.kmz.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public  class Action {
    @JsonProperty("actionActuatorFunc")
    private String actionActuatorFunc;

}