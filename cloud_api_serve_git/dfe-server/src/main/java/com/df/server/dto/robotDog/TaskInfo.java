package com.df.server.dto.robotDog;

import lombok.Data;

@Data
public class TaskInfo {

    //变电站编码
    private String subCode;
    //任务ID
    private String taskPatrolledId;
    //任务名称
    private String taskName;
    //计划编码
    private String planNo;
    //任务点位数量
    private Integer pointNum;

    public TaskInfo(String subCode, String taskPatrolledId, String taskName, String planNo, Integer pointNum) {
        this.subCode = subCode;
        this.taskPatrolledId = taskPatrolledId;
        this.taskName = taskName;
        this.planNo = planNo;
        this.pointNum = pointNum;
    }
}
