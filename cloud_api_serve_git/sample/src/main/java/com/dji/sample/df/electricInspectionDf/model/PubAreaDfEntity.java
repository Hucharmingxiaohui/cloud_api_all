package com.dji.sample.df.electricInspectionDf.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
//区域实体类
@Data
@TableName("pub_area_df")
public class PubAreaDfEntity implements Serializable {
    @TableId(type = IdType.AUTO) // MyBatis-Plus注解，指定主键字段及其生成策略
    private Integer id; // 主键id

    @TableField("sub_code") // MyBatis-Plus注解，指定数据库字段名
    private String subCode; // 变电站编码

    @TableField("area_id") // MyBatis-Plus注解，指定数据库字段名
    private String areaId; // 区域ID

    @TableField("area_name") // MyBatis-Plus注解，指定数据库字段名
    private String areaName; // 区域名称

    @TableField("area_src") // MyBatis-Plus注解，指定数据库字段名
    private Integer areaSrc; // 区域数据来源
}
