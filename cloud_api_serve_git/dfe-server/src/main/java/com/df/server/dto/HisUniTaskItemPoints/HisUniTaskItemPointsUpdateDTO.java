package com.df.server.dto.HisUniTaskItemPoints;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 更新入参
 */
@Data
public class HisUniTaskItemPointsUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private Integer id;
    /**
     * 任务执行ID，格式为：任务编码_任务执行开始时间
     */
    private String taskPatrolledId;
    /**
     * 任务计划编码
     */
    private String planNo;
    /**
     * 点位编码
     */
    private String pointCode;
    /**
     * 点位名称
     */
    private String pointName;
    /**
     * 任务总编码
     */
    private String taskCode;
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 巡视设备编码
     */
    private String patroldeviceCode;
    /**
     * 巡视设备名称
     */
    private String patroldeviceName;
    /**
     * 实物ID
     */
    private String objId;
    /**
     * 巡视时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date runTime;
    /**
     * 数据来源：1摄像机 2机器人 3无人机 4声纹 5在线监测
     */
    private Integer dataType;
    /**
     * 识别结果值
     */
    private String pointVal;
    /**
     * 识别结果值带单位(识别结果描述)
     */
    private String pointValUnit;
    /**
     * 识别结果单位
     */
    private String pointUnit;
    /**
     * 值类型 0-默认值类型 11-局放放电频次 12-局放信号峰值 13-局放信号均值
     */
    private Integer valueType;
    /**
     * 识别类型，详见字典表类型recognition_type。
     * 1表计读取
     * 2位置状态识别
     * 3设备外观查看
     * 4红外测温
     * 5声音检测
     * 6闪烁检测
     */
    private String recognitionType;
    /**
     * 数据结果：0 失败(抓拍超时、分析超时、分析失败、没有结果) 1正常 2异常（点位分析判别异常、阈值告警）
     */
    private Integer valid;
    /**
     * 是否报警 0 否 1 是
     */
    private Integer isAlarm;
    /**
     * 人工处理：0待确认 1识别正常 2识别异常
     */
    private Integer manualHand;
    /**
     * 人工修正结果
     */
    private String setVal;
    /**
     * 点位是否巡视完成，视频任务判断用  0否 1是
     */
    private Integer isFinished;
    /**
     * 审核人姓名
     */
    private String confirmUser;
    /**
     * 人工识别异常描述
     */
    private String confirmResult;
    /**
     * 审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmTime;
    /**
     * 点位关联的预置位号
     */
    private String presetNo;
}
