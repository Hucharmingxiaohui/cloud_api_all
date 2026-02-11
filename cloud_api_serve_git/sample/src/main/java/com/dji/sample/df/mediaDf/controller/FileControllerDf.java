package com.dji.sample.df.mediaDf.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.server.entity.uni.UniPointEntity;
import com.dji.sample.center.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.mediaDf.model.JobIdEntity;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.mediaDf.service.IFileServiceDf;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointResult;
import com.dji.sample.df.wind.dao.DefectEntityMapper;
import com.dji.sample.df.wind.model.entity.DefectEntity;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.Pagination;
import com.dji.sdk.common.PaginationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author sean
 * @version 0.2
 * @date 2021/12/9
 */
@Slf4j
@RestController
@RequestMapping("${url.media.prefix}${url.media.version}/files")
public class FileControllerDf {

    @Autowired
    private IFileServiceDf fileService;

    @Autowired
    DefectEntityMapper defectEntityMapper;

    @Autowired
    private IWaylineJobMapper waylineJobMapper;

    @Autowired
    PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;

    @Value("${server.base-url:http://172.20.36.157:6789}")
    private String serverBaseUrl;

    @Autowired
    UniPointMapper2 uniPointMapper2;

    //查询一张图片
    @GetMapping("/{workspace_id}/files/{file_name}")
    public HttpResultResponse<PaginationData<MediaFileDTO>> getOneFilesList(@RequestParam(defaultValue = "1") Long page,
                                                                            @RequestParam(name = "page_size", defaultValue = "10") Long pageSize,
                                                                            @PathVariable(name = "workspace_id") String workspaceId,
                                                                            @PathVariable(name = "file_name") String fileName) {
        PaginationData<MediaFileDTO> filesList = fileService.getMediaFilesPaginationByFileName(workspaceId, page, pageSize, fileName);
        return HttpResultResponse.success(filesList);
    }

    //删除一张图片
    @DeleteMapping("/{workspace_id}/files/{file_id}")
    public HttpResultResponse deletMedia(
            @PathVariable(name = "workspace_id") String workspaceId,
            @PathVariable(name = "file_id") String fileId) {
        boolean isDel = fileService.deleteMedia(workspaceId, fileId);
        System.out.println(isDel);
        return isDel ? HttpResultResponse.success() : HttpResultResponse.error("Failed to delete media.");
    }

    //获取job_id
    @GetMapping("/{workspace_id}/files/mission/getJobId")
    public HttpResultResponse<?> getJobidList(@PathVariable(name = "workspace_id") String workspaceId) {
        List<MediaFileDTO> jobIdList = fileService.getJobIdList(workspaceId);
        List<JobIdEntity> jobIds = new ArrayList<>();
        Set<String> idsSet = new HashSet<>(); // 用于存储已经添加到jobIds的jobId
        for (int i = 0; i < jobIdList.size(); i++) {
            if (jobIdList.get(i).getJobId() != null && !idsSet.contains(jobIdList.get(i).getJobId())) {
                JobIdEntity jobidentity = new JobIdEntity();
                jobidentity.setFilePath(jobIdList.get(i).getFilePath());
                jobidentity.setDrone(jobIdList.get(i).getDrone());
                jobidentity.setPayload(jobIdList.get(i).getPayload());
                jobidentity.setCreateTime(jobIdList.get(i).getCreateTime());
                jobidentity.setJobId(jobIdList.get(i).getJobId());
                idsSet.add(jobIdList.get(i).getJobId()); // 将jobId添加到idsSet中
                jobIds.add(jobidentity);
            }
        }
        return HttpResultResponse.success(jobIds);
    }


