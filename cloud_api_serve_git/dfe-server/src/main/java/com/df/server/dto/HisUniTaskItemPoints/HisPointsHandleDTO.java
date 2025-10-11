package com.df.server.dto.HisUniTaskItemPoints;

import lombok.Data;

@Data
public class HisPointsHandleDTO {

    /**
     * 巡视任务执行ID
     */
    private String taskPatrolledId;
    /**
     * 点位编码
     */
    private String pointCode;
    /**
     * 修正值
     */
    private String setVal;
    /**
     * 结果描述
     */
    private String pointValueUnit;

    /**
     * 告警确认 描述
     */
    private String alarmConfirmComment;
    /**
     * 告警确认 结果  0-误告警 1-告警
     */
    private Integer confirmResult;

}
