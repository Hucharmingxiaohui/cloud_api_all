package com.dji.sample.df.solar.model.entity;

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
 * 太阳能光伏板巡检区域参数实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("manage_solar_panel")
public class SolarPanel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 光伏板区域名称
     */
    @TableField("solar_panel_name")
    private String solarPanelName;

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
     * 航线高度（米）
     */
    @TableField("flight_altitude")
    private Double flightAltitude;

    /**
     * 光伏板倾角（度）
     */
    @TableField("angle_tilt")
    private Double angleTilt;

    /**
     * 横向航线数
     */
    @TableField("horizontal_lines")
    private Integer horizontalLines;

    // ========== 新增字段 ==========

    /**
     * 光伏区域海拔
     */
    @TableField("area_height")
    private Double areaHeight;

    /**
     * 光伏架设高度
     */
    @TableField("panel_height")
    private Double panelHeight;

    /**
     * 光伏板朝向
     */
    @TableField("panel_heading")
    private Double panelHeading;

    /**
     * 区域边距
     */
    @TableField("margin")
    private Double margin;

    /**
     * 航线内点数
     */
    @TableField("points_per_line")
    private Integer pointsPerLine;
}
