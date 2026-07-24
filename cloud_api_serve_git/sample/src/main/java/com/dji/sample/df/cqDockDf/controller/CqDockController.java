package com.dji.sample.df.cqDockDf.controller;

import com.dji.sample.df.cqDockDf.model.dto.CqApiResponse;
import com.dji.sample.df.cqDockDf.model.dto.CqAssignTaskRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqTaskIdRequest;
import com.dji.sample.df.cqDockDf.service.CqDockApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 重庆电力下级无人机平台对接接口（转发调用下级 HTTP）
 */
@Slf4j
@RestController
@RequestMapping("/api/cqDock")
public class CqDockController {

    @Resource
    private CqDockApiService cqDockApiService;

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
}
