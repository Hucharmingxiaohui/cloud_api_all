package com.dji.sample.df.electricInspectionDf.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

// 设备实体类
@Data
@TableName("pub_device_df") // 假设表名为 device
public class PubDeviceDfEntity implements Serializable {
    @TableId(type = IdType.AUTO) // MyBatis-Plus注解，指定主键字段及其生成策略
    private Integer id; // 主键id

    @TableField("sub_code") // MyBatis-Plus注解，指定数据库字段名
    private String subCode; // 变电站编码

    @TableField("device_id") // MyBatis-Plus注解，指定数据库字段名
    private String deviceId; // 主设备ID

    @TableField("device_name") // MyBatis-Plus注解，指定数据库字段名
    private String deviceName; // 主设备名称

    @TableField("device_type") // MyBatis-Plus注解，指定数据库字段名
    private Integer deviceType; // 主设备类型，字典device_type

    @TableField("area_id") // MyBatis-Plus注解，指定数据库字段名
    private String areaId; // 区域id

    @TableField("bay_id") // MyBatis-Plus注解，指定数据库字段名
    private String bayId; // 所属间隔ID
}
