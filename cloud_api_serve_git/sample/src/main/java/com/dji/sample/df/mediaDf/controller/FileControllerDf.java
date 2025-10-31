package com.dji.sample.df.mediaDf.controller;

import com.df.server.entity.uni.UniPointEntity;
import com.dji.sample.df.mediaDf.model.JobIdEntity;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.mediaDf.service.IFileServiceDf;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointResult;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * @author sean
 * @version 0.2
 * @date 2021/12/9
 */
@RestController
@RequestMapping("${url.media.prefix}${url.media.version}/files")
public class FileControllerDf {

    @Autowired
    private IFileServiceDf fileService;


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

    @GetMapping("/getMediaFileByJobId")
    public HttpResultResponse getMediaFileByJobId(String job_id,String workspace_id) throws Exception {
        List<MediaFileDTO> mediaFileDTOS = fileService.getMediaDileByJobId3(job_id, workspace_id);
        return HttpResultResponse.success(mediaFileDTOS).setMessage("查询任务结果成功");
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
