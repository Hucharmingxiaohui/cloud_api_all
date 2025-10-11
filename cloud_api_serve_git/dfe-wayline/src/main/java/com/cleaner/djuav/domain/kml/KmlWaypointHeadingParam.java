package com.cleaner.djuav.domain.kml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

@Data
@XStreamAlias("wpml:waypointHeadingParam")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KmlWaypointHeadingParam {

    @XStreamAlias("wpml:waypointHeadingMode")
    private String waypointHeadingMode;

    @XStreamAlias("wpml:waypointHeadingAngle")
    private String waypointHeadingAngle;

    @XStreamAlias("wpml:waypointPoiPoint")
    private String waypointPoiPoint;

    @XStreamAlias("wpml:waypointHeadingPathMode")
    private String waypointHeadingPathMode;


}
