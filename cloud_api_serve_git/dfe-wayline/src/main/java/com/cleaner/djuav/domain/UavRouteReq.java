package com.cleaner.djuav.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Author:Cleaner
 * Date: 2024/12/22 10:46
 **/
@Data
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class UavRouteReq implements Serializable {

    private String routeName;

    /**
     * 航线类型
     */
    private String templateType;

    /**
     * 无人机类型
     */
    private Integer droneType;

    /**
     * 无人机子类型
     */
    private Integer subDroneType;

    /**
     * 负载类型
     */
    private Integer payloadType;

    /**
     * 负载挂载位置
     */
    private Integer payloadPosition;

    /**
     * 负载图片存储类型
     */
    private String imageFormat;

    /**
     * 航线结束动作
     */
    private String finishAction;

    /**
     * 失控动作
     */
    private String exitOnRcLostAction;

    /**
     * 全局航线高度
     */
    private Double globalHeight;

    /**
     * 全局航线飞行速度
     */
    private Double autoFlightSpeed;

    /**
     * 全局偏航角模式
     */
    private WaypointHeadingReq waypointHeadingReq;

    /**
     * 全局航点转弯模式
     */
    private WaypointTurnReq waypointTurnReq;

    /**
     * 云台俯仰角控制模式
     */
    private String gimbalPitchMode;

    /**
     * 参考起飞点
     */
    private String takeOffRefPoint;

    /**
     * 航点列表
     */
    private List<RoutePointReq> routePointList;

    /**
     * 建图航拍、倾斜摄影、航带飞行模板参数
     */

    private MappingTypeReq mappingTypeReq;

    /**
     * 航线初始动作列表
     */
    private List<PointActionReq> startActionList;
    /**
     * 起飞安全高度
     */
    private Double takeOffSecurityHeight;
    /**
     * 过渡速度
     */
    private Double globalTransitionalSpeed;
    /**
     * 返航高度
     */
    private Double globalRTHHeight;

}
