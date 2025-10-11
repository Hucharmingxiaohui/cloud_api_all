package com.dji.sample.df.electricInspectionDf.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import javax.persistence.Transient;
import java.io.Serializable;

@Data
@TableName("pub_wayline_job_plan_df")
public class PubWaylineJobPlanDfEntity  implements Serializable {
    @TableId(type = IdType.AUTO) // MyBatis-Plus注解，指定主键字段及其生成策略
    private Integer id; // 主键id

    @TableField("plan_id") // MyBatis-Plus注解，指定数据库字段名
    private String planId; // 计划ID

    @TableField("sub_code") // MyBatis-Plus注解，指定数据库字段名
    private String subCode; // 变电站编码

    @TableField("major") // MyBatis-Plus注解，指定数据库字段名
    private String major; // 专业

    @TableField("plan_source") // MyBatis-Plus注解，指定数据库字段名
    private String planSource; // 计划来源

    @TableField("name") // MyBatis-Plus注解，指定数据库字段名
    private String name; // 任务名称

    @TableField("file_id") // MyBatis-Plus注解，指定数据库字段名
    private String fileId; // 文件ID

    @TableField("dock_sn") // MyBatis-Plus注解，指定数据库字段名
    private String dockSn; // 机场序列号

    @TableField("workspace_id") // MyBatis-Plus注解，指定数据库字段名
    private String workspaceId; // 工作空间ID

    @TableField("task_type") // MyBatis-Plus注解，指定数据库字段名
    private Integer taskType; // 任务类型{"0":"立即任务","1":"定时任务","2":"条件任务"}

    @TableField("wayline_type") // MyBatis-Plus注解，指定数据库字段名
    private Integer waylineType; // 航线类型 WAYPOINT(0, "waypoint"),MAPPING_2D(1, "mapping2d"),MAPPING_3D(2, "mapping3d"),MAPPING_STRIP(3, "mappingStrip");

    @TableField("username") // MyBatis-Plus注解，指定数据库字段名
    private String username; // 创建者用户名

    @TableField("begin_time") // MyBatis-Plus注解，指定数据库字段名
    private Long beginTime; // 计划开始时间

    @TableField("end_time") // MyBatis-Plus注解，指定数据库字段名
    private Long endTime; // 计划结束时间

    @TableField("execute_time")//开始飞行时间点
    private Long executeTime;

    @TableField("completed_time")//结束飞行时间点
    private Long completedTime;

    @TableField("rth_altitude") // MyBatis-Plus注解，指定数据库字段名
    private Integer rthAltitude; // 返航高度

    @TableField("out_of_control") // MyBatis-Plus注解，指定数据库字段名
    private Integer outOfControl; // 越控行为

    @TableField("create_time") // MyBatis-Plus注解，指定数据库字段名
    private Long createTime; // 创建时间

    @TableField("update_time") // MyBatis-Plus注解，指定数据库字段名
    private Long updateTime; // 更新时间

    @TableField("parent_id") // MyBatis-Plus注解，指定数据库字段名
    private String parentId; // 父ID

    @TableField("enable_status") // MyBatis-Plus注解，指定数据库字段名
    private Integer enableStatus; // 启用状态

    @TableField("plan_priority") // MyBatis-Plus注解，指定数据库字段名
    private Integer planPriority; // 计划优先级
    @TableField("task_days") // MyBatis-Plus注解，指定数据库字段名
    private String taskDays;
    @TableField("task_periods") // MyBatis-Plus注解，指定数据库字段名
    private String taskPeriods;
    @Range(min = 50, max = 90)
    @TableField("min_battery_capacity")
    private Integer minBatteryCapacity;
    @TableField("min_storage_capacity")
    private Integer minStorageCapacity;

    @TableField("wayline_point_pos") // 航点预置位号
    private String waylinePointPos;

    @Transient
    @TableField(exist = false)
    private String deviceList;

    @Transient
    @TableField(exist = false)
    private Integer deviceLevel;
}
