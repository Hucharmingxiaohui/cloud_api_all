package com.df.server.dto.HisUniTaskItemPoints;

import lombok.Data;

@Data
public class HisUniTaskItemPointsParamsDTO {

    /**
     * 巡视任务执行ID
     */
    private String taskPatrolledId;
    /**
     * 点位编码
     */
    private String pointCode;

}
