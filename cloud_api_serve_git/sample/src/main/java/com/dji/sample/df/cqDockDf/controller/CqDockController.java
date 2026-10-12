package com.dji.sample.df.cqDockDf.controller;

import com.dji.sample.df.cqDockDf.model.dto.CqApiResponse;
import com.dji.sample.df.cqDockDf.model.dto.CqAssignTaskRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqPictureReportTestRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqTaskIdRequest;
import com.dji.sample.df.cqDockDf.service.CqDockApiService;
import com.dji.sample.df.cqDockDf.service.CqDockPictureReportService;
import com.dji.sample.df.uavCommonHandleDf.handler.CenterTaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 重庆电力下级无人机平台对接接口（转发调用下级 HTTP）
 */
@Slf4j
@RestController
@RequestMapping("/api/cqDock")
public class CqDockController {

    @Resource
    private CqDockApiService cqDockApiService;

    @Resource
    private CqDockPictureReportService cqDockPictureReportService;

    @Resource
    private CenterTaskHandler centerTaskHandler;

    /**
     * 机场任务下发
     */
    @PostMapping("/task/assign")
    public CqApiResponse assignTask(@RequestBody CqAssignTaskRequest request) {
        log.info("[cq-dock] controller assignTask body={}", request);
        return cqDockApiService.assignTask(request);
    }

    /**
     * 图片列表获取
     */
    @PostMapping("/picture/list")
    public CqApiResponse pictureList(@RequestBody CqTaskIdRequest request) {
        log.info("[cq-dock] controller pictureList taskId={}", request.getTaskId());
        return cqDockApiService.pictureList(request.getTaskId());
    }

    /**
     * 任务状态查询
     */
    @PostMapping("/task/status")
    public CqApiResponse taskStatus(@RequestBody CqTaskIdRequest request) {
        log.info("[cq-dock] controller taskStatus taskId={}", request.getTaskId());
        return cqDockApiService.taskStatus(request.getTaskId());
    }

    /**
     * 手动测试EUA图片结果上报链路，不改变任务状态轮询等原有流程。
     */
    @PostMapping("/test/picture/report")
    public CqApiResponse testPictureReport(@RequestBody CqPictureReportTestRequest request) {
        log.info("[cq-dock] controller testPictureReport body={}", request);
        CqApiResponse response = new CqApiResponse();
        if (!StringUtils.hasText(request.getTaskCode()) || !StringUtils.hasText(request.getEuaTaskId())) {
            response.setCode(400);
            response.setSuccess(false);
            response.setMsg("taskCode和euaTaskId不能为空");
            return response;
        }
        cqDockPictureReportService.fetchSaveAndReport(request.getTaskCode(), request.getTaskName(), request.getEuaTaskId());
        response.setCode(200);
        response.setSuccess(true);
        response.setMsg("EUA图片列表获取、入库、本地保存、分析调度已触发");
        return response;
    }

    /**
     * 本地联调：模拟上级下发间隔任务（真实走 Redis 定时调度 -> CenterTaskHandler 60秒扫描 -> EUA下发）。
     * 入参：bayId（间隔ID，点位表的bay_id）、taskCode、taskName、delaySeconds（延迟执行秒数，默认5）。
     */
    @PostMapping("/test/schedule-task")
    public CqApiResponse scheduleTask(@RequestBody Map<String, Object> params) {
        log.info("[cq-dock] controller scheduleTask body={}", params);
        CqApiResponse response = new CqApiResponse();
        String bayId = params.get("bayId") == null ? null : String.valueOf(params.get("bayId"));
        String taskCode = params.get("taskCode") == null ? null : String.valueOf(params.get("taskCode"));
        String taskName = params.get("taskName") == null ? "本地联调EUA任务" : String.valueOf(params.get("taskName"));
        int delaySeconds = params.get("delaySeconds") == null ? 5 : Integer.parseInt(String.valueOf(params.get("delaySeconds")));
        if (!StringUtils.hasText(bayId) || !StringUtils.hasText(taskCode)) {
            response.setCode(400);
            response.setSuccess(false);
            response.setMsg("bayId和taskCode不能为空");
            return response;
        }
        String fixedStartTime = java.time.LocalDateTime.now().plusSeconds(delaySeconds)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        centerTaskHandler.addScheduledTask(5, taskCode, fixedStartTime, bayId, taskName);
        response.setCode(200);
        response.setSuccess(true);
        response.setMsg("定时任务已加入Redis调度，" + fixedStartTime + " 到点后由扫描器执行EUA下发");
        return response;
    }
}
