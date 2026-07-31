package com.dji.sample.df.cqDockDf.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("df_cq_dock_hms")
public class CqDockHmsEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("unit_code")
    private String unitCode;

    @TableField("topic_dock_sn")
    private String topicDockSn;

    @TableField("message_id")
    private String messageId;

    @TableField("alarm_code")
    private String alarmCode;

    @TableField("dock_sn")
    private String dockSn;

    @TableField("uav_sn")
    private String uavSn;

    @TableField("gateway_type")
    private String gatewayType;

    @TableField("imminent")
    private Boolean imminent;

    @TableField("in_the_sky")
    private Boolean inTheSky;

    @TableField("level")
    private Integer level;

    @TableField("module")
    private Integer module;

    @TableField("message")
    private String message;

    @TableField("raw_data")
    private String rawData;

    @TableField("report_status")
    private Integer reportStatus;

    @TableField("report_msg")
    private String reportMsg;

    @TableField("report_time")
    private Date reportTime;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
