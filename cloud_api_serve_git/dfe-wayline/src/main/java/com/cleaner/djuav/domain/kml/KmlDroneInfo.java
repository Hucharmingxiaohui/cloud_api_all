package com.cleaner.djuav.domain.kml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

@Data
@XStreamAlias("wpml:droneInfo")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KmlDroneInfo {

    @XStreamAlias("wpml:droneEnumValue")
    private String droneEnumValue;

    @XStreamAlias("wpml:droneSubEnumValue")
    private String droneSubEnumValue;

}
