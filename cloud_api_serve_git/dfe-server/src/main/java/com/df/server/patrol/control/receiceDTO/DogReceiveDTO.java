package com.df.server.patrol.control.receiceDTO;

import lombok.Data;

import java.io.Serializable;

/**
 * 接收机器狗数据
 */
@Data
public class DogReceiveDTO implements Serializable {

    private String robotCode;

    /**
     * 电池电量
     */
    private Integer battery_soc;
    /**
     * 电机温度
     */
    private Integer max_temperature;
    /**
     * 执行任务状态
     * 0 空闲  1运动  2抓拍
     */
    private Integer state;
    /**
     * 机器人图中实时像素坐标X
     */
    private Integer x;
    /**
     * 机器人图中实时像素坐标Y
     */
    private Integer y;
}
