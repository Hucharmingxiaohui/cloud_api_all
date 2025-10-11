package com.dji.sample.df.waylineDf.model.dto;

import lombok.Data;

/**
 * @author sean
 * @version 1.1
 * @date 2022/6/9
 */
@Data
public class FlighttaskProgressExt {

    private Integer currentWaypointIndex;

    private Integer mediaCount;

    private String flightId;

    private String trackId;
}
