package com.dji.sample.df.supControlDf.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class DroneOsd implements Serializable {
    private Integer capacityPercent;//电量百分数
    private Integer rtkNum;//rtk数量
    private Integer gpsNum;//gps数量
    private Double horizontalSpeed;//水平速度
    private Double verticalSpeed;//垂直速度
    private Double windSpeed;//风速
    private Double longitude;//经度
    private Double latitude;//纬度
    private Double AsHeight;//海拔高度
    private Double AlHeight;//距离起飞点高度
    private Double home_distance;//离起飞点距离
}
