package com.df.server.dto.UniTaskPlan;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 新增入参
 */
@Data
public class UniTaskPlanAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 巡视系统编码
     */
    private String sysCode;
    /**
     * 任务计划编号
     */
    private String planNo;
    /**
     * 任务计划名称
     */
    private String planName;
    /**
     * 计划类型多个逗号隔开
     * <1>: = 室外机器人
     * <2>: = 室内机器人
     * <3>: = 挂轨机器人
     * <4>: = 四足机器人
     * <5>: = 无人机
     */
    private String planType;
    /**
     * 巡检任务类型 1例行巡视 2特殊巡检 3专项巡检 4自定义巡检
     */
    private Integer taskType;
    /**
     * 巡视任务子类型拼接字符串，逗号分隔，根据task_type不同，对应字典表task_sub_type_3、task_sub_type_4、task_sub_type_5
     */
    private String taskSubType;
    /**
     * 定期开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date fixedStartTime;
    /**
     * 设备层级，1间隔 2主设备 3设备点位 4设备部件
     */
    private Integer deviceLevel;
    /**
     * 设备列表，多个采用逗号分割
     */
    private String deviceList;
    /**
     * 周期(月)
     */
    private String cycleMonth;
    /**
     * 周期(周)
     */
    private String cycleWeek;
    /**
     * 周期(日)
     */
    private String cycleDay;
    /**
     * 周期类型 1按周执行 2按日执行
     */
    private Integer cycleType;
    /**
     * 周期(执行时间)
     */
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private Date cycleExecuteTime;
    /**
     * 周期开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cycleStartTime;
    /**
     * 周期结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cycleEndTime;
    /**
     * 间隔(数量)
     */
    private String intervalNumber;
    /**
     * 间隔(类型)  1小时 2天
     */
    private String intervalType;
    /**
     * 间隔(执行时间)
     */
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private Date intervalExecuteTime;
    /**
     * 间隔开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date intervalStartTime;
    /**
     * 间隔结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date intervalEndTime;
    /**
     * 不可用开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date invalidStartTime;
    /**
     * 不可用结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date invalidEndTime;
    /**
     * 任务优先级 1(最低),2,3,4(最高)
     */
    private Integer priority;
    /**
     * 是否可用 0启用 1停用
     */
    private Integer isenable = 0;
    /**
     * 编制人
     */
    private String creator;
    /**
     * 任务执行方式 1立即执行 2定时执行 3周期执行
     */
    private Integer executeType;
    /**
     * 是否审核 0未审核 1已审核
     */
    private Integer isaudit = 1;
    /**
     * 插入到df_his_uni_task表的时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastExecuteTime;
    /**
     * 机器人任务是否二次分析 0否 1是
     */
    private Integer isSecondAnalyse = 1;
    /**
     * 任务来源：0本地创建 1上级下发
     */
    private Integer createSource;
}
