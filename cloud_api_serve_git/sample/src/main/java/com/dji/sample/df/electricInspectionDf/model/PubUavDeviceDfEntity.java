package com.dji.sample.df.electricInspectionDf.model;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("pub_uav_device_df")
public class PubUavDeviceDfEntity implements Serializable {
    @TableId(type = IdType.AUTO) // MyBatis-Plus注解，指定主键字段及其生成策略
    private Integer id; // 主键id

    @TableField("sub_code") // MyBatis-Plus注解，指定数据库字段名
    private String subCode; // 变电站编码

    @TableField("device_sn") // MyBatis-Plus注解，指定数据库字段名
    private String deviceSn; // 设备唯一编码（无人机、机场、遥控器）

    @TableField("device_name") // MyBatis-Plus注解，指定数据库字段名
    private String deviceName; // 设备名称

    @TableField("workspace_id") // MyBatis-Plus注解，指定数据库字段名
    private String workspaceId; // 组织id

    @TableField("device_type") // MyBatis-Plus注解，指定数据库字段名
    private Integer deviceType; // 设备类型（无人机相关设备的型号、沿用大疆）

    @TableField("major") // MyBatis-Plus注解，指定数据库字段名
    private String deviceMajor; // 设备专业

    @TableField("main_device_type") // MyBatis-Plus注解，指定数据库字段名
    private String mainDeviceType; // 无人机设备主类型：1. 飞行器；2. 遥控器；3. 机巢
}
