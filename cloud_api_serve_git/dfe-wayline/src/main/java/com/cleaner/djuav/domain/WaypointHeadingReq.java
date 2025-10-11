package com.cleaner.djuav.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;

/**
 * Author:Cleaner
 * Date: 2024/12/22 13:05
 **/
@Data
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class WaypointHeadingReq implements Serializable {

    /**
     * 偏航角模式
     */
    private String waypointHeadingMode;

    /**
     * 偏航角度
     */
    private Double waypointHeadingAngle;

    /**
     * 兴趣点
     */
    private String waypointPoiPoint;
}
