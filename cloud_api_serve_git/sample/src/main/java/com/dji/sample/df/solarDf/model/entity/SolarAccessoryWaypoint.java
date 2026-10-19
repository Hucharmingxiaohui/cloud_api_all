package com.dji.sample.df.solarDf.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("manage_solar_accessory_waypoint")
public class SolarAccessoryWaypoint {
    @TableId(type = IdType.INPUT)
    private String id;
    private String deviceId;
    private String name;
    private Integer sortOrder;
    private Double longitude;
    private Double latitude;
    private Double height;
    private Double heading;
    private Double pitch;
    private Double focalLength;
    private String action;
    private String imageFormat;
    private Boolean enabled;
}
