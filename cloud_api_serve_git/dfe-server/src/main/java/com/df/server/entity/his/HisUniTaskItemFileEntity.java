package com.df.server.entity.his;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 机器人巡检结果文件接口
 * <p>
 * Created by lianyc on 2025-05-21
 */
@Data
public class HisUniTaskItemFileEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId
    private Integer id;
    /**
     * df_his_uni_task_item_points表的request_id
     */
    private String requestId;
    /**
     * 文件创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date fileTime;
    /**
     * 文件编号，任务条目内从1开始顺序编号
     */
    private Integer fileNo;
    /**
     * 采集文件类型：1红外图谱 2可见光图片 3音频 4视频
     * 5识别图片
     */
    private String fileType;
    /**
     * 采集文件路径
     */
    private String filePath;
    /**
     * 智能分析图片路径
     */
    private String recgFilePath;
    /**
     * 压缩图片路径
     */
    private String recgFilePathPress;
    /**
     * 图像框，格式：x1,y1;x2,y2;x3,y3;x4,y4
     */
    private String rectangle;
    /**
     * 当file_type是1时，这个字段存储可见光抓拍照片路径
     */
    private String filePathOther;
    /**
     * 因这个抓拍图也需要展示到报告，所以需要压缩
     */
    private String filePathOtherPress;

}
