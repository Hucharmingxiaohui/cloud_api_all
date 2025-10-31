package com.dji.sample.df.wind.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 风机参数实体类
 */
@Data
@TableName("manage_fan")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WindTurbine implements Serializable {

    @TableId(type = IdType.INPUT)
    private String id; // 主键ID

    @TableField("turbine_name")
    private String turbineName; // 风机名称

    @TableField("airport_longitude")
    private Double airportLongitude; // 机场经度

    @TableField("airport_latitude")
    private Double airportLatitude; // 机场纬度

    @TableField("airport_altitude")
    private Double airportAltitude; // 机场海拔高度

    @TableField("approach_yaw")
    private Double approachYaw; // 正对着时航向角

    @TableField("peak_longitude")
    private Double peakLongitude; // 飞行最高点经度

    @TableField("peak_latitude")
    private Double peakLatitude; // 飞行最高点纬度

    @TableField("peak_altitude")
    private Double peakAltitude; // 飞行最高点高度

    @TableField("blade_center_height")
    private Double bladeCenterHeight; // 叶片旋转中心高度

    @TableField("blade_stop_angle")
    private Double bladeStopAngle; // 停机时叶片1夹角

    @TableField("blade_length")
    private Double bladeLength; // 单个叶片长度

    @TableField("uav_blade_distance")
    private Double uavBladeDistance;//无人机距离叶片距离

    @TableField("blade_bottom_height")
    private Double bladeBottomHeight;//风机底部的高度

    @TableField("blade_points")
    private Integer bladePoints;//单个扇叶的点数

    @TableField("tower_points")
    private Integer towerPoints;//塔筒的点数

}
