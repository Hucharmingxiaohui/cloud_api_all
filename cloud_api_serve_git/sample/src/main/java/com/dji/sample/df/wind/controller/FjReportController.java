package com.dji.sample.df.wind.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.Result;
import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;
import com.dji.sample.df.electricInspectionDf.service.ReportService;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.wind.handler.PictureSaveHandler;
import com.dji.sample.df.wind.config.FjFileConfig;
import com.dji.sample.df.wind.dao.DefectEntityMapper;
import com.dji.sample.df.wind.model.entity.AnalysisRequest;
import com.dji.sample.df.wind.model.entity.AnalysisResponse;
import com.dji.sample.df.wind.service.FjReportService;

import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.springframework.http.HttpHeaders;

@Slf4j
@RestController
@RequestMapping("fjReport/api/v1/")
public class FjReportController {


    @Autowired
    ReportService reportService;

    @Autowired
    private FjReportService fjReportService;

    @Autowired
    private FjFileConfig fileConfig;

    @Autowired
    private PictureSaveHandler pictureSaveHandler;

    @Autowired
    IFileMapperDf iFileMapperDf;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    DefectEntityMapper defectEntityMapper;

    @Autowired
    IWaylineJobMapper waylineJobMapper;



    private static final String ANALYSIS_URL = "http://172.20.63.157:20012/api";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * 保存巡检图片并分析
     */
    @PostMapping("/pictureSave")
    public Result pictureSave(@RequestBody JSONObject jsonObject) {
        String jobId = jsonObject.get("jobId").toString();
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>().eq(WaylineJobEntity::getJobId, jobId));
//      正在分析（实则是正在保存加分析）
        waylineJobEntity.setIsAnalyzed(2);
        waylineJobMapper.updateById(waylineJobEntity);
        log.info(jobId+"正在分析。。。");
        Result result = pictureSaveHandler.pictureSave(jobId);
        if (result.getCode() == 0) {
             AnalysisRequest request = new AnalysisRequest();
             request.setFunction("defect_fjxj");
             request.setFile_path(fileConfig.getFilePictrueUrl() + jobId);
             // 动态生成文件名列表
             List<MediaFileEntity> mediaFileEntities = iFileMapperDf.selectList(new LambdaQueryWrapper<MediaFileEntity>().
                     eq(MediaFileEntity::getJobId, jobId));
             // 从Redis获取图片命名规则
             String fanPointsJson = redisUtils.get("fanPoints").toString();
             JSONArray points = JSON.parseArray(fanPointsJson);
             List<String> fileNames = generateFileNames(mediaFileEntities, points);
             request.setFile_name(fileNames);
             AnalysisResponse response = sendAnalysisRequest(request);
             if (response != null) {
                 System.out.println("分析结果: " + response);
             }
             fjReportService.processAndAddDefects(response, jobId);
//           分析完成
             waylineJobEntity.setIsAnalyzed(1);
             log.info(jobId+"分析完成。。。");
             waylineJobMapper.updateById(waylineJobEntity);
             return Result.success("success");
        }
        return Result.success("success");
    }

    @GetMapping("/isAnalyzed")
    public Result isAnalyzed(@RequestParam String jobId) {
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>().eq(WaylineJobEntity::getJobId, jobId));
        Integer isAnalyzed = waylineJobEntity.getIsAnalyzed();
        if(isAnalyzed == null){
            return Result.success(0);
        }else if(isAnalyzed == 1){
            return Result.success(1);
        }else if(isAnalyzed == 2){
            return Result.success(2);
        }
        return Result.success(0);
    }

    /**
     * 巡视报告界面接口-巡视报告生成
     */
    @PostMapping("/createTaskReport")
    public Result createHisTaskReport(@RequestBody JSONObject jsonObject) {
        String jobId = jsonObject.get("jobId").toString();
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>().eq(WaylineJobEntity::getJobId, jobId));
        Integer isAnalyzed = waylineJobEntity.getIsAnalyzed();
        if(isAnalyzed == null){
            return Result.analyzing("巡检结果还在分析，请稍后尝试");
        }else {
            Integer isReported = waylineJobEntity.getIsReported();
            if(isReported != null){
                return Result.duplicate("巡检结果已生成巡检报告，无需重复生成");
            }
        }
        String reportId = fjReportService.createNewReport(jobId);
//        ExecuteFJReportGenTimer.putMap(id, jobId);
        fjReportService.genPatrolTaskWordNew(reportId,jobId);
//      已进行巡检
        waylineJobEntity.setIsReported(1);
        waylineJobMapper.updateById(waylineJobEntity);
        log.info("创建巡视报告记录，排队生成报告，reportId:{} jobId {}", reportId, jobId);
        return Result.success("reportId:"+reportId);
    }

    @GetMapping("/downloadDocxFile")
    public void downloadDocxFile(@RequestParam String jobId, HttpServletResponse response) {
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(
                new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, jobId)
        );
        String name = waylineJobEntity.getName();
        String filePath = fileConfig.getFileReportPath() + name +".docx";
        fjReportService.downloadDocxFile(filePath, response);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String jobId) {
        try {
            WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(
                    new LambdaQueryWrapper<WaylineJobEntity>()
                            .eq(WaylineJobEntity::getJobId, jobId)
            );
            String name = waylineJobEntity.getName();
            String filePath = fileConfig.getFileReportPath() + name + ".docx";
            Path path = Paths.get(filePath).normalize();
            File file = path.toFile();

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            if (!file.isFile()) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(file.toURI());
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String encodedFileName = new String(file.getName().getBytes("UTF-8"), "ISO-8859-1");

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFileName + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, contentType);
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()));
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            log.error("文件下载失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

        }
    }



    /**
     * 查看巡视报告
     */
    @PostMapping("/lookReport")
    public Result<TaskReportDTO> lookReport(@RequestBody HisUniTaskParamsDTO params) {
        ParamsUtils.isBlank(params, "taskPatrolledId");
        TaskReportDTO dto = reportService.lookReport(params);
        return Result.success(dto);
    }

    public List<String> generateFileNames(List<MediaFileEntity> mediaFileEntities, JSONArray points) {
        List<String> fileNames = new ArrayList<>();
        int index = 0;

        for (MediaFileEntity mediaFileEntity : mediaFileEntities) {
            String fileName;
            if (index < points.size()) {
                // 使用Redis中的命名规则
                String pointName = points.getString(index);
                fileName = pointName + ".jpg";
            } else {
                // 如果图片数量超过Redis规则，使用原始文件名
                String originalName = mediaFileEntity.getFileName() != null ?
                        mediaFileEntity.getFileName() :
                        "file_" + mediaFileEntity.getFileId();
                // 确保文件扩展名
                if (!originalName.toLowerCase().endsWith(".jpg") &&
                        !originalName.toLowerCase().endsWith(".jpeg")) {
                    fileName = originalName + ".jpg";
                } else {
                    fileName = originalName;
                }
            }
            fileNames.add(fileName);
            index++;
        }
        return fileNames;
    }

    public AnalysisResponse sendAnalysisRequest(AnalysisRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ANALYSIS_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() == 200) {
                return objectMapper.readValue(httpResponse.body(), AnalysisResponse.class);
            } else {
                System.err.println("请求失败，状态码: " + httpResponse.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("发送分析请求时发生错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

}
