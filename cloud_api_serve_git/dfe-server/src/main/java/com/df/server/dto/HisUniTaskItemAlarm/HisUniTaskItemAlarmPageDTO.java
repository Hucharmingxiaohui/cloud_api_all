package com.df.server.dto.HisUniTaskItemAlarm;


import com.df.framework.dto.PageDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 分页查询入参
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HisUniTaskItemAlarmPageDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskPatrolledId;
    /**
     * 告警等级:
     * <1>: = 预警
     * <2>: = 一般
     * <3>: = 严重
     * <4>: = 危急
     */
    private Integer alarmLevel;
    /**
     * 告警类型 :  字典 alarm_type
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
     * 告警是否确认  0-未确认 1-已确认
     */
    private Integer isConfirm;
    /**
     * 确认结果  0-误告警 1-告警
     */
    private Integer confirmResult;
    /**
     * 告警时间 开始
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date alarmTimeStart;
    /**
     * 告警时间  结束
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date alarmTimeEnd;
}
