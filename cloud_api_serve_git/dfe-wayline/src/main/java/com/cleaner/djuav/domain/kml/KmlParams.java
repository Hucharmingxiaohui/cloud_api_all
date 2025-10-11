package com.cleaner.djuav.domain.kml;

import com.cleaner.djuav.domain.MappingTypeReq;
import com.cleaner.djuav.domain.PointActionReq;
import com.cleaner.djuav.domain.WaypointHeadingReq;
import com.cleaner.djuav.domain.WaypointTurnReq;
import lombok.Data;

import java.util.List;

/**
 * 航线文件
 */
@Data
public class KmlParams {

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
     * 航线结束动作
     */
    private String finishAction;

    /**
     * 失控动作
     */
    private String exitOnRcLostAction;

    /**
     * 参考起飞点
     */
    private String takeOffRefPoint;

    /**
     * 全局航点转弯模式
     */
    private WaypointTurnReq waypointTurnReq;

    /**
     * 全局航线高度
     */
    private Double globalHeight;

    /**
     * 全局偏航角模式
     */
    private WaypointHeadingReq waypointHeadingReq;

    /**
     * 云台俯仰角控制模式
     */
    private String gimbalPitchMode;

    /**
     * 全局航线飞行速度
     */
    private Double autoFlightSpeed;

    /**
     * 负载图片存储类型
     */
    private String imageFormat;

    /**
     * 建图航拍、倾斜摄影、航带飞行模板参数
     */
    private MappingTypeReq mappingTypeReq;

    /**
     * 航点列表
     */
    private List<RoutePointInfo> routePointList;

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

