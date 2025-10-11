package com.dji.sample.df.supControlDf.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("center_to_uav_plan_df")
public class CenterToUavPlanDfEntity implements Serializable {
    @TableId(type = IdType.AUTO) // MyBatis-Plus注解，指定主键字段及其生成策略
    private Integer id; // 主键id
    @TableField("sub_code") // MyBatis-Plus注解，指定数据库字段名
    private String subCode; // 变电站编码
    @TableField("center_plan_code")
    private String centerPlanCode;//无人机计划id,上级
    @TableField("uav_plan_code")
    private String uavPlanCode;//无人机计划\任务id，本地
    @TableField("center_task_patrolled_id")
    private String centerTaskPatrolledId;//无人机计划\任务id,用TaskUtils.genTaskPatrolledId(xxx)生成
    @TableField("wayline")
    private String wayline;//无人机计划\任务id
    @TableField("status")
    private Integer status;//状态 1：已执行；2：正在执行；3：暂停；4：终止；
    // 5：未执行；6：超期
    @TableField("start_time")
    private LocalDateTime startTime;//开始时间
    @TableField("end_time")
    private LocalDateTime endTime;//结束时间

}
