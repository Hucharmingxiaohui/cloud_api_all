package com.dji.sample.df.electricInspectionDf.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("pub_wayline_point_df")
public class PubWaylinePointDfEntity implements Serializable {
    @TableId(type = IdType.AUTO) // MyBatis-Plus注解，指定主键字段及其生成策略
    private Integer id; // 主键id

    @TableField("point_code") // MyBatis-Plus注解，指定数据库字段名
    private String pointCode; // 点位编码

    @TableField("sub_code") // MyBatis-Plus注解，指定数据库字段名
    private String subCode; // 变电站编码

    @TableField("area_name") // MyBatis-Plus注解，指定数据库字段名
    private String areaName; // 区域名称

    @TableField("area_id") // MyBatis-Plus注解，指定数据库字段名
    private String areaId; // 区域ID（area_id）

    @TableField("bay_name") // MyBatis-Plus注解，指定数据库字段名
    private String bayName; // 间隔名称

    @TableField("bay_id") // MyBatis-Plus注解，指定数据库字段名
    private String bayId; // 间隔ID（bay_id）

    @TableField("device_name") // MyBatis-Plus注解，指定数据库字段名
    private String deviceName; // 设备名称（设备编号+设备类型（必须按照国网规范填写：油浸式变压器（电抗器）断路器组合电器隔离开关开关柜电流互感器电压互感器避雷器并联电容器组干式电抗器串联补偿装置母线及绝缘子穿墙套管消弧线圈高频阻波器耦合电容器高压熔断器中性点隔直（限直）装置接地装置端子箱及检修电源箱站用变压器站用交流电源系统站用直流电源系统设备构架辅助设施土建设施独立避雷针电力电缆二次屏柜消防系统））

    @TableField("device_id") // MyBatis-Plus注解，指定数据库字段名
    private String deviceId; // 设备ID（main_device_id）

    @TableField("device_type") // MyBatis-Plus注解，指定数据库字段名
    private String deviceType; // 设备类型（必须按照国网规范填写）

    @TableField("component_name") // MyBatis-Plus注解，指定数据库字段名
    private String componentName; // 部位名称（按照国网规范填写）

    @TableField("component_type_code") // MyBatis-Plus注解，指定数据库字段名
    private String componentTypeCode; // 部位类型ID（component_type_code）

    @TableField("phase") // MyBatis-Plus注解，指定数据库字段名
    private String phase; // 相别（实际有相别时必须区分填写）

    @TableField("point_describe") // MyBatis-Plus注解，指定数据库字段名
    private String pointDescribe; // 点位描述

    @TableField("point_name") // MyBatis-Plus注解，指定数据库字段名
    private String pointName; // 设备点位（按照国网规范中的巡视点位填写）

    @TableField("component_id") // MyBatis-Plus注解，指定数据库字段名
    private String componentId; // 部位ID（component_id）

    @TableField("point_analyse_type") // MyBatis-Plus注解，指定数据库字段名
    private String pointAnalyseType; // 识别类型（按照国网规范填写）

    @TableField("waypoint_name") // MyBatis-Plus注解，指定数据库字段名
    private String waypointName; // 航点名称（航线+唯一航点）

    @TableField("wayline_id") // MyBatis-Plus注解，指定数据库字段名
    private String waylineId; // 航线id

    @TableField("waypoint_sequence") // MyBatis-Plus注解，指定数据库字段名
    private String waypointSequence; // 航点序列1-1号航点第1张图片 1-1号航点第2张图片 1-1号航点第3种类型图片（1广角2变焦3红外）

    @TableField("tem_type") // MyBatis-Plus注解，指定数据库字段名
    private Integer temType; // 测温类型 1点测温 2区域测温

    @TableField("tem_conf") // MyBatis-Plus注解，指定数据库字段名
    private String temConf; // 测温配置
}
