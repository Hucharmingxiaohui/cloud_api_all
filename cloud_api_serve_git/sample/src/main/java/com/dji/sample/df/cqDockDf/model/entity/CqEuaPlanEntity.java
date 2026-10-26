package com.dji.sample.df.cqDockDf.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@TableName("df_cq_eua_plan")
public class CqEuaPlanEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("plan_name")
    private String planName;

    @TableField(value = "bay_id", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.IGNORED)
    private String bayId;

    @TableField(value = "bay_name", updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.IGNORED)
    private String bayName;

    @TableField("route_id")
    private String routeId;

    @TableField("route_name")
    private String routeName;

    @TableField("sub_code")
    private String subCode;

    @TableField("point_codes")
    private String pointCodes;

    @TableField(exist = false)
    private java.util.List<Long> pointIds;

    @TableField(exist = false)
    private Integer pointCount;

    @TableField(exist = false)
    private boolean requiresPointSelection;

    @TableField("remark")
    private String remark;

    @TableField("enabled")
    private Integer enabled;

    @TableField("last_task_code")
    private String lastTaskCode;

    @TableField(exist = false)
    private String lastTaskName;

    @TableField("last_eua_task_id")
    private String lastEuaTaskId;

    @TableField("last_execute_time")
    private Date lastExecuteTime;

    @TableField("last_execute_status")
    private Integer lastExecuteStatus;

    @TableField("last_execute_msg")
    private String lastExecuteMsg;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
