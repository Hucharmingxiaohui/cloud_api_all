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
 * 光伏板组件实体类
 * 包含四个角的经纬度坐标及对应的像素行列坐标
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("manage_solar_panel_component")
public class SolarPanelComponent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 光伏组件名称
     */
    @TableField("solar_panel_component_name")
    private String solarPanelComponentName;

    /**
     * 角1经度
     */
    @TableField("corner1_lng")
    private Double corner1Lng;

    /**
     * 角1纬度
     */
    @TableField("corner1_lat")
    private Double corner1Lat;

    /**
     * 角2经度
     */
    @TableField("corner2_lng")
    private Double corner2Lng;

    /**
     * 角2纬度
     */
    @TableField("corner2_lat")
    private Double corner2Lat;

    /**
     * 角3经度
     */
    @TableField("corner3_lng")
    private Double corner3Lng;

    /**
     * 角3纬度
     */
    @TableField("corner3_lat")
    private Double corner3Lat;

    /**
     * 角4经度
     */
    @TableField("corner4_lng")
    private Double corner4Lng;

    /**
     * 角4纬度
     */
    @TableField("corner4_lat")
    private Double corner4Lat;

    /**
     * 角1像素列号
     */
    @TableField("corner1_col")
    private Integer corner1Col;

    /**
     * 角1像素行号
     */
    @TableField("corner1_row")
    private Integer corner1Row;

    /**
     * 角2像素列号
     */
    @TableField("corner2_col")
    private Integer corner2Col;

    /**
     * 角2像素行号
     */
    @TableField("corner2_row")
    private Integer corner2Row;

    /**
     * 角3像素列号
     */
    @TableField("corner3_col")
    private Integer corner3Col;

    /**
     * 角3像素行号
     */
    @TableField("corner3_row")
    private Integer corner3Row;

    /**
     * 角4像素列号
     */
    @TableField("corner4_col")
    private Integer corner4Col;

    /**
     * 角4像素行号
     */
    @TableField("corner4_row")
    private Integer corner4Row;

    /**
     * 正射图ID
     */
    @TableField("orthophoto_id")
    private String orthophotoId;

    /**
     * 光伏板ID
     */
    @TableField("solar_panel_id")
    private String solarPanelId;
}
