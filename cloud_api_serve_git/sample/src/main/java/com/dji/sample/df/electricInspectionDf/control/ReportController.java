package com.dji.sample.df.electricInspectionDf.control;

import com.df.framework.config.FileConfig;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.Result;
import com.df.server.dto.HisUniTask.ConfirmHisTaskReportParams;
import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;
import com.df.server.entity.his.HisUniTaskReportEntity;
import com.df.server.service.his.HisUniTaskReportService;
import com.dji.sample.df.electricInspectionDf.service.ReportService;
import com.dji.sample.df.electricInspectionDf.timer.ReportGenTimer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("report/api/v1/")
public class ReportController {
    @Autowired
    ReportService reportService;
    @Autowired
    private HisUniTaskReportService hisUniTaskReportService;
    @Autowired
    private FileConfig fileConfig;
    /**
     * 查看巡视报告
     */
    @PostMapping("/lookReport")
    public Result<TaskReportDTO> lookReport(@RequestBody HisUniTaskParamsDTO params) {
        ParamsUtils.isBlank(params, "taskPatrolledId");
        TaskReportDTO dto = reportService.lookReport(params);
        return Result.success(dto);
    }

    /**
     * 巡视报告界面接口-巡视报告生成
     */
    @PostMapping("/createTaskReport")
    public Result createHisTaskReport(@RequestBody ConfirmHisTaskReportParams params) {
        ParamsUtils.isBlank(params, "taskPatrolledId", "fileType", "imageNeed");
        Integer id = hisUniTaskReportService.createNewReport(params);
        ReportGenTimer.putMap(id, params);
        log.info("创建巡视报告记录，排队生成报告，id:{} params {}", id, params);
        return Result.success(id);
    }

    /**
     * 获取报告下载地址
     */
    @PostMapping("/checkDownloadReport")
    public Result checkDownloadReport(@RequestBody ConfirmHisTaskReportParams params) {
        HisUniTaskReportEntity report = hisUniTaskReportService.getByTaskId(params.getTaskPatrolledId());
        if (report == null) {
            return Result.error("报告不存在,请先生成报告");
        }
        if (report.getReportStatus() == null || report.getReportStatus() == 1) {
            return Result.error("请先生成报告");
        }
        if (report.getReportStatus() == 2) {
            return Result.error("报告生成中，请稍等");
        }
        if (report.getReportStatus() == 3 && StringUtils.isNotBlank(report.getWordFile())) {
            String wordFile = report.getWordFile();
            String url = fileConfig.getFileVisitUrl(wordFile);
            return Result.success(url, "");
        }
        return Result.error("报告生活才能异常，请重新生成报告");
    }
}
