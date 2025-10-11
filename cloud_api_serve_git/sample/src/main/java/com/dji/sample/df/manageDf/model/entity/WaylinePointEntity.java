package com.dji.sample.df.manageDf.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @author 贾彬
 * @Time 2024/11/22 16:23
 */
@TableName(value = "pub_wayline_point_df")
@Data
public class WaylinePointEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField(value = "point_code")
    private String pointCode;

    @TableField(value = "sub_code")
    private String subCode;

    @TableField(value = "area_name")
    private String areaName;

    @TableField(value = "area_id")
    private String areaId;

    @TableField(value = "bay_name")
    private String bayName;

    @TableField(value = "bay_id")
    private String bayId;

    @TableField(value = "device_name")
    private String deviceName;

    @TableField(value = "device_id")
    private String deviceId;

    @TableField(value = "device_type")
    private String deviceType;

    @TableField(value = "component_name")
    private String componentName;

    @TableField(value = "component_type_code")
    private String componentTypeCode;

    @TableField(value = "phase")
    private String phase;

    @TableField(value = "point_describe")
    private String pointDescribe;

    @TableField(value = "point_name")
    private String pointName;

    @TableField(value = "component_id")
    private String componentId;

    @TableField(value = "point_analyse_type")
    private String pointAnalyseType;

    @TableField(value = "waypoint_name")
    private String waypointName;

    @TableField(value = "wayline_id")
    private String waylineId;

    @TableField(value = "waypoint_sequence")
    private String waypointSequence;

    @TableField(value = "tem_type")
    private Integer temType;

    @TableField(value = "tem_conf")
    private String temConf;

}
