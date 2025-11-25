package com.dji.sample.df.wind.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("fan_wayline_points")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FanWaylinePoints {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("dji_fan_points")
    private String djiFanPoints;

    @TableField("video_fan_points")
    private String videoFanPoints;

    @TableField("job_type")
    private Integer jobType;

    @TableField("job_id")
    private String jobId;

}
