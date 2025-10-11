package com.df.server.dto.HisUniTaskItemAlarm;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 更新入参
 */
@Data
public class HisUniTaskItemAlarmUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private Integer id;
    private String requestId;
    /**
     * 巡检任务执行id
     */
    private String taskPatrolledId;
    /**
     *
     */
    private String pointCode;
    /**
     *
     */
    private String taskCode;
    /**
     * 计划编号
     */
    private String planNo;
    /**
     *
     */
    private String subCode;
    /**
     * 巡视设备编码
     */
    private String patroldeviceCode;
    /**
     * 告警时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date alarmTime;
    /**
     * 告警等级:
     * <1>: = 预警
     * <2>: = 一般
     * <3>: = 严重
     * <4>: = 危急
     */
    private Integer alarmLevel;
    /**
     * 告警类型 :
     * <1>: = 超温报警
     * <2>: = 温升报警
     * <3>: = 三相温差报警
     * <4>: = 三相对比报警
     * <5>: = 声音异常
     * <6>: = 外观异常
     * <7>: = 仪表越限报警
     * <8>: = 仪表超量程报警
     * <9>: = 仪表三相对比
     * <10>: = 变位报警
     */
    private Integer alarmType;
    /**
     * 识别类型 :
     * <1>: = 表计读取
     * <2>: = 位置状态识别
     * <3>: = 设备外观查看
     * <4>: = 红外测温
     * <5>: = 声音检测
     * <6>: = 闪烁检测
     */
    private Integer recognitionType;
    /**
     * 值
     */
    private String value;
    /**
     * 值带单位
     */
    private String valueUnit;
    /**
     * 单位
     */
    private String unit;
    /**
     * 告警描述
     */
    private String alarmContent;
    /**
     * 告警是否确认  0-未确认 1-已确认 2-转缺陷
     */
    private Integer isConfirm;
    /**
     * 确认结果  0-误告警 1-告警
     */
    private Integer confirmResult;
    /**
     * 确认人
     */
    private String confirmUser;
    /**
     *
     */
    private String confirmComment;
    /**
     * 是否转缺陷 0否 1是（不用了）
     */
    private Integer isDefect;
    /**
     * 告警次数
     */
    private Integer alarmFrequency;
    /**
     *
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmTime;
    /**
     * 实物ID
     */
    private String objId;
}
