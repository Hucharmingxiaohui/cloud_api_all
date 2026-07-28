package com.dji.sample.df.cqDockDf.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("df_cq_dock_task_record")
public class CqDockTaskRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上级下发任务业务ID，用于和EUA任务建立映射关系 */
//  对应的其实是巡视的计划编码plan_no
    @TableField("business_id")
    private String businessId;

    @TableField("task_name")
    private String taskName;

    @TableField("bay_id")
    private String bayId;

    @TableField("route_id")
    private String routeId;

    /** EUA平台返回的任务ID，后续查询任务状态、结果时使用 */
    @TableField("eua_task_id")
    private String euaTaskId;

    @TableField("response_code")
    private Integer responseCode;

    @TableField("response_msg")
    private String responseMsg;

    @TableField("success")
    private Integer success;

    @TableField("raw_response")
    private String rawResponse;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
