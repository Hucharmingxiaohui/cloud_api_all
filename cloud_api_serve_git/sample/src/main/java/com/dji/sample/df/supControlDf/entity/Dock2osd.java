package com.dji.sample.df.supControlDf.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class Dock2osd implements Serializable {
    private Long accumulatedRunningTime;//累计运行时间
    private Long activationTime;//激活时间
    private Double networkState;//网络状态 kb/s
    private Integer  missions;//累计执行任务次数
    private Integer remainFiles;//剩余上传文件数量
    private Double windSpeed;//风速
    private Integer rainFull;//是否在下雨 0 没有 1下雨
    private Double environmentTemperature;//环境温度
    private Double dockTemperature;//机场温度
    private Double humidity;//湿度
    private Double workingVoltage;//工作电压
    private Double workingCurrent;//工作电流
    private Integer droneInDock;//0仓位1仓内
    private Double longitude;//精度180 -180
    private Double latitude;//维度
    private Double height;//机场海拔高度96系

}
