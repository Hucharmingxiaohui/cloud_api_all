package com.df.server.dto.UniTaskPlan;


import lombok.Data;

import java.io.Serializable;

/**
 * 更新入参
 */
@Data
public class UniTaskPlanParamsDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private String subCode;
    /**
     * 任务计划编号
     */
    private String planNo;

    /**
     * 是否可用 0启用 1停用
     */
    private Integer isenable;

    /**
     * 是否审核 0未审核 1已审核
     */
    private Integer isaudit;

}
