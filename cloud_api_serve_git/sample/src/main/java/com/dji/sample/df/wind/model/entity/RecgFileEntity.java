package com.dji.sample.df.wind.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;

/*
分析图片实体类
 */
@Data
@TableName("recg_file")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecgFileEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @TableId(type = IdType.INPUT)
    private Integer id;

    /**
     * df_his_uni_task_item_points表的request_id
     */
    @TableField("request_id")
    private String requestId;


    @TableField("task_patrolled_id")
    private String taskPatrolledId;

    @TableField("point_code")
    private String pointCode;

    /**
     * 文件创建时间
     */
    @TableField("file_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date fileTime;

    /**
     * 文件编号，任务条目内从1开始顺序编号
     */
    @TableField("file_no")
    private Integer fileNo;

    /**
     * 采集文件类型：1红外图谱 2可见光图片 3音频 4视频 5识别图片
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 采集文件路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 智能分析图片路径
     */
    @TableField("recg_file_path")
    private String recgFilePath;

    /**
     * 压缩图片路径
     */
    @TableField("recg_file_path_press")
    private String recgFilePathPress;

    /**
     * 图像框，格式：x1,y1;x2,y2;x3,y3;x4,y4
     */
    @TableField("rectangle")
    private String rectangle;

    /**
     * 当file_type是1时，这个字段存储可见光抓拍照片路径
     */
    @TableField("file_path_other")
    private String filePathOther;

    /**
     * 因这个抓拍图也需要展示到报告，所以需要压缩
     */
    @TableField("file_path_other_press")
    private String filePathOtherPress;

    /**
     * 点位关联的预置位号
     */
    @TableField("preset_no")
    private Integer presetNo;
}
