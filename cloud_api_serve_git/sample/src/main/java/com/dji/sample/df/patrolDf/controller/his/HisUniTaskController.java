package com.dji.sample.df.patrolDf.controller.his;

import com.df.framework.config.FileConfig;
import com.df.framework.exception.FastException;
import com.df.framework.utils.PageUtils;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.PageVO;
import com.df.framework.vo.Result;
import com.df.server.dto.HisUniTask.ConfirmHisTaskReportParams;
import com.df.server.dto.HisUniTask.HisUniTaskPageDTO;
import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;
import com.df.server.entity.his.HisUniTaskReportEntity;
import com.df.server.service.his.HisUniTaskReportService;
import com.df.server.service.his.HisUniTaskService;
import com.df.server.vo.his.HisUniTaskVO;
import com.dji.sample.component.AuthInterceptor;
import com.dji.sample.df.patrolDf.timer.ExecuteReportGenTimer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 任务结果
 *
 * @date 2025-04-22 12:12
 */
@Slf4j
@RestController
@RequestMapping("/his/task")
@AuthInterceptor.IgnoreAuth
public class HisUniTaskController {
    @Autowired
    private HisUniTaskService hisUniTaskService;
    @Autowired
    private HisUniTaskReportService hisUniTaskReportService;
    @Autowired
    private FileConfig fileConfig;

    /**
     * 分页查询
     *
     * @param pageDTO
     * @return
     */
    @PostMapping("/getList")
    public Result<PageVO<HisUniTaskVO>> getList(@RequestBody HisUniTaskPageDTO pageDTO) {
        PageUtils.pageParamsExtend(pageDTO);
        PageVO<HisUniTaskVO> pageVO = hisUniTaskService.getPageList(pageDTO);
        return Result.success(pageVO);
    }

    /**
     * 单个查询
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/getInfo")
    public Result<HisUniTaskVO> getInfo(@RequestBody HisUniTaskParamsDTO paramsDTO) {
        ParamsUtils.isBlank(paramsDTO, "taskPatrolledId");
        HisUniTaskVO hisUniTaskVO = hisUniTaskService.getInfo(paramsDTO.getTaskPatrolledId());
        return Result.success(hisUniTaskVO);
    }

    /**
     * 改为最新的一个任务（不管状态）
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/getRunningTask")
    public Result<HisUniTaskVO> getShowTask(@RequestBody HisUniTaskParamsDTO paramsDTO) {
        HisUniTaskVO hisUniTaskVO = hisUniTaskService.getShowTask();
        return Result.success(hisUniTaskVO);
    }

    /**
     * 暂停任务
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/pauseTask")
    public Result pauseTask(@RequestBody HisUniTaskParamsDTO paramsDTO, HttpServletRequest request) {
        ParamsUtils.isNull(paramsDTO, "taskPatrolledId");
        try {
            hisUniTaskService.pauseTask(paramsDTO.getTaskPatrolledId());
            return Result.success();
        } catch (Exception e) {
            if (e instanceof FastException) {
                return Result.error(e.getMessage());
            }
            e.printStackTrace();
            return Result.error("暂停失败");
        }
    }

    /**
     * 手动恢复任务
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/recoverTask")
    public Result recoverTask(@RequestBody HisUniTaskParamsDTO paramsDTO, HttpServletRequest request) {
        ParamsUtils.isNull(paramsDTO, "taskPatrolledId");
        try {
            hisUniTaskService.recoverTask(paramsDTO.getTaskPatrolledId());
            return Result.success();
        } catch (Exception e) {
            if (e instanceof FastException) {
                return Result.error(e.getMessage());
            }
            e.printStackTrace();
            return Result.error("恢复失败");
        }

    }

    /**
     * 终止任务
     *
     * @param paramsDTO
     * @return
     */
    @PostMapping("/stopTask")
    public Result stopTask(@RequestBody HisUniTaskParamsDTO paramsDTO, HttpServletRequest request) {
        ParamsUtils.isNull(paramsDTO, "taskPatrolledId");
        try {
            hisUniTaskService.stopTask(paramsDTO.getTaskPatrolledId());
            return Result.success();
        } catch (Exception e) {
            if (e instanceof FastException) {
                return Result.error(e.getMessage());
            }
            e.printStackTrace();
            return Result.error("终止失败");
        }

    }

    /**
     * 查看巡视报告
     */
    @PostMapping("/lookReport")
    public Result<TaskReportDTO> lookReport(@RequestBody HisUniTaskParamsDTO params) {
        ParamsUtils.isBlank(params, "taskPatrolledId");
        TaskReportDTO dto = hisUniTaskService.lookReport(params);
        return Result.success(dto);
    }

    /**
     * 巡视报告界面接口-巡视报告生成
     */
    @PostMapping("/createTaskReport")
    public Result createHisTaskReport(@RequestBody ConfirmHisTaskReportParams params) {
        ParamsUtils.isBlank(params, "taskPatrolledId", "fileType", "imageNeed");
        Integer id = hisUniTaskReportService.createNewReport(params);
        ExecuteReportGenTimer.putMap(id, params);
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
