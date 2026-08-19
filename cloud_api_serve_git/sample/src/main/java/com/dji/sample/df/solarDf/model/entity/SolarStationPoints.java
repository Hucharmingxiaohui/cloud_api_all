package com.dji.sample.df.solarDf.model.entity;

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
 * 光伏站点点位信息实体类
 */
@Data
@TableName("solar_station_points")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolarStationPoints implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("station_name")
    private String stationName;

    @TableField("station_code")
    private String stationCode;

    @TableField("area_name")
    private String areaName;

    @TableField("area_id")
    private String areaId;

    @TableField("bay_name")
    private String bayName;

    @TableField("bay_id")
    private String bayId;

    @TableField("main_device_name")
    private String mainDeviceName;

    @TableField("main_device_id")
    private String mainDeviceId;

    @TableField("component_name")
    private String componentName;

    @TableField("component_id")
    private String componentId;

    @TableField("point_name")
    private String pointName;

    @TableField("point_id")
    private String pointId;

    @TableField("device_type")
    private String deviceType;

    @TableField("phase")
    private String phase;

    @TableField("save_type")
    private String saveType;

    @TableField("upper_value")
    private String upperValue;

    @TableField("lower_value")
    private String lowerValue;

    @TableField("label_attrib")
    private String labelAttrib;

    @TableField("data_type")
    private Integer dataType;

    @TableField("point_type")
    private String pointType;

    @TableField("device_info")
    private String deviceInfo;

    @TableField("recognition_type")
    private String recognitionType;

    @TableField("meter_type")
    private String meterType;

    @TableField("video_pos")
    private String videoPos;

    @TableField("appearance_type")
    private String appearanceType;
}
