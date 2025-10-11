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
public class WaypointTurnReq implements Serializable {

    /**
     * 航点转弯模式
     */
    private String waypointTurnMode;

    /**
     * 航点转弯截距
     */
    private Double waypointTurnDampingDist;

    /**
     * 该航段是否贴合直线
     */
    private Integer useStraightLine;


}
