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
//  本地存储图片绝对路径
    @TableField("local_path")
    private String localPath;

    @TableField("ftp_path")
    private String ftpPath;

    /**
     * EUA图片列表 pointId，对应 df_uni_point.point_code。
     */
    @TableField("point_id")
    private String pointId;

    @TableField("point_name")
    private String pointName;

    /**
     * 图片类型 0可见光 1红外（EUA图片列表 pointType）
     */
    @TableField("point_type")
    private Integer pointType;

    /**
     * 发送给智能分析服务的请求ID（可见光回调关联）
     */
    @TableField("request_id")
    private String requestId;

    /**
     * 分析状态 0待分析 1已分析 2分析失败 3红外测温完成
     */
    @TableField("analysis_status")
    private Integer analysisStatus;

    /**
     * 分析结果图地址（可见光回调 resImageUrl）
     */
    @TableField("analysis_image_url")
    private String analysisImageUrl;

    /**
     * 分析/测温结论值
     */
    @TableField("point_val")
    private String pointVal;

    /**
     * 结论单位或失败描述
     */
    @TableField("point_val_unit")
    private String pointValUnit;

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
