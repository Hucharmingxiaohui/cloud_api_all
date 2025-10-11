package com.dji.sample.df.electricInspectionDf.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

//间隔实体类
@Data
@TableName("pub_bay_df")
public class PubBayDfEntity implements Serializable {
    @TableId(type = IdType.AUTO) // MyBatis-Plus注解，指定主键字段及其生成策略
    private Integer id; // 主键id

    @TableField("sub_code") // MyBatis-Plus注解，指定数据库字段名
    private String subCode; // 变电站编码

    @TableField("area_id") // MyBatis-Plus注解，指定数据库字段名
    private String areaId; // 所属区域ID

    @TableField("bay_id") // MyBatis-Plus注解，指定数据库字段名
    private String bayId; // 间隔ID

    @TableField("bay_name") // MyBatis-Plus注解，指定数据库字段名
    private String bayName; // 间隔名称

    @TableField("volt_level") // MyBatis-Plus注解，指定数据库字段名
    private Integer voltLevel; // 电压等级，单位kV

    @TableField("bay_src") // MyBatis-Plus注解，指定数据库字段名
    private Integer baySrc; // 间隔数据来源
}