    //按job_id查询任务图片
    @GetMapping("/{workspace_id}/files/{job_id}/jobIdUrl")
    public HttpResultResponse<?> getJobIdFileList(@RequestParam(defaultValue = "1") Long page,
                                                  @RequestParam(name = "page_size", defaultValue = "10") Long pageSize,
                                                  @PathVariable(name = "workspace_id") String workspaceId,
                                                  @RequestParam(name = "start_time", defaultValue = "-1") Long startTime,
                                                  @RequestParam(name = "end_time", defaultValue = "-1") Long endTime,
                                                  @PathVariable(name = "job_id") String job_id) {
        PaginationData<MediaFileDTO> filesList = fileService.getMediaFilesPaginationByJobId(workspaceId, page, pageSize, job_id, startTime, endTime);
        List<MediaFileDTO> jobFileList = filesList.getList();
        Map<String, List<MediaFileDTO>> jobMap = new HashMap<String, List<MediaFileDTO>>();
        for (int i = 0; i < jobFileList.size(); i++) {
            String wayPointName = "航点" + (i + 1);
            List<MediaFileDTO> hangdianList = new ArrayList<>();
            hangdianList.add(jobFileList.get(i));
            if (hangdianList.size() > 0) {
                jobMap.put(wayPointName, hangdianList);
            }

        }

        return HttpResultResponse.success(jobMap);
    }
    @GetMapping("/getMediaDileByJobId")
    public HttpResultResponse getMediaDileByJobId(String job_id,String workspace_id,String wayline_id) throws Exception {
        PointResult pointResult =fileService.getMediaDileByJobId(job_id,workspace_id,wayline_id);
        return HttpResultResponse.success(pointResult).setMessage("查询任务结果成功");
    }

    @GetMapping("/getPlanType")
    public HttpResultResponse getPlanType(String job_id) throws Exception {
        int planType = fileService.getPlanType(job_id);
        return HttpResultResponse.success(planType).setMessage("查询任务结果成功");
    }

    @GetMapping("/getMediaFileByJobId")
    public HttpResultResponse getMediaFileByJobId(String job_id,String workspace_id,@RequestParam(defaultValue = "1") Long page,
                                                  @RequestParam(defaultValue = "10") Long pageSize,@RequestParam Map map) throws Exception {
        List<MediaFileDTO> allFiles = fileService.getMediaDileByJobId3(job_id, workspace_id);

        List<DefectEntity> defectList = defectEntityMapper.selectList(new LambdaQueryWrapper<DefectEntity>()
                .eq(DefectEntity::getJobId,job_id).orderByAsc(DefectEntity::getId));
        // 条件过滤
        List<MediaFileDTO> filteredFiles = allFiles.stream()
                .filter(file -> {
                    boolean match = true;
                    // 文件名模糊查询
                    if (map.containsKey("fileName") && map.get("fileName") != null) {
                        String fileName = map.get("fileName").toString();
                        if (file.getFileName() != null) {
                            match = match && file.getFileName().contains(fileName);
                        } else {
                            match = false;
                        }
                    }
                    return match;
                })
                .collect(Collectors.toList());

        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId, job_id));
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
//      如果为风机任务则加入分析图url
        if(pubWaylineJobPlanDfEntity != null&&pubWaylineJobPlanDfEntity.getPlanType()==1){
            for (int j = 0; j < filteredFiles.size(); j++) {
                if(defectList!=null&& !defectList.isEmpty()){
                    DefectEntity defect = defectList.get(j);
                    String imagePath = defect.getImagePath();
                    if (imagePath != null && !imagePath.isEmpty()) {
                        // 转换为可访问的URL
                        String imageUrl = "/api/file/defect?path=" +
                                URLEncoder.encode(imagePath, "UTF-8");
                        filteredFiles.get(j).setDefectImageUrl(imageUrl);
                    }
                    String imagePath1="/home/uav_server/defect_images/"+job_id+"/"+defect.getFanCode()+"-"+defect.getFanPart()+".jpg";
                    String imageUrl1 = "/api/file/defect?path=" +
                            URLEncoder.encode(imagePath1, "UTF-8");
                    filteredFiles.get(j).setOriginalImageUrl(imageUrl1);
                    filteredFiles.get(j).setDefectType(defect.getDefectType());
                    filteredFiles.get(j).setDefectDescription(defect.getDefectDescription());
                    filteredFiles.get(j).setFanCode(defect.getFanCode());
                    filteredFiles.get(j).setFanPart(defect.getFanPart());
                    filteredFiles.get(j).setDefectId(defect.getId());
                }
            }
        }else {
            for (int j = 0; j < filteredFiles.size(); j++) {
                String fileName = filteredFiles.get(j).getFileName();
                Integer pointPos = extractWaypointNumber(fileName);
                String picType = extractTOrV(fileName);
                Integer picType1 = 0;
                if(picType.equals("V")){
                    picType1 = 0;
                }else if(picType.equals("T")){
                    picType1 = 1;
                }
                UniPoint uniPoint = uniPointMapper2.selectOne(new LambdaQueryWrapper<UniPoint>()
                        .eq(UniPoint::getWaylineId, waylineJobEntity.getFileId())
                        .eq(UniPoint::getWaylinePointPos, pointPos)
                        .eq(UniPoint::getPicType,picType1));
                if(uniPoint == null){
                    log.info("未查到对应点位-----");
                    filteredFiles.get(j).setOriginalImageUrl("未查到对应点位");
                    continue;
                }
                String replace = fileName.replace(".jpeg", "");
//                  对接分析服务唯一标识
                String regId=uniPoint.getPointCode()+picType;
//                  普通照片保存形式为：点位编码+照片类型+"_"+图片原名
                    String imagePath1="/ftpdir/admin_files/recfile_images/"+job_id+"/"+regId+"_"+replace+".jpg";
                    String imageUrl1 = "/api/file/defect?path=" +
                            URLEncoder.encode(imagePath1, "UTF-8");
                    filteredFiles.get(j).setOriginalImageUrl(imageUrl1);
                }
        }

