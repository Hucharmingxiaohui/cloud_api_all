package com.dji.sample.df.cqDockDf.controller;

import com.df.framework.vo.Result;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaPlanAddRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaPlanExecuteRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaPlanPageRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaPlanUpdateRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaTaskPictureListRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaTaskRecordPageRequest;
import com.dji.sample.df.cqDockDf.service.CqEuaPlanService;
import com.dji.sample.df.cqDockDf.service.CqEuaTaskReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/cqDock/eua/plan")
public class CqEuaPlanController {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result> handleTaskReportStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Result.error(exception.getReason()));
    }

    @Resource
    private CqEuaPlanService cqEuaPlanService;

    @Resource
    private CqEuaTaskReportService cqEuaTaskReportService;

    @PostMapping("/task-report/generate")
    public Result generateTaskReport(@RequestBody CqEuaTaskPictureListRequest request) {
        return Result.success(cqEuaTaskReportService.generate(request));
    }

    @GetMapping("/task-report/status")
    public Result taskReportStatus(@RequestParam(required = false) String businessId,
                                   @RequestParam(required = false) String euaTaskId) {
        return Result.success(cqEuaTaskReportService.status(reportRequest(businessId, euaTaskId)));
    }

    @GetMapping("/task-report/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadTaskReport(
            @RequestParam(required = false) String businessId,
            @RequestParam(required = false) String euaTaskId) {
        org.springframework.core.io.Resource file = cqEuaTaskReportService.download(reportRequest(businessId, euaTaskId));
        String fileName = file.getFilename();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header("Content-Disposition", org.springframework.http.ContentDisposition.attachment()
                        .filename(fileName, java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(file);
    }

    private CqEuaTaskPictureListRequest reportRequest(String businessId, String euaTaskId) {
        CqEuaTaskPictureListRequest request = new CqEuaTaskPictureListRequest();
        request.setBusinessId(businessId);
        request.setEuaTaskId(euaTaskId);
        return request;
    }

    @PostMapping("/page")
    public Result page(@RequestBody CqEuaPlanPageRequest request) {
        return Result.success(cqEuaPlanService.page(request));
    }

    @PostMapping("/add")
    public Result add(@RequestBody CqEuaPlanAddRequest request) {
        try {
            return Result.success(cqEuaPlanService.add(request));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result update(@RequestBody CqEuaPlanUpdateRequest request) {
        try {
            return Result.success(cqEuaPlanService.update(request));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam Long id) {
        try {
            cqEuaPlanService.delete(id);
            return Result.success("删除成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/execute")
    public Result execute(@RequestBody CqEuaPlanExecuteRequest request) {
        try {
            return Result.success(cqEuaPlanService.execute(request.getPlanId()));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/bay-options")
    public Result bayOptions() {
        return Result.success(cqEuaPlanService.bayOptions());
    }

    @PostMapping("/task-record/page")
    public Result taskRecordPage(@RequestBody CqEuaTaskRecordPageRequest request) {
        return Result.success(cqEuaPlanService.taskRecordPage(request));
    }

    @PostMapping("/task-picture/list")
    public Result taskPictureList(@RequestBody CqEuaTaskPictureListRequest request) {
        try {
            return Result.success(cqEuaPlanService.taskPictureList(request));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/task-picture/image")
    public ResponseEntity<org.springframework.core.io.Resource> taskPictureImage(@RequestParam Long id,
                                                    @RequestParam(defaultValue = "false") boolean analysis) throws java.io.IOException {
        org.springframework.core.io.Resource image = cqEuaPlanService.taskPictureImage(id, analysis);
        String type = java.nio.file.Files.probeContentType(image.getFile().toPath());
        MediaType mediaType = type != null && type.startsWith("image/")
                ? MediaType.parseMediaType(type) : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType)
                .header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff").body(image);
    }
}
