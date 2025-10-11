package com.df.server.dto.HisUniTask;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 更新入参
 */
@Data
public class HisUniTaskUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Integer id;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 方案编码
     */
    private String taskCode;
    /**
     * 方案名称
     */
    private String taskName;
    /**
     * 巡视任务执行ID
     */
    private String taskPatrolledId;
    /**
     * 任务计划编码
     */
    private String planNo;
    /**
     * 任务类型 0机器人任务  1视频任务 2无人机任务(此字段暂时不用了)
     */
    private Integer planType;
    /**
     * 巡检任务类型： 1自定义巡检 2 例行巡检 3专项巡检 4特殊巡检 5 熄灯巡检
     */
    private Integer taskType;
    /**
     * 巡视任务子类型拼接字符串，逗号分隔，根据task_type不同，对应字典表task_sub_type_3、task_sub_type_4、task_sub_type_5
     */
    private String taskSubType;
    /**
     * 任务计划开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planStartTime;
    /**
     * 实际开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    /**
     * 实际结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /**
     * 任务状态: 1已执行 2 正在执行 3暂停  4 终止 5未执行 6超期
     */
    private Integer runState;
    /**
     * 任务进度百分比
     */
    private String taskProgress;
    /**
     * 任务预计剩余时间
     */
    private String taskEstimatedTime;
    /**
     * 任务描述
     */
    private String description;
    /**
     * 总点位数
     */
    private Integer allPointCnt;
    /**
     * 失败点位数
     */
    private Integer failPointCnt;
    /**
     * 正常点位数
     */
    private Integer normalPointCnt;
    /**
     * 异常点位数
     */
    private Integer exceptionPointCnt;
    /**
     * 告警点位数
     */
    private Integer warnPointCnt;
    /**
     * 检修中点位数
     */
    private Integer repairCnt;
    /**
     * 审核状态 0未审核 1已审核
     */
    private Integer isConfirm;
    /**
     * 确认人ID
     */
    private Integer confirmUsrNo;
    /**
     * 确认时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmDate;
    /**
     * 报告是否确认：0代表未确认 1代表已确认
     */
    private Integer reportOk;
}
