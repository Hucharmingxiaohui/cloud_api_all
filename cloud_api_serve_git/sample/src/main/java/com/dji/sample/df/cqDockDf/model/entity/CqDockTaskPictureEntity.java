package com.dji.sample.df.cqDockDf.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("df_cq_dock_task_picture")
public class CqDockTaskPictureEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("business_id")
    private String businessId;

    @TableField("task_name")
    private String taskName;

    @TableField("eua_task_id")
    private String euaTaskId;

    @TableField("picture_id")
    private String pictureId;

    @TableField("picture_name")
    private String pictureName;

    @TableField("picture_url")
    private String pictureUrl;

    @TableField("local_path")
    private String localPath;

    @TableField("ftp_path")
    private String ftpPath;

    @TableField("point_id")
    private String pointId;

    @TableField("point_name")
    private String pointName;

    @TableField("raw_data")
    private String rawData;

    @TableField("report_status")
    private Integer reportStatus;

    @TableField("report_msg")
    private String reportMsg;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
