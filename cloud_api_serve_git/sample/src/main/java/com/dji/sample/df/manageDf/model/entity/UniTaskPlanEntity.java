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
import java.util.Date;

/**
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("uni_task_plan_df")
public class UniTaskPlanEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField(value = "sub_code")
    private String subCode;

    @TableField(value = "sys_code")
    private String sysCode;

    @TableField(value = "plan_no")
    private String planNo;

    @TableField(value = "plan_name")
    private String planName;

    @TableField(value = "task_type")
    private Integer taskType;

    @TableField(value = "task_sub_type")
    private String taskSubType;

    @TableField(value = "fixed_start_time")
    private Date fixedStartTime;

    @TableField(value = "device_level")
    private Integer deviceLevel;

    @TableField(value = "device_list")
    private String deviceList;

    @TableField(value = "cycle_month")
    private String cycleMonth;

    @TableField(value = "cycle_week")
    private String cycleWeek;

    @TableField(value = "cycle_day")
    private String cycleDay;

    @TableField(value = "cycle_type")
    private Integer cycleType;

    @TableField(value = "cycle_execute_time")
    private Date cycleExecuteTime;

    @TableField(value = "cycle_start_time")
    private Date cycleStartTime;

    @TableField(value = "cycle_end_time")
    private Date cycleEndTime;

    @TableField(value = "interval_number")
    private String intervalNumber;

    @TableField(value = "interval_type")
    private String intervalType;

    @TableField(value = "interval_execute_time")
    private Date intervalExecuteTime;

    @TableField(value = "interval_start_time")
    private Date intervalStartTime;

    @TableField(value = "interval_end_time")
    private Date intervalEndTime;

    @TableField(value = "invalid_start_time")
    private Date invalidStartTime;

    @TableField(value = "invalid_end_time")
    private Date invalidEndTime;

    @TableField(value = "priority")
    private Integer priority;

    @TableField(value = "isenable")
    private Integer isenable;

    @TableField(value = "creator")
    private String creator;

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(value = "execute_type")
    private Integer executeType;

    @TableField(value = "histask_insert_time")
    private Date histaskInsertTime;

    @TableField(value = "point_list")
    private String pointList;

    @TableField(value = "wayline_list")
    private String waylineList;
}