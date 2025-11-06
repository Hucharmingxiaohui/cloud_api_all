package com.dji.sample.df.wind.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 缺陷实体类
 */
@Data
@TableName("defect_file")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DefectEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private String id; // 主键ID

    /**
     * 报告id
     */
    @TableField("report_id")
    private String reportId;

    /**
     * 任务id
     */
    @TableField("job_id")
    private String jobId;

    /**
     * 风机编号
     */
    @TableField("fan_code")
    private String fanCode;

    /**
     * 风机部位
     */
    @TableField("fan_part")
    private String fanPart;

    /**
     * 采集时间
     */
    @TableField("acquisition_time")
    private String acquisitionTime;

    /**
     * 缺陷类型
     */
    @TableField("defect_type")
    private String defectType;

    /**
     * 缺陷描述
     */
    @TableField("defect_description")
    private String defectDescription;

    /**
     * 图片路径
     */
    @TableField("image_path")
    private String imagePath;

    /**
     * 是否缺陷
     */
    @TableField("is_defect")
    private Integer isDefect;

}
