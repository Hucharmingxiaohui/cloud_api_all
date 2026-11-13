package com.dji.sample.df.centerOrdinary;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("df_center_ordinary_task")
public class CenterOrdinaryTaskEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String subCode;
    private String taskCode;
    private String taskName;
    private Long scheduledTime;
    private String fixedStartTime;
    private String status;
    private String routeId;
    private String planId;
    private String planConfig;
    private String jobId;
    private String pointCodes;
    private String pointSnapshot;
    private String resultSnapshot;
    private String message;
    private Long claimedTime;
    private String statusReportKey;
    private Long statusReportTime;
}
