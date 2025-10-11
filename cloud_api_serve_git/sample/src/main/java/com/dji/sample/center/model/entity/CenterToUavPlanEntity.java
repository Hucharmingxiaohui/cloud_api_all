package com.dji.sample.center.model.entity;

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
 *
 * @author sean.zhou
 * @date 2021/11/15
 * @version 0.1
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("center_to_uav_plan_df")
public class CenterToUavPlanEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField(value = "sub_code")
    private String subCode;

    @TableField(value = "center_plan_code")
    private String centerPlanCode;

    @TableField(value = "uav_plan_code")
    private String uavPlanCode;

    @TableField(value = "center_task_patrolled_id")
    private String centerTaskPatrolledId;

    @TableField(value = "wayline")
    private String wayline;

    @TableField(value = "status")
    private Integer status;

    @TableField(value = "start_time")
    private Date startTime;

    @TableField(value = "end_time")
    private Date endTime;
}