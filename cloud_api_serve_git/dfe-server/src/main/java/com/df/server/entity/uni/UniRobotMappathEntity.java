package com.df.server.entity.uni;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 机器人巡检路线接口
 * <p>
 * Created by lianyc on 2025-05-26
 */
@Data
@TableName("df_uni_robot_mappath")
public class UniRobotMappathEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId
    private Integer id;
    /**
     * 机器人编码
     */
    private String robotCode;
    /**
     * 机器人名称
     */
    private String robotName;
    /**
     * 路线节点序号，同一巡检路线从1开始累加
     */
    private Integer nodeNo;
    /**
     * 对应时间，格式为 yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nodeTime;
    /**
     * 坐标框（像素点），格式：x,y,z,a，x、y、z 为地图
     * 文件的坐标，a 为机器人航向角
     */
    private String coordinatePixel;

}
