package com.df.server.dto.HisUniTask;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 巡视报告界面接口-巡视报告审核 传参
 *
 * @author 姜学云
 * @Time 2022/3/21 10:31
 */
@Data
public class ConfirmHisTaskReportParams {

    /**
     * 巡视任务执行ID
     */
    private String taskPatrolledId;

    /**
     * 导出文件类型 1word  2excel
     */
    private String fileType;
    /**
     * 导出图片类型 1 全图片 2 异常大图 3 不导出大图
     */
    private Integer imageNeed;
}
