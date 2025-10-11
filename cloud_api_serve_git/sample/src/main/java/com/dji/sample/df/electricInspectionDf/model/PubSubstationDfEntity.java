package com.dji.sample.df.electricInspectionDf.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("pub_substation_df")
public class PubSubstationDfEntity implements Serializable {
    @TableId(type = IdType.AUTO) // MyBatis-Plus注解，指定主键字段及其生成策略
    private Integer id; // 主键id

    @TableField("sub_code") // MyBatis-Plus注解，指定数据库字段名
    private String subCode; // 变电站编码

    @TableField("sub_name") // MyBatis-Plus注解，指定数据库字段名
    private String subName; // 变电站名称

    @TableField("workspace_id") // MyBatis-Plus注解，指定数据库字段名
    private String workspaceId; // 组织(工作空间)id

    @TableField("user_id") // MyBatis-Plus注解，指定数据库字段名
    private String userId; // 用户id，处理上级系统

    @TableField("dock_sn") // MyBatis-Plus注解，指定数据库字段名
    private String dockSn; // 机场唯一标识码，执行上级任务

    @TableField("drone_sn") // MyBatis-Plus注解，指定数据库字段名
    private String droneSn; // 无人机唯一编码和机场配套

    @TableField("join_type") // MyBatis-Plus注解，指定数据库字段名
    private Integer joinType; // 接入类型，1直接接入型 2边缘节点型

    @TableField("des") // MyBatis-Plus注解，指定数据库字段名
    private String des; // 变电站描述

    @TableField("dept_id") // MyBatis-Plus注解，指定数据库字段名
    private Integer deptId; // 所属运维班ID

    @TableField("uri") // MyBatis-Plus注解，指定数据库字段名
    private String uri; // 未知字段

    @TableField("addr") // MyBatis-Plus注解，指定数据库字段名
    private String addr; // 地址

    @TableField("vlevel") // MyBatis-Plus注解，指定数据库字段名
    private Integer vlevel; // 电压等级

    @TableField("longitude") // MyBatis-Plus注解，指定数据库字段名
    private Double longitude; // 经度

    @TableField("latitude") // MyBatis-Plus注解，指定数据库字段名
    private Double latitude; // 纬度

    @TableField("altitude") // MyBatis-Plus注解，指定数据库字段名
    private Double altitude; // 海拔

    @TableField("sub_type") // MyBatis-Plus注解，指定数据库字段名
    private String subType; // 变电站类型：AIS站、GIS站、HGIS站
}
