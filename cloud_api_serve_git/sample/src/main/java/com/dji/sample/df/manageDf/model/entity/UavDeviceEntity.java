package com.dji.sample.df.manageDf.model.entity;

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
 * pub_uav_device_df表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("pub_uav_device_df")
public class UavDeviceEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField(value = "sub_code")
    private String subCode;

    @TableField(value = "device_sn")
    private String deviceSn;

    @TableField(value = "device_name")
    private String deviceName;

    @TableField(value = "workspace_id")
    private String workspaceId;

    @TableField(value = "device_type")
    private Integer deviceType;

    @TableField(value = "device_major")
    private String deviceMajor;

    @TableField(value = "main_device_type")
    private Integer mainDeviceType;
}