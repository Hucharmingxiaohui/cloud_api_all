package com.cleaner.djuav.domain.kml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

@Data
@XStreamAlias("wpml:action")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KmlAction {

    @XStreamAlias("wpml:actionId")
    private String actionId;

    @XStreamAlias("wpml:actionActuatorFunc")
    private String actionActuatorFunc;

    @XStreamAlias("wpml:actionActuatorFuncParam")
    private KmlActionActuatorFuncParam actionActuatorFuncParam;


}