//      如果为普通任务加上智能分析图url

        // 内存分页
        int total = filteredFiles.size();
        int fromIndex = (int) ((page - 1) * pageSize);
        int toIndex = (int) Math.min(fromIndex + pageSize, total);
        if (fromIndex >= total) {
            fromIndex = 0;
            toIndex = 0;
        }
        List<MediaFileDTO> pageList = filteredFiles.subList(fromIndex, toIndex);
        // 构建分页结果
        PaginationData<MediaFileDTO> result = new PaginationData<>(pageList,
                new Pagination(page, pageSize, total));
        return HttpResultResponse.success(result).setMessage("查询任务结果成功");
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


//  根据任务获取点位列表
    @GetMapping("/getPointByJobId")
    public HttpResultResponse getPointByJobId(String job_id) throws Exception {
        List<UniPointEntity> uniPointEntityList = fileService.getPointByJobId(job_id);
        return HttpResultResponse.success(uniPointEntityList).setMessage("查询任务结果成功");
    }
//  根据点位获取结果
    @GetMapping("/getMediaFileByPoint")
    public HttpResultResponse getMediaFileByPoint(String job_id,Integer point_pos, String workspace_id, String wayline_id) throws Exception {
        PointResult mediaFileByPoint = fileService.getMediaFileByPoint(job_id, point_pos, workspace_id, wayline_id);
        return HttpResultResponse.success(mediaFileByPoint).setMessage("查询任务结果成功");
    }

    // 测试接口  根据workspaceId 和fileId  生成缩略图
    @GetMapping("/getThumbnailByJobId")
    public HttpResultResponse getThumbnailByJobId(@RequestParam String file_id, @RequestParam String workspace_id) throws Exception {
        boolean result =fileService.getThumbnailByJobId(workspace_id,file_id);
        System.out.println(result);
        return result ? HttpResultResponse.success() : HttpResultResponse.error("缩略图生成失败");
    }

    // 获取缩略图url
    @GetMapping("/{workspace_id}/file/{file_id}/getthumbnail")
    public void getThumbnailUrl(@PathVariable(name = "workspace_id") String workspaceId,
                                @PathVariable(name = "file_id") String fileId, HttpServletResponse response) {

        try {
            URL url = fileService.getThumbnailUrl(workspaceId, fileId);
            response.sendRedirect(url.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
