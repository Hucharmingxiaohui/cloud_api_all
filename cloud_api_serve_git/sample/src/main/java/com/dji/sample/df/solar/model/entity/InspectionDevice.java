package com.dji.sample.df.solar.model.entity; // 请根据实际包路径调整

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
 * 巡视设备实体类
 * 对应表 inspection_device
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("manage_inspection_device")
public class InspectionDevice implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID，自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 设备名称
     */
    @TableField("device_name")
    private String deviceName;

    /**
     * 目标点经度
     */
    @TableField("target_longitude")
    private Double targetLongitude;

    /**
     * 目标点纬度
     */
    @TableField("target_latitude")
    private Double targetLatitude;

    /**
     * 目标点高度（米）
     */
    @TableField("target_altitude")
    private Double targetAltitude;

    /**
     * 无人机本身高度（米）
     */
    @TableField("drone_altitude")
    private Double droneAltitude;

    /**
     * 距离设备距离（米）
     */
    @TableField("drone_distance")
    private Double droneDistance;

    /**
     * 偏航角（0-360度）
     */
    @TableField("drone_yaw")
    private Integer droneYaw;
}
