package com.cleaner.djuav.domain.kml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

@Data
@XStreamAlias("wpml:payloadInfo")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KmlPayloadInfo {

    @XStreamAlias("wpml:payloadEnumValue")
    private String payloadEnumValue;

    @XStreamAlias("wpml:payloadPositionIndex")
    private String payloadPositionIndex;

}
