package com.cleaner.djuav.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class RoutePointReq implements Serializable {

    /**
     * 航点编号
     */
    private Integer routePointIndex;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 高度
     */
    private Double height;

    /**
     * 飞行速度
     */
    private Double speed;

    /**
     * 航点偏航角
     */
    private WaypointHeadingReq waypointHeadingReq;

    /**
     * 航点转弯模式
     */
    private WaypointTurnReq waypointTurnReq;

    /**
     * 航点云台俯仰角
     */
    private Double gimbalPitchAngle;

    /**
     * 航点动作列表
     */
    private List<PointActionReq> actions;

    /**
     * 等时拍照间隔时间 单位s
     */
    private Double timeInterval;

    /**
     * 等距拍照间隔距离 单位m
     */
    private Double distanceInterval;

    /**
     * 停止间隔拍照航点编号
     */
    private Integer endIntervalRouteIndex;

}
