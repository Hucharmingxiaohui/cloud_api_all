package com.cleaner.djuav.domain.kml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

@Data
@XStreamAlias("wpml:waypointTurnParam")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KmlWaypointTurnParam {

    @XStreamAlias("wpml:waypointTurnMode")
    private String waypointTurnMode;

    @XStreamAlias("wpml:waypointTurnDampingDist")
    private String waypointTurnDampingDist;

}
