package com.cleaner.djuav.domain.kml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

@Data
@XStreamAlias("wpml:actionTrigger")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KmlActionTrigger {

    @XStreamAlias("wpml:actionTriggerType")
    private String actionTriggerType;

    @XStreamAlias("wpml:actionTriggerParam")
    private String actionTriggerParam;


}
