package com.dji.sample.df.pointOfInterestDf.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 兴趣点实体类
 */
@Data
@Entity
@TableName("manage_point_of_interest")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PointOfInterest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 兴趣点名称
     */
    @TableField("point_name")
    private String pointName;

    /**
     * 兴趣点经度
     */
    @TableField("point_longitude")
    private Double pointLongitude;

    /**
     * 兴趣点纬度
     */
    @TableField("point_latitude")
    private Double pointLatitude;

    /**
     * 兴趣点高度
     */
    @TableField("point_altitude")
    private Double pointAltitude;

    /**
     * 环绕高度
     */
    @TableField("orbit_height")
    private Double orbitHeight;

    /**
     * 环绕半径
     */
    @TableField("orbit_radius")
    private Double orbitRadius;

    /**
     * 初始方向
     */
    @TableField("init_direction")
    private Double initDirection;


    /**
     * 焦距
     */
    @TableField("focal_length")
    private Double focalLength;
}
