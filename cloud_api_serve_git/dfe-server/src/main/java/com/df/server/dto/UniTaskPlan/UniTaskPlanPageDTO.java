package com.df.server.dto.UniTaskPlan;


import com.df.framework.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询入参
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UniTaskPlanPageDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 方案名称
     */
    private String planName;
    /**
     * 巡视类型 字典 task_type
     */
    private String taskType;
    /**
     * 任务执行方式 1立即执行 2定时执行 3周期执行
     */
    private String executeType;
    /**
     * 方案编码
     */
    private String planNo;
    /**
     * 任务优先级 1(最低),2,3,4(最高)
     */
    private Integer priority;
    /**
     * 是否可用 0启用 1停用
     */
    private Integer isenable;
    /**
     * 是否审核 0未审核 1已审核
     */
    private Integer isaudit;
    /**
     * 任务来源：0本地创建 1上级下发
     */
    private Integer createSource;

}
