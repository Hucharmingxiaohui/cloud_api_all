package com.dji.sample.df.centerOrdinary;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("df_center_patrol_plan")
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class CenterPatrolPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String centerCode;
    private String subCode;
    private String taskCode;
    private String taskName;
    private String workspaceId;
    private Integer isenable;
    private String commandJson;
    private Long executionId;
}
