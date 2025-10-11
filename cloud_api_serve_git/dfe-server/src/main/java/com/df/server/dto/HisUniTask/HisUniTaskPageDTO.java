package com.df.server.dto.HisUniTask;


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
public class HisUniTaskPageDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 方案编码
     */
    private String planNo;
    /**
     * 方案名称
     */
    private String taskName;
    /**
     * 巡检任务类型： 1自定义巡检 2 例行巡检 3专项巡检 4特殊巡检 5 熄灯巡检
     */
    private Integer taskType;
    /**
     * 任务状态: 1已执行 2 正在执行 3暂停  4 终止 5未执行 6超期
     */
    private Integer runState;
    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
