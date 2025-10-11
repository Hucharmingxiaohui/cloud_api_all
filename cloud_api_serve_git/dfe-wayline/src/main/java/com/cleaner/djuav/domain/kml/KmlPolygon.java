package com.cleaner.djuav.domain.kml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

@Data
@XStreamAlias("Polygon")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KmlPolygon {

    @XStreamAlias("outerBoundaryIs")
    private KmlOuterBoundaryIs outerBoundaryIs;
}
