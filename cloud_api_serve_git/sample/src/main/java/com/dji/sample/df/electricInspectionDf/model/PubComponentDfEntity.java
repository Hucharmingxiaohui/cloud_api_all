package com.dji.sample.df.electricInspectionDf.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

// 设备实体类
@Data
@TableName("pub_component_df")
public class PubComponentDfEntity implements Serializable {
    @TableId(type = IdType.AUTO) // MyBatis-Plus注解，指定主键字段及其生成策略
    private Integer id; // 主键id

    @TableField("sub_code") // MyBatis-Plus注解，指定数据库字段名
    private String subCode; // 变电站编码

    @TableField("area_id") // MyBatis-Plus注解，指定数据库字段名
    private String areaId; // 区域ID

    @TableField("bay_id") // MyBatis-Plus注解，指定数据库字段名
    private String bayId; // 间隔ID

    @TableField("device_id") // MyBatis-Plus注解，指定数据库字段名
    private String deviceId; // 所属设备ID

    @TableField("component_id") // MyBatis-Plus注解，指定数据库字段名
    private String componentId; // 部件ID

    @TableField("component_name") // MyBatis-Plus注解，指定数据库字段名
    private String componentName; // 部件名称
}
