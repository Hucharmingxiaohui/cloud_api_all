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
 * 风机站点点位信息实体类
 */
@Data
@TableName("fan_station_points")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FanStationPoints implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID，自动生成

    /**
     * 场站名称
     */
    @TableField("station_name")
    private String stationName;

    /**
     * 场站编码
     */
    @TableField("station_code")
    private String stationCode;

    /**
     * 区域名称
     */
    @TableField("area_name")
    private String areaName;

    /**
     * 区域ID
     */
    @TableField("area_id")
    private String areaId;

    /**
     * 间隔名称
     */
    @TableField("bay_name")
    private String bayName;

    /**
     * 间隔ID
     */
    @TableField("bay_id")
    private String bayId;

    /**
     * 主设备名称（风机名）
     */
    @TableField("main_device_name")
    private String mainDeviceName;

    /**
     * 主设备ID（风机id）
     */
    @TableField("main_device_id")
    private String mainDeviceId;

    /**
     * 组件名称
     */
    @TableField("component_name")
    private String componentName;

    /**
     * 组件ID
     */
    @TableField("component_id")
    private String componentId;

    /**
     * 测点名称（按规则生成）
     */
    @TableField("point_name")
    private String pointName;

    /**
     * 测点ID
     */
    @TableField("point_id")
    private String pointId;

    /**
     * 设备类型
     */
    @TableField("device_type")
    private String deviceType;

    /**
     * 相位
     */
    @TableField("phase")
    private String phase;

    /**
     * 存储类型
     */
    @TableField("save_type")
    private String saveType;

    /**
     * 上限值
     */
    @TableField("upper_value")
    private String upperValue;

    /**
     * 下限值
     */
    @TableField("lower_value")
    private String lowerValue;

    /**
     * 标签属性
     */
    @TableField("label_attrib")
    private String labelAttrib;

    /**
     * 数据类型
     */
    @TableField("data_type")
    private Integer dataType;

    /**
     * 测点类型
     */
    @TableField("point_type")
    private String pointType;

    /**
     * 设备信息
     */
    @TableField("device_info")
    private String deviceInfo;

    /**
     * 识别类型
     */
    @TableField("recognition_type")
    private String recognitionType;

    /**
     * 仪表类型
     */
    @TableField("meter_type")
    private String meterType;

    /**
     * 视频位置
     */
    @TableField("video_pos")
    private String videoPos;

    /**
     * 外观类型
     */
    @TableField("appearance_type")
    private String appearanceType;

    /**
     * 风机任务类型(0停机1不停机)
     */
    @TableField("fan_type")
    private Integer fanType;
}
