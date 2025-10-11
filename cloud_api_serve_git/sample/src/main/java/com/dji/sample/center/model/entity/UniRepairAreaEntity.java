package com.dji.sample.center.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("uni_repair_area_df")
public class UniRepairAreaEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField(value = "config_code")
    private String configCode;

    @TableField(value = "enable")
    private Integer enable;

    @TableField(value = "start_time")
    private Date startTime;

    @TableField(value = "end_time")
    private Date endTime;

    @TableField(value = "device_level")
    private Integer deviceLevel;

    @TableField(value = "device_list")
    private String deviceList;

    @TableField(value = "coordinate_pixel")
    private String coordinatePixel;

    @TableField(value = "point_list")
    private String pointList;

    @TableField(value = "sub_code")
    private String subCode;
}