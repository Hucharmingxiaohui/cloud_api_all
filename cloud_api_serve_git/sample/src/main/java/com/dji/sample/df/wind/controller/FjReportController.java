package com.dji.sample.df.wind.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.Result;
import com.df.server.dto.HisUniTask.HisUniTaskParamsDTO;
import com.df.server.dto.HisUniTask.TaskReportDTO;
import com.dji.sample.center.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.ReportService;
import com.dji.sample.df.electricInspectionDf.service.ResultService;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.mediaDf.service.IFileServiceDf;
import com.dji.sample.df.solar.service.GfReportService;
import com.dji.sample.df.wind.dao.FanWaylinePointsMapper;
import com.dji.sample.df.uavHandlerDf.PictureSaveHandler;
import com.dji.sample.df.wind.config.FjFileConfig;
import com.dji.sample.df.wind.model.entity.AnalysisRequest;
import com.dji.sample.df.wind.model.entity.AnalysisResponse;
import com.dji.sample.df.wind.model.entity.FanWaylinePoints;
import com.dji.sample.df.wind.service.FjReportService;

import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.dao.IWorkspaceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sample.manage.model.entity.WorkspaceEntity;
import com.dji.sample.media.dao.IFileMapper;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("fjReport/api/v1/")
public class FjReportController {

    @Autowired
    ReportService reportService;
    @Autowired
    private FjReportService fjReportService;
    @Autowired
    private GfReportService gfReportService;
    @Autowired
    private FjFileConfig fileConfig;
    @Autowired
    private PictureSaveHandler pictureSaveHandler;
    @Autowired
    IFileMapperDf iFileMapperDf;
    @Autowired
    IWaylineJobMapper waylineJobMapper;
    @Autowired
    FanWaylinePointsMapper  fanWaylinePointsMapper;
    @Autowired
    PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;
    @Autowired
    private IDeviceMapper deviceMapper;
//  OssServiceContext还是IOssService需要区分
    @Autowired
    private OssServiceContext ossService;
    @Autowired
    IFileMapper fileMapper;
    @Autowired
    private IWorkspaceMapper workspaceMapper;
    @Autowired
    private ResultService resultService;
    @Autowired
    private IFileServiceDf fileService;
    @Autowired
    UniPointMapper2 uniPointMapper2;

    /**
     * 保存巡检图片并分析
     */
    @PostMapping("/pictureSave")
    public Result pictureSaveAndAnalysis(@RequestBody JSONObject jsonObject) throws Exception {
        String jobId = jsonObject.get("jobId").toString();
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>().eq(WaylineJobEntity::getJobId, jobId));
        waylineJobEntity.setIsReported(0);
        waylineJobMapper.updateById(waylineJobEntity);
//      正在分析（实则是正在保存加分析）
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
//      如果不是风机任务直接返回不分析，状态置为3（改为对接分析服务）
        if(pubWaylineJobPlanDfEntity!=null && pubWaylineJobPlanDfEntity.getPlanType()==0){

            waylineJobEntity.setIsAnalyzed(2);
            waylineJobMapper.updateById(waylineJobEntity);
            Result<Map> result = pictureSaveHandler.pictureSave(jobId);
            if(result.getCode() == 0){
                Map data = result.getData();
                resultService.handleUavResult(data,"e3dea0f5-37f2-4d79-ae58-490af3228069",jobId);
                log.info("进行分析------");
                return Result.success("success");
            }
//          return Result.notfan("不是风机任务");
        }
//      普通计划直接保存
        if(pubWaylineJobPlanDfEntity!=null && pubWaylineJobPlanDfEntity.getPlanType()==3){
           waylineJobEntity.setIsAnalyzed(2);
           waylineJobMapper.updateById(waylineJobEntity);
           pictureSaveHandler.pictureSave(jobId);
           return Result.success("success");
        }
//      光伏计划先直接保存
        if(pubWaylineJobPlanDfEntity!=null && pubWaylineJobPlanDfEntity.getPlanType()==4){
            waylineJobEntity.setIsAnalyzed(2);
            waylineJobMapper.updateById(waylineJobEntity);
            Result result = pictureSaveHandler.pictureSave(jobId);
            if (result.getCode() == 0) {
                AnalysisRequest request = new AnalysisRequest();
                request.setFunction("defect_fgxj");
                request.setFile_path(fileConfig.getFilePictrueUrl() + jobId);
                // 动态生成文件名列表
                List<MediaFileEntity> mediaFileEntities = iFileMapperDf.selectList(new LambdaQueryWrapper<MediaFileEntity>().
                        eq(MediaFileEntity::getJobId, jobId).orderByAsc(MediaFileEntity::getId));
                List<String> fileNames = new ArrayList<>();
                for (MediaFileEntity mediaFileEntity : mediaFileEntities) {
                    String fileName = mediaFileEntity.getFileName();
                    fileNames.add(fileName);
                }
                request.setFile_name(fileNames);
                AnalysisResponse response = gfReportService.sendGfAnalysisRequest(request);
                if (response != null) {
                    System.out.println("分析结果: " + response);
                }
                gfReportService.processAndAddDefects(response, jobId);
//              分析完成
                waylineJobEntity.setIsAnalyzed(1);
                log.info(jobId+"分析完成。。。");
                waylineJobMapper.updateById(waylineJobEntity);
                return Result.success("success");
            }
            return Result.success("success");
        }
//      风机计划
        if(pubWaylineJobPlanDfEntity!=null && pubWaylineJobPlanDfEntity.getPlanType()==1){
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
                        eq(MediaFileEntity::getJobId, jobId).orderByAsc(MediaFileEntity::getId));

                FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
                        .eq(FanWaylinePoints::getJobId, jobId));
                JSONArray jsonArray = new JSONArray();
                Integer jobType = fanWaylinePoints.getJobType();
                JSONArray djiPoints = JSON.parseArray(fanWaylinePoints.getDjiFanPoints());
                JSONArray videoPoints = JSON.parseArray(fanWaylinePoints.getVideoFanPoints());
                if(jobType==0){
                    jsonArray.addAll(djiPoints);
                }else if(jobType==1){
                    jsonArray.addAll(videoPoints);
                    jsonArray.addAll(djiPoints);
                }
                List<String> fileNames = fjReportService.generateFjFileNames(mediaFileEntities, jsonArray);
                log.info("文件名为-------------"+fileNames);
                log.info("request为-------------"+request);
                request.setFile_name(fileNames);
                AnalysisResponse response = fjReportService.sendFjAnalysisRequest(request);
                if (response != null) {
                    System.out.println("分析结果: " + response);
                }
                fjReportService.processAndAddDefects(response, jobId);
//              分析完成
                waylineJobEntity.setIsAnalyzed(1);
                log.info(jobId+"分析完成。。。");
                waylineJobMapper.updateById(waylineJobEntity);
                return Result.success("success");
            }
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
        } else if (isAnalyzed == 3) {
//          不是风机任务，无需分析
            return Result.success(3);
        } else if (isAnalyzed == 4) {
//          还在保存截图文件
            return Result.success(4);
        }
        return Result.success(0);
    }

    @GetMapping("/exportPic")
    public void exportPic(@RequestParam String jobId, HttpServletResponse response) throws Exception {
        List<MediaFileDTO> allFiles = fileService.getMediaDileByJobId3(jobId, "e3dea0f5-37f2-4d79-ae58-490af3228069");

        if (allFiles == null || allFiles.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("未找到相关图片文件");
            return;
        }

        // 设置响应头
        String zipFileName = "exported_images_" + jobId + ".zip";
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFileName + "\"");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            for (MediaFileDTO mediaFileDTO : allFiles) {
                // 获取原始文件名
                String originalFileName = mediaFileDTO.getFileName();
                // 将.jpeg替换为.jpg
                String newFileName = originalFileName.replace(".jpeg", ".jpg");

                Integer pointPos = extractWaypointNumber(newFileName);
                String picType = extractTOrV(newFileName);
                WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, jobId));
                UniPoint uniPoint = uniPointMapper2.selectOne(new LambdaQueryWrapper<UniPoint>()
                        .eq(UniPoint::getWaylineId, waylineJobEntity.getFileId())
                        .eq(UniPoint::getWaylinePointPos, pointPos)
                        .eq(UniPoint::getPicType,picType));
//                  对接分析服务唯一标识
                String regId=uniPoint.getPointCode()+picType;

                String filePictureUrl = fileConfig.getRecfilePath() + fileConfig.getRecfileNativePath() +
                        jobId + "/" + regId +"_" + newFileName;  // 注意：这里还是使用原始文件名读取文件

                File imageFile = new File(filePictureUrl);

                if (imageFile.exists() && imageFile.isFile()) {
                    // 创建ZIP条目，使用新的文件名
                    String entryName = regId + "_" + newFileName;
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zipEntry.setSize(imageFile.length());
                    zipEntry.setTime(imageFile.lastModified());
                    zipOut.putNextEntry(zipEntry);

                    // 将文件内容写入ZIP
                    try (FileInputStream fis = new FileInputStream(imageFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zipOut.write(buffer, 0, len);
                        }
                    }
                    zipOut.closeEntry();
                } else {
                    // 可以记录日志，文件不存在
                    System.out.println("文件不存在: " + filePictureUrl);
                }
            }
            zipOut.finish();
        }
    }

    public Integer extractWaypointNumber(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        // 正则表达式：匹配"航点"后面的一个或多个数字
        // 注意：航点可能是中文，数字可能是一位或多位
        Pattern pattern = Pattern.compile("航点(\\d+)");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // 如果数字太大或格式错误，返回null
                return null;
            }
        }

        return null;
    }

    public static String extractTOrV(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        // 移除路径和扩展名，只获取文件名部分
        String nameWithoutPath = fileName.substring(fileName.lastIndexOf('/') + 1);
        nameWithoutPath = nameWithoutPath.substring(nameWithoutPath.lastIndexOf('\\') + 1);
        String nameWithoutExt = nameWithoutPath.split("\\.", 2)[0];

        // 方法1：使用正则表达式匹配_T_或_V_模式
        Pattern pattern = Pattern.compile("_(T|V)_");
        Matcher matcher = pattern.matcher(nameWithoutExt);

        if (matcher.find()) {
            return matcher.group(1);  // 直接返回字符串
        }
        return null;
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
            if(isReported == 1){
                return Result.duplicate("巡检结果已生成巡检报告，无需重复生成");
            }
        }
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
        Integer planType = pubWaylineJobPlanDfEntity.getPlanType();
        String reportId =null;
        if(planType==0){
//           普通任务生成报告
             reportId = fjReportService.createNewReport(jobId);
             fjReportService.genNormalPatrolTaskWordNew(reportId,jobId);
             log.info("生成普通报告------");
        }else if(planType==1){
//           风机任务生成报告
             reportId = fjReportService.createNewReport(jobId);
             fjReportService.genFjPatrolTaskWordNew(reportId,jobId);
        } else if (planType==4) {
//           光伏任务生成报告
            reportId = fjReportService.createNewReport(jobId);
            fjReportService.genFjPatrolTaskWordNew(reportId,jobId);

        }

//      已进行巡检
        waylineJobEntity.setIsReported(1);
        waylineJobMapper.updateById(waylineJobEntity);
        log.info("创建巡视报告记录，排队生成报告，reportId:{} jobId {}", reportId, jobId);
        return Result.success("reportId:"+reportId);
    }

//  删除报告，重新生成报告用，之前调用
    @GetMapping("deleteReport")
    public Result deleteReport(@RequestParam String jobId) {
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(
                new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, jobId)
        );
        String reportPath = fileConfig.getFileReportPath() + "/"+ waylineJobEntity.getName() +".docx";
        File reportFile = new File(reportPath);

        boolean isFileDeleted = deleteReportFile(reportFile, waylineJobEntity.getName());
        waylineJobEntity.setIsReported(0);
        waylineJobMapper.updateById(waylineJobEntity);
        if (isFileDeleted) {
            return Result.success("报告删除成功，可以重新生成");
        } else {
            return Result.error("报告删除失败，请检查文件是否存在");
        }
    }

    /**
     * 删除报告文件
     */
    private boolean deleteReportFile(File reportFile, String fileName) {
        if (reportFile.exists()) {
            if (reportFile.delete()) {
                log.info("报告文件删除成功: {}", fileName);
                return true;
            } else {
                log.error("报告文件删除失败: {}", fileName);
                return false;
            }
        } else {
            log.warn("报告文件不存在: {}", fileName);
            return true; // 文件不存在也算删除成功
        }
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

    /**
     * 保存视频截图
     */
    @PostMapping("/savePic")
    public Result savePic(@RequestBody JSONObject jsonResponse) {
        String jobId = jsonResponse.getString("jobId");
        WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());
        String workspaceId = workspaceEntity.getWorkspaceId();
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>().
                eq(WaylineJobEntity::getJobId, jobId));
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
//              处理抓拍图片
                String filePath = jsonResponse.getString("file_path");
                JSONArray imageList = jsonResponse.getJSONArray("imageList");
                for (int i = 0; i < imageList.size(); i++) {
                    try {
                        File checkFile = new File(filePath + imageList.getString(i));
                        if (!checkFile.exists()) {
                            log.warn("文件不存在: {}", filePath + imageList.getString(i));
                            continue;  // 跳过这个文件
                        }
                        if (!checkFile.isFile()) {
                            log.warn("路径不是文件: {}", filePath + imageList.getString(i));
                            continue;
                        }
                        if (checkFile.length() == 0) {
                            log.warn("文件为空: {}", filePath + imageList.getString(i));
                            continue;
                        }
                        MultipartFile file = convert(checkFile);
                        // 先检查文件是否存在且是文件（不是目录）
                        log.info("保存文件视频截图文件---"+file.getOriginalFilename());
                        String ObjectKey= OssConfiguration.objectDirPrefix + "/" + jobId + "/" +file.getOriginalFilename();;
                        ossService.putObject(OssConfiguration.bucket, ObjectKey, file.getInputStream());
                        com.dji.sample.media.model.MediaFileEntity mediaFileEntity = new com.dji.sample.media.model.MediaFileEntity();
                        mediaFileEntity.setFileId(UUID.randomUUID().toString());
                        mediaFileEntity.setFileName(file.getOriginalFilename());
                        mediaFileEntity.setFilePath(OssConfiguration.objectDirPrefix + "/" + jobId);
                        mediaFileEntity.setObjectKey(ObjectKey);
                        mediaFileEntity.setJobId(jobId);
                        mediaFileEntity.setWorkspaceId(workspaceId);
                        DeviceEntity dockEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDeviceSn, pubWaylineJobPlanDfEntity.getDockSn()));
                        mediaFileEntity.setDrone(dockEntity.getDeviceSn());
//                     负载暂时不写
                        mediaFileEntity.setPayload(null);
                        mediaFileEntity.setIsOriginal(true);
                        log.info("插入文件入库---"+mediaFileEntity);
                        fileMapper.insert(mediaFileEntity);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                log.info("开始执行保存点位----");
//              截图点位入库
                FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
                        .eq(FanWaylinePoints::getJobId, jobId));
                for (int i = 0; i < imageList.size(); i++) {
                    String fileName = imageList.getString(i);
                    imageList.set(i, fileName.replace(".jpg", ""));
                }
                JSONArray jsonArray = new JSONArray();
                String videoFanPoints = fanWaylinePoints.getVideoFanPoints();
                if (videoFanPoints != null) {
                    JSONArray videoPoints = JSON.parseArray(videoFanPoints);
                    jsonArray.addAll(videoPoints);
                    jsonArray.addAll(imageList);
                }else {
                    jsonArray.addAll(imageList);
                }
                fanWaylinePoints.setVideoFanPoints(jsonArray.toJSONString());
                fanWaylinePointsMapper.updateById(fanWaylinePoints);
//              保存job的保存标志位，正面保存完是1，反面保存完是2
                String segment = jsonResponse.getString("segment");
                if(segment.equals("front")){
                    waylineJobEntity.setIsSaved(1);
                    waylineJobMapper.updateById(waylineJobEntity);
                }else if(segment.equals("back")){
                    waylineJobEntity.setIsSaved(2);
                    waylineJobMapper.updateById(waylineJobEntity);
                }
                log.info("截图点位入库---"+fanWaylinePoints);
                log.info("处理返回图像---------------------");
           return Result.success("保存成功");
    }


    public static MultipartFile convert(File file) throws IOException {
        return new CustomMultipartFile(
                file.getName(), file
        );
    }

    static class CustomMultipartFile implements MultipartFile {
        private final String name;
        private final File file;
        private FileInputStream fis;

        public CustomMultipartFile(String name, File file) throws IOException {
            this.name = name;
            this.file = file;
            this.fis = new FileInputStream(file);
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return null; }
        @Override public boolean isEmpty() { return file.length() == 0; }
        @Override public long getSize() { return file.length(); }

        @Override
        public byte[] getBytes() throws IOException {
            return new byte[0];
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return fis; // 直接返回文件流
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.copy(fis, dest.toPath());
        }
    }


    private static final long MAX_FOLDER_SIZE = 1024 * 1024 * 1024;
    private static final int MAX_FILE_COUNT = 10000;

    /**
     * 最直接的解决方案 - 使用HttpServletResponse直接写入
     * GET /api/files/download-folder?path=/home/user/documents
     */
    @GetMapping("/download-folder")
    public void downloadFolderAsZip(
            @RequestParam String jobId,
            HttpServletResponse response) throws IOException {
        String filePath = fileConfig.getFilePictrueUrl()+jobId;
        try {
            // 验证路径

            Path folderPath = validateAndResolvePath(filePath);

            if (!Files.exists(folderPath)) {
                sendJsonError(response, 404, "文件夹不存在: " + filePath);
                return;
            }

            if (!Files.isDirectory(folderPath)) {
                sendJsonError(response, 400, "路径不是文件夹: " + filePath);
                return;
            }

            // 检查大小
            FolderInfo folderInfo = calculateFolderInfo(folderPath);
            if (folderInfo.getFileCount() > MAX_FILE_COUNT) {
                sendJsonError(response, 413,
                        "文件夹包含文件过多，最多允许 " + MAX_FILE_COUNT + " 个文件");
                return;
            }

            if (folderInfo.getTotalSize() > MAX_FOLDER_SIZE) {
                sendJsonError(response, 413,
                        "文件夹太大，最大允许 " + formatFileSize(MAX_FOLDER_SIZE));
                return;
            }

            // 设置响应头
            String zipFileName = folderPath.getFileName() + ".zip";
            response.setContentType("application/zip");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + zipFileName + "\"");

            // 直接写入响应流
            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                zipFolderToStream(folderPath, folderPath.getFileName().toString(), zos);
                zos.finish();
            }

        } catch (InvalidPathException e) {
            sendJsonError(response, 400, "无效的路径格式: " + filePath);
        } catch (AccessDeniedException e) {
            sendJsonError(response, 403, "无权访问该文件夹: " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonError(response, 500, "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 发送JSON格式错误响应
     */
    private void sendJsonError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write("{\"success\":false,\"message\":\"" + message +
                "\",\"timestamp\":" + System.currentTimeMillis() + "}");
        writer.flush();
    }

    /**
     * 递归压缩文件夹到输出流
     */
    private void zipFolderToStream(Path sourcePath, String parentDir, ZipOutputStream zos)
            throws IOException {
        Files.walk(sourcePath)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String zipPath = parentDir + "/" +
                                sourcePath.relativize(path).toString().replace("\\", "/");

                        ZipEntry zipEntry = new ZipEntry(zipPath);
                        zipEntry.setTime(Files.getLastModifiedTime(path).toMillis());
                        zos.putNextEntry(zipEntry);

                        try (InputStream is = Files.newInputStream(path)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = is.read(buffer)) > 0) {
                                zos.write(buffer, 0, len);
                            }
                        }

                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
      }

        /**
         * 安全地验证和解析路径
         */
        private Path validateAndResolvePath(String userPath) throws IOException {
            // 解析路径
            Path requestedPath = Paths.get(userPath).normalize();

            // 防止路径遍历攻击
            if (requestedPath.toString().contains("..")) {
                throw new AccessDeniedException("路径包含非法字符");
            }
            return requestedPath;
        }

        /**
         * 计算文件夹信息
         */
        private FolderInfo calculateFolderInfo(Path folderPath) throws IOException {
            FolderInfo info = new FolderInfo();

            Files.walk(folderPath)
                    .forEach(path -> {
                        try {
                            if (Files.isRegularFile(path)) {
                                info.incrementFileCount();
                                info.addSize(Files.size(path));
                            }
                        } catch (IOException e) {
                            // 记录但继续处理其他文件
                            e.printStackTrace();
                        }
                    });

            return info;
        }

        private String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            int exp = (int) (Math.log(size) / Math.log(1024));
            char unit = "KMGTPE".charAt(exp - 1);
            return String.format("%.2f %sB", size / Math.pow(1024, exp), unit);
        }

        /**
         * 文件夹信息类
         */
        private static class FolderInfo {
            private long fileCount = 0;
            private long totalSize = 0;

            public void incrementFileCount() {
                fileCount++;
            }

            public void addSize(long size) {
                totalSize += size;
            }

            public long getFileCount() {
                return fileCount;
            }

            public long getTotalSize() {
                return totalSize;
            }
        }


}


