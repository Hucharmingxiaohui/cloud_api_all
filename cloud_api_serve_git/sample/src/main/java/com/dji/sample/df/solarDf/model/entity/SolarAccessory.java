package com.dji.sample.df.solarDf.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("manage_solar_accessory")
public class SolarAccessory {
    @TableId(type = IdType.INPUT)
    private String id;
    private String orthophotoId;
    private String name;
    private String deviceType;
    private Double longitude;
    private Double latitude;
    private Double height;
    private Boolean enabled;
    @TableField(exist = false)
    private List<SolarAccessoryWaypoint> waypoints = new ArrayList<>();
}
