package com.dji.sample.df.mediaDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.mapper.uni.UniPointMapper;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.df.TemperatureMeasurementDF.service.IFileServiceDF;
import com.dji.sample.df.commonDf.util.DeleteFile;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.model.PubWaylinePointDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylinePointDfService;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.dao.IThumbnailMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.mediaDf.model.MediaThumbnailEntity;
import com.dji.sample.df.mediaDf.service.IFileServiceDf;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointCount.PointCount;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointResult;
import com.dji.sample.df.thirdKmzDf.service.WaylineKmzThirdService;
import com.dji.sample.manage.model.dto.DeviceDictionaryDTO;
import com.dji.sample.manage.service.IDeviceDictionaryService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.cloudapi.device.DeviceEnum;
import com.dji.sdk.cloudapi.media.MediaSubFileTypeEnum;
import com.dji.sdk.cloudapi.media.MediaUploadCallbackRequest;
import com.dji.sdk.common.Pagination;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author sean
 * @version 0.2
 * @date 2021/12/9
 */
@Service
@Transactional
public class FileServiceImplDf implements IFileServiceDf {

    @Autowired
    private IFileMapperDf mapper;

    @Autowired
    private IDeviceDictionaryService deviceDictionaryService;

    @Autowired
    private OssServiceContext ossService;
    @Autowired
    private IWaylineFileService waylineFileService;
    @Autowired
    private PubWaylinePointDfService pubWaylinePointDfService;
    @Autowired
    private WaylineKmzThirdService waylineKmzThirdService;
    @Autowired
    private IFileServiceDF fileService;
    @Autowired
    private IThumbnailMapperDf thumbnailMapper;
    @Autowired
    private IWaylineJobMapper waylineJobMapper;
    @Autowired
    private PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;
    @Autowired
    private UniPointMapper uniPointMapper;

    private Optional<MediaFileEntity> getMediaByFingerprint(String workspaceId, String fingerprint) {
        MediaFileEntity fileEntity = mapper.selectOne(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getWorkspaceId, workspaceId)
                .eq(MediaFileEntity::getFingerprint, fingerprint));
        return Optional.ofNullable(fileEntity);
    }

    private Optional<MediaFileEntity> getMediaByFileId(String workspaceId, String fileId) {
        MediaFileEntity fileEntity = mapper.selectOne(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getWorkspaceId, workspaceId)
                .eq(MediaFileEntity::getFileId, fileId));
        return Optional.ofNullable(fileEntity);
    }

    @Override
    public Boolean checkExist(String workspaceId, String fingerprint) {
        return this.getMediaByFingerprint(workspaceId, fingerprint).isPresent();
    }

    @Override
    public Integer saveFile(String workspaceId, MediaUploadCallbackRequest file) {
        MediaFileEntity fileEntity = this.fileUploadConvertToEntity(file);
        fileEntity.setWorkspaceId(workspaceId);
        fileEntity.setFileId(UUID.randomUUID().toString());
        return mapper.insert(fileEntity);
    }

    @Override
    public List<MediaFileDTO> getAllFilesByWorkspaceId(String workspaceId) {
        return mapper.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getWorkspaceId, workspaceId))
                .stream()
                .map(this::entityConvertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public PaginationData<MediaFileDTO> getMediaFilesPaginationByWorkspaceId(String workspaceId, long page, long pageSize) {
        Page<MediaFileEntity> pageData = mapper.selectPage(
                new Page<MediaFileEntity>(page, pageSize),
                new LambdaQueryWrapper<MediaFileEntity>()
                        .eq(MediaFileEntity::getWorkspaceId, workspaceId)
                        .orderByDesc(MediaFileEntity::getId));
        List<MediaFileDTO> records = pageData.getRecords()
                .stream()
                .map(this::entityConvertToDto)
                .collect(Collectors.toList());

        return new PaginationData<MediaFileDTO>(records, new Pagination(pageData.getCurrent(), pageData.getSize(), pageData.getTotal()));
    }

    @Override
    public URL getObjectUrl(String workspaceId, String fileId) {
        Optional<MediaFileEntity> mediaFileOpt = getMediaByFileId(workspaceId, fileId);
        if (mediaFileOpt.isEmpty()) {
            throw new IllegalArgumentException("{} doesn't exist.");
        }

        return ossService.getObjectUrl(OssConfiguration.bucket, mediaFileOpt.get().getObjectKey());
    }

    @Override
    public List<MediaFileDTO> getFilesByJobId( String jobId) {
        return mapper.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getJobId, jobId))
                .stream()
                .map(this::entityConvertToDto).collect(Collectors.toList());
    }

    @Override
    public List<MediaFileDTO> getUniqueFilesByJobId( String jobId) {
        return mapper.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                        .eq(MediaFileEntity::getJobId, jobId))
                .stream()
                .collect(Collectors.groupingBy(
                        MediaFileEntity::getFileName,
                        Collectors.minBy(Comparator.comparing(MediaFileEntity::getCreateTime))
                ))
                .values()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::entityConvertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public PaginationData<MediaFileDTO> getMediaFilesPaginationByFileName(String workspaceId, long page, long pageSize, String fileName) {
        Page<MediaFileEntity> pageData = mapper.selectPage(
                new Page<MediaFileEntity>(page, pageSize),
                new LambdaQueryWrapper<MediaFileEntity>()
                        .eq(MediaFileEntity::getWorkspaceId, workspaceId)
                        .like(MediaFileEntity::getFileName,fileName)
                        .orderByDesc(MediaFileEntity::getId));
        List<MediaFileDTO> records = pageData.getRecords()
                .stream()
                .map(this::entityConvertToDto)
                .collect(Collectors.toList());

        return new PaginationData<MediaFileDTO>(records, new Pagination(pageData.getCurrent(), pageData.getSize(), pageData.getTotal()));
    }

    @Override
    public boolean deleteMedia(String workspaceId, String fileId) {
        Optional<MediaFileEntity> MediaOpt = this.getMediaByFileId(workspaceId,fileId);
        if ( MediaOpt.isEmpty()) {
            return true;
        }
        MediaFileEntity media = MediaOpt.get();
        boolean isDel = mapper.delete(new LambdaUpdateWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getWorkspaceId, workspaceId)
                .eq(MediaFileEntity::getFileId, fileId))
                > 0;
        if (!isDel) {
            return false;
        }
        return ossService.deleteObject(OssConfiguration.bucket,media.getObjectKey());
    }

    @Override
    public PaginationData<MediaFileDTO> getMediaFilesPaginationByJobId(String workspaceId, Long page, Long pageSize, String jobId, Long startTime, Long endTime) {
        Page<MediaFileEntity> pageData = mapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<MediaFileEntity>()
                        .eq(MediaFileEntity::getWorkspaceId, workspaceId)
                        .between(endTime != -1L && startTime != -1L, MediaFileEntity::getUpdateTime, startTime, endTime)
                        .eq(MediaFileEntity::getJobId,jobId)
                        .orderByDesc(MediaFileEntity::getId));
        List<MediaFileDTO> records = pageData.getRecords()
                .stream()
                .map(this::entityConvertToDto)
                .collect(Collectors.toList());

        return new PaginationData<MediaFileDTO>(records, new Pagination(pageData.getCurrent(), pageData.getSize(), pageData.getTotal()));
    }

    @Override
    public List<MediaFileDTO> getJobIdList(String workspaceId) {
        return mapper.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                        .eq(MediaFileEntity::getWorkspaceId, workspaceId))
                .stream()
                .map(this::entityConvertToDto).collect(Collectors.toList());
    }
    //根据job_id查询具体的航点媒体图像信息
    private final Object lock = new Object();
    //获取文件夹路径
    @Value("${singlePointUrl}")
    private String singlePointUrl;
    //通过job_id查询图片列表
    @Override
    public PointResult getMediaDileByJobId(String job_id, String workspace_id, String wayline_id) throws Exception {
        synchronized (lock){
            List<MediaFileDTO> mediaFileDTOList = this.getUniqueFilesByJobId(job_id);
            //排序
            for (int i = 0; i < mediaFileDTOList.size()- 1; i++) {
                // 设置一个标志位，用于检测是否发生了交换
                boolean swapped = false;
                // 内层循环，从第一个元素到倒数第i个元素
                for (int j = 0; j < mediaFileDTOList.size() - i - 1; j++) {
                    //获取第一张图片的时间数
                    //获取第j张照片名称
                    String T1fileName = mediaFileDTOList.get(j).getFileName();
                    //记录第J张照片时间
                    String strT1num ="";
                    //记录下划线
                    int T1count = 0;
                    for(int i1=0;i1<T1fileName.length();i1++){
                        if (T1fileName.charAt(i1) == '_'){
                            T1count++;
                        }
                        if(T1count==1&&T1fileName.charAt(i1) != '_'){
                            strT1num=strT1num+T1fileName.charAt(i1);
                        }
                        if(T1count==2){
                            break;
                        }

                    }
                    Long T1num=Long.valueOf(strT1num);

                    //获取第二张图片时间数
                    String T2fileName = mediaFileDTOList.get(j+1).getFileName();
                    //记录第J张照片时间
                    String strT2num ="";
                    //记录下划线
                    int T2count = 0;
                    for(int i1=0;i1<T2fileName.length();i1++){
                        if (T2fileName.charAt(i1) == '_'){
                            T2count++;
                        }
                        if(T2count==1&&T2fileName.charAt(i1) != '_'){
                            strT2num=strT2num+T2fileName.charAt(i1);
                        }
                        if(T2count==2){
                            break;
                        }


                    }
                    Long T2num=Long.valueOf(strT2num);
                    // 如果当前元素比下一个元素大，则交换它们
                    if (T1num> T2num) {
                        MediaFileDTO temp = mediaFileDTOList.get(j);
                        mediaFileDTOList.set(j,mediaFileDTOList.get(j+1));
                        mediaFileDTOList.set(j+1,temp);
                        swapped = true;
                    }
                }
                // 如果没有发生交换，说明列表已经有序，提前退出
                if (!swapped) {
                    break;
                }
            }
            // System.out.println(mediaFileDTOList);
            //第一步解析航线一共有多少张图片，每个点位有几张图片
            //获取下载路径
            URL url = waylineFileService.getObjectUrl(workspace_id, wayline_id);
            PointResult pointResult=waylineKmzThirdService.getPointResult(workspace_id,wayline_id,url,singlePointUrl);
            //照片位置记录
            //记录图片在数组里的位置
            int position=0;
            for(int i=0;i<pointResult.getPointCountList().size();i++){
                //记录每一个点位
                PointCount pointCount=pointResult.getPointCountList().get(i);
                //切分序列,
                // 2024.12.3修改按时间切割
                if(pointCount.getCount()>0){
                    //存储点位配置
                    PubWaylinePointDfEntity pubWaylinePointDfEntity=pubWaylinePointDfService.getPointByWaylineIdAndSequence(wayline_id,String.valueOf(i));
                    //存储点位图片
                    List<MediaFileDTO> mediaFileDTOS=new ArrayList<>();
                    for(int j=0;j<pointCount.getCount();j++){
                        if(position<mediaFileDTOList.size()){
                            //添加媒体图片
                            mediaFileDTOS.add(mediaFileDTOList.get(position));
                        }


                        position=position+1;
                    }
                    pointCount.setMediaFileDTOS(mediaFileDTOS);
                    pointCount.setPubWaylinePointDfEntity(pubWaylinePointDfEntity);
                }
            }
            return pointResult;
        }
    }
//  最新版接口，不分析航点
    @Override
    public List<MediaFileDTO> getMediaDileByJobId3(String job_id, String workspace_id) throws Exception {
        synchronized (lock) {
            List<MediaFileDTO> mediaFileDTOList = this.getUniqueFilesByJobId(job_id);
            //排序
            for (int i = 0; i < mediaFileDTOList.size() - 1; i++) {
                // 设置一个标志位，用于检测是否发生了交换
                boolean swapped = false;
                // 内层循环，从第一个元素到倒数第i个元素
                for (int j = 0; j < mediaFileDTOList.size() - i - 1; j++) {
                    //获取第一张图片的时间数
                    //获取第j张照片名称
                    String T1fileName = mediaFileDTOList.get(j).getFileName();
                    //记录第J张照片时间
                    String strT1num = "";
                    //记录下划线
                    int T1count = 0;
                    for (int i1 = 0; i1 < T1fileName.length(); i1++) {
                        if (T1fileName.charAt(i1) == '_') {
                            T1count++;
                        }
                        if (T1count == 1 && T1fileName.charAt(i1) != '_') {
                            strT1num = strT1num + T1fileName.charAt(i1);
                        }
                        if (T1count == 2) {
                            break;
                        }

                    }
                    Long T1num = Long.valueOf(strT1num);

                    //获取第二张图片时间数
                    String T2fileName = mediaFileDTOList.get(j + 1).getFileName();
                    //记录第J张照片时间
                    String strT2num = "";
                    //记录下划线
                    int T2count = 0;
                    for (int i1 = 0; i1 < T2fileName.length(); i1++) {
                        if (T2fileName.charAt(i1) == '_') {
                            T2count++;
                        }
                        if (T2count == 1 && T2fileName.charAt(i1) != '_') {
                            strT2num = strT2num + T2fileName.charAt(i1);
                        }
                        if (T2count == 2) {
                            break;
                        }


                    }
                    Long T2num = Long.valueOf(strT2num);
                    // 如果当前元素比下一个元素大，则交换它们
                    if (T1num > T2num) {
                        MediaFileDTO temp = mediaFileDTOList.get(j);
                        mediaFileDTOList.set(j, mediaFileDTOList.get(j + 1));
                        mediaFileDTOList.set(j + 1, temp);
                        swapped = true;
                    }
                }
                // 如果没有发生交换，说明列表已经有序，提前退出
                if (!swapped) {
                    break;
                }
            }
            return mediaFileDTOList;
        }
    }


    @Override
    public PointResult getMediaDileByJobId2(String job_id, String workspace_id, String wayline_id ) throws Exception {
        synchronized (lock){
//          用job_id找到plan_id,再用plan_id找到航点
            WaylineJobEntity waylineJobEntity = waylineJobMapper
                    .selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                    .eq(WaylineJobEntity::getJobId, job_id));
            PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper
                    .selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                    .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
            String waylinePointPos = pubWaylineJobPlanDfEntity.getWaylinePointPos();
            String[] numbers = waylinePointPos.split(",");
            List<MediaFileDTO> mediaFileDTOList = this.getFilesByJobId(job_id);
            List<MediaFileDTO> mediaFileDTOList1 = new  ArrayList<>();
            for (int i = 0; i < mediaFileDTOList.size(); i++) {
                String fileName = mediaFileDTOList.get(i).getFileName();
                int i2 = extractNumber(fileName);
                for (String numStr : numbers) {
                    if (Integer.parseInt(numStr.trim()) == i2) {
                        mediaFileDTOList1.add(mediaFileDTOList.get(i));
                    }
                }
            }
            //排序
            for (int i = 0; i < mediaFileDTOList1.size()- 1; i++) {
                // 设置一个标志位，用于检测是否发生了交换
                boolean swapped = false;
                // 内层循环，从第一个元素到倒数第i个元素
                for (int j = 0; j < mediaFileDTOList1.size() - i - 1; j++) {
                    //获取第一张图片的时间数
                    //获取第j张照片名称
                    String T1fileName = mediaFileDTOList1.get(j).getFileName();
                    //记录第J张照片时间
                    String strT1num ="";
                    //记录下划线
                    int T1count = 0;
                    for(int i1=0;i1<T1fileName.length();i1++){
                        if (T1fileName.charAt(i1) == '_'){
                            T1count++;
                        }
                        if(T1count==1&&T1fileName.charAt(i1) != '_'){
                            strT1num=strT1num+T1fileName.charAt(i1);
                        }
                        if(T1count==2){
                            break;
                        }


                    }
                    Long T1num=Long.valueOf(strT1num);

                    //获取第二张图片时间数
                    String T2fileName = mediaFileDTOList1.get(j+1).getFileName();
                    //记录第J张照片时间
                    String strT2num ="";
                    //记录下划线
                    int T2count = 0;
                    for(int i1=0;i1<T2fileName.length();i1++){
                        if (T2fileName.charAt(i1) == '_'){
                            T2count++;
                        }
                        if(T2count==1&&T2fileName.charAt(i1) != '_'){
                            strT2num=strT2num+T2fileName.charAt(i1);
                        }
                        if(T2count==2){
                            break;
                        }


                    }
                    Long T2num=Long.valueOf(strT2num);
                    // 如果当前元素比下一个元素大，则交换它们
                    if (T1num> T2num) {
                        MediaFileDTO temp = mediaFileDTOList1.get(j);
                        mediaFileDTOList1.set(j,mediaFileDTOList1.get(j+1));
                        mediaFileDTOList1.set(j+1,temp);
                        swapped = true;
                    }
                }
                // 如果没有发生交换，说明列表已经有序，提前退出
                if (!swapped) {
                    break;
                }
            }
            // System.out.println(mediaFileDTOList);
            //第一步解析航线一共有多少张图片，每个点位有几张图片
            //获取下载路径
            URL url = waylineFileService.getObjectUrl(workspace_id, wayline_id);
            PointResult pointResult=waylineKmzThirdService.getPointResult(workspace_id,wayline_id,url,singlePointUrl);
            //照片位置记录
            //记录图片在数组里的位置
            List<PointCount> pointCountList = pointResult.getPointCountList();
            List<PointCount> pointCountList1 = new ArrayList<>();
            for (int i = 0; i < pointCountList.size(); i++) {
                int pointPosition = pointCountList.get(i).getPointPosition();
                pointPosition++;
                for (String numStr : numbers) {
                    if (Integer.parseInt(numStr.trim()) == pointPosition) {
                        pointCountList1.add(pointCountList.get(i));
                    }
                }
            }
            PointResult pointResult1=new PointResult(0,pointCountList1);

            int position=0;
            for(int i=0;i<pointResult1.getPointCountList().size();i++){
                //记录每一个点位
                PointCount pointCount=pointResult1.getPointCountList().get(i);
                //切分序列,
                // 2024.12.3修改按时间切割
                if(pointCount.getCount()>0){
                    //存储点位配置
                    PubWaylinePointDfEntity pubWaylinePointDfEntity=pubWaylinePointDfService.getPointByWaylineIdAndSequence(wayline_id,String.valueOf(i));
                    //存储点位图片
                    List<MediaFileDTO> mediaFileDTOS=new ArrayList<>();
                    for(int j=0;j<pointCount.getCount();j++){
                        if(position<mediaFileDTOList1.size()){
                            //添加媒体图片
                            mediaFileDTOS.add(mediaFileDTOList1.get(position));
                        }


                        position=position+1;
                    }
                    pointCount.setMediaFileDTOS(mediaFileDTOS);
                    pointCount.setPubWaylinePointDfEntity(pubWaylinePointDfEntity);
                }
            }
            return pointResult1;
        }
    }

    @Override
    public List<UniPointEntity> getPointByJobId(String job_id) throws Exception {
        WaylineJobEntity waylineJobEntity = waylineJobMapper
                .selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, job_id));
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper
                .selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                        .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
        String waylinePointPos = pubWaylineJobPlanDfEntity.getWaylinePointPos();
        String[] numbers = waylinePointPos.split(",");
        List<UniPointEntity> uniPointEntities = uniPointMapper.selectList(new LambdaQueryWrapper<UniPointEntity>()
                .in(UniPointEntity::getWaylinePointPos, Arrays.asList(numbers)));
        return uniPointEntities;
    }

    @Override
    public PointResult getMediaFileByPoint(String job_id,Integer point_pos, String workspace_id, String wayline_id) throws Exception {
        synchronized (lock){
            List<MediaFileDTO> mediaFileDTOList = this.getFilesByJobId(job_id);
            List<MediaFileDTO> mediaFileDTOList1 = new  ArrayList<>();
            for (int i = 0; i < mediaFileDTOList.size(); i++) {
                String fileName = mediaFileDTOList.get(i).getFileName();
                int i2 = extractNumber(fileName);
                    if (point_pos == i2) {
                        mediaFileDTOList1.add(mediaFileDTOList.get(i));
                }
            }
            //排序
            for (int i = 0; i < mediaFileDTOList1.size()- 1; i++) {
                // 设置一个标志位，用于检测是否发生了交换
                boolean swapped = false;
                // 内层循环，从第一个元素到倒数第i个元素
                for (int j = 0; j < mediaFileDTOList1.size() - i - 1; j++) {
                    //获取第一张图片的时间数
                    //获取第j张照片名称
                    String T1fileName = mediaFileDTOList1.get(j).getFileName();
                    //记录第J张照片时间
                    String strT1num ="";
                    //记录下划线
                    int T1count = 0;
                    for(int i1=0;i1<T1fileName.length();i1++){
                        if (T1fileName.charAt(i1) == '_'){
                            T1count++;
                        }
                        if(T1count==1&&T1fileName.charAt(i1) != '_'){
                            strT1num=strT1num+T1fileName.charAt(i1);
                        }
                        if(T1count==2){
                            break;
                        }


                    }
                    Long T1num=Long.valueOf(strT1num);

                    //获取第二张图片时间数
                    String T2fileName = mediaFileDTOList1.get(j+1).getFileName();
                    //记录第J张照片时间
                    String strT2num ="";
                    //记录下划线
                    int T2count = 0;
                    for(int i1=0;i1<T2fileName.length();i1++){
                        if (T2fileName.charAt(i1) == '_'){
                            T2count++;
                        }
                        if(T2count==1&&T2fileName.charAt(i1) != '_'){
                            strT2num=strT2num+T2fileName.charAt(i1);
                        }
                        if(T2count==2){
                            break;
                        }


                    }
                    Long T2num=Long.valueOf(strT2num);
                    // 如果当前元素比下一个元素大，则交换它们
                    if (T1num> T2num) {
                        MediaFileDTO temp = mediaFileDTOList1.get(j);
                        mediaFileDTOList1.set(j,mediaFileDTOList1.get(j+1));
                        mediaFileDTOList1.set(j+1,temp);
                        swapped = true;
                    }
                }
                // 如果没有发生交换，说明列表已经有序，提前退出
                if (!swapped) {
                    break;
                }
            }
            // System.out.println(mediaFileDTOList);
            //第一步解析航线一共有多少张图片，每个点位有几张图片
            //获取下载路径
            URL url = waylineFileService.getObjectUrl(workspace_id, wayline_id);
            PointResult pointResult=waylineKmzThirdService.getPointResult(workspace_id,wayline_id,url,singlePointUrl);
            //照片位置记录
            //记录图片在数组里的位置
            List<PointCount> pointCountList = pointResult.getPointCountList();
            List<PointCount> pointCountList1 = new ArrayList<>();
            for (int i = 0; i < pointCountList.size(); i++) {
                int pointPosition = pointCountList.get(i).getPointPosition();
                pointPosition++;
                    if (point_pos == pointPosition) {
                        pointCountList1.add(pointCountList.get(i));
                    }
            }
            PointResult pointResult1=new PointResult(0,pointCountList1);

            int position=0;
            for(int i=0;i<pointResult1.getPointCountList().size();i++){
                //记录每一个点位
                PointCount pointCount=pointResult1.getPointCountList().get(i);
                //切分序列,
                // 2024.12.3修改按时间切割
                if(pointCount.getCount()>0){
                    //存储点位配置
                    PubWaylinePointDfEntity pubWaylinePointDfEntity=pubWaylinePointDfService.getPointByWaylineIdAndSequence(wayline_id,String.valueOf(i));
                    //存储点位图片
                    List<MediaFileDTO> mediaFileDTOS=new ArrayList<>();
                    for(int j=0;j<pointCount.getCount();j++){
                        if(position<mediaFileDTOList1.size()){
                            //添加媒体图片
                            mediaFileDTOS.add(mediaFileDTOList1.get(position));
                        }


                        position=position+1;
                    }
                    pointCount.setMediaFileDTOS(mediaFileDTOS);
                    pointCount.setPubWaylinePointDfEntity(pubWaylinePointDfEntity);
                }
            }
            return pointResult1;
        }
    }

    public static int extractNumber(String filename) {
        // 正则匹配"航点"后的连续数字
        Pattern pattern = Pattern.compile("航点(\\d+)");
        Matcher matcher = pattern.matcher(filename);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1; // 未找到时返回-1
    }

    /**
     * Convert the received file object into a database entity object.
     * @param file
     * @return
     */
    private MediaFileEntity fileUploadConvertToEntity(MediaUploadCallbackRequest file) {
        MediaFileEntity.MediaFileEntityBuilder builder = MediaFileEntity.builder();

        if (file != null) {
            builder.fileName(file.getName())
                    .filePath(file.getPath())
                    .fingerprint(file.getFingerprint())
                    .objectKey(file.getObjectKey())
                    .subFileType(Optional.ofNullable(file.getSubFileType()).map(MediaSubFileTypeEnum::getType).orElse(null))
                    .isOriginal(file.getExt().getOriginal())
                    .jobId(file.getExt().getFileGroupId())
                    .drone(file.getExt().getSn())
                    .tinnyFingerprint(file.getExt().getTinnyFingerprint())
                    .payload(file.getExt().getPayloadModelKey().getDevice());

            // domain-type-subType
            DeviceEnum payloadModelKey = file.getExt().getPayloadModelKey();
            Optional<DeviceDictionaryDTO> payloadDict = deviceDictionaryService
                    .getOneDictionaryInfoByTypeSubType(payloadModelKey.getDomain().getDomain(),
                            payloadModelKey.getType().getType(), payloadModelKey.getSubType().getSubType());
            payloadDict.ifPresent(payload -> builder.payload(payload.getDeviceName()));
        }
        return builder.build();
    }

    /**
     * Convert database entity objects into file data transfer object.
     * @param entity
     * @return
     */
    private MediaFileDTO entityConvertToDto(MediaFileEntity entity) {
        MediaFileDTO.MediaFileDTOBuilder builder = MediaFileDTO.builder();

        if (entity != null) {
            builder.fileName(entity.getFileName())
                    .fileId(entity.getFileId())
                    .filePath(entity.getFilePath())
                    .isOriginal(entity.getIsOriginal())
                    .fingerprint(entity.getFingerprint())
                    .objectKey(entity.getObjectKey())
                    .tinnyFingerprint(entity.getTinnyFingerprint())
                    .payload(entity.getPayload())
                    .createTime(entity.getCreateTime())
//                    .createTime(LocalDateTime.ofInstant(
//                            Instant.ofEpochMilli(entity.getCreateTime()), ZoneId.systemDefault()))
                    .drone(entity.getDrone())
                    .jobId(entity.getJobId());

        }

        return builder.build();
    }
    //生成缩略图方法实现
    @Override
    public boolean getThumbnailByJobId(String workspaceId, String fileId) {
//        // 查询所有属于指定 workspaceId 的 MediaFileEntity 记录
//        List<MediaFileEntity> fileList = mapper.selectList(new LambdaQueryWrapper<MediaFileEntity>()
//                .eq(MediaFileEntity::getWorkspaceId, workspaceId));
//
//        // 遍历查询到的每一条记录
//        for (MediaFileEntity fileEntity : fileList) {
//            String fileId = fileEntity.getFileId();
//        }
        DeleteFile deleteFile = new DeleteFile();
        // 1. 下载图片
        // 1.1 下载路径
        String targetFolder = singlePointUrl + "dji_thermal_sdk_v1.6_20240927/temTest";

        // 1.2 获取文件信息
        Optional<com.dji.sample.media.model.MediaFileEntity> mediaFileEntity = fileService.getMediaByFileId(workspaceId, fileId);

        // 1.4 检查数据库图像信息
        String targetFileName = "";
        Path outputPath = null;

        // 1.5 检查图片是否存入数据库
        if (mediaFileEntity.isPresent()) {
            targetFileName = mediaFileEntity.get().getFileName();
            outputPath = Path.of(targetFolder, targetFileName);
        } else {
            return false;  // 如果文件信息未找到，返回 false
        }

        // 1.6 获取下载路径url
        URL imageRemoteAddr = fileService.getObjectUrl(workspaceId, fileId);

        // 1.7 下载图片
        try (InputStream in = imageRemoteAddr.openStream()) {
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return false;  // 下载图片失败，返回 false
        }

        // 1.8 生成缩略图
        ByteArrayOutputStream thumbnailStream = createThumbnail(outputPath);
        if (thumbnailStream == null) {
            System.out.println("Thumbnail creation failed.");
            return false;  // 如果生成缩略图失败，返回 false
        }

        // 1.9 上传字节流到 MinIO
        String objectKey = "thumbnails/" + outputPath.getFileName().toString();
        try (InputStream thumbnailInputStream = new ByteArrayInputStream(thumbnailStream.toByteArray())) {
            ossService.putObject(OssConfiguration.bucket, objectKey, thumbnailInputStream);
            System.out.println("Thumbnail uploaded successfully to MinIO");
        } catch (IOException e) {
            deleteFile.deleteFileInFolder(targetFolder, mediaFileEntity.get().getFileName());
            System.out.println("Error uploading thumbnail to MinIO: " + e.getMessage());
            return false;  // 上传失败，返回 false
        }

        // 2.0 插入数据库
        Integer result = saveThumbnail(workspaceId, fileId, objectKey);
        if (result == 0) {
            deleteFile.deleteFileInFolder(targetFolder, mediaFileEntity.get().getFileName());
            return false;  // 如果保存缩略图失败，返回 false
        }
        deleteFile.deleteFileInFolder(targetFolder, mediaFileEntity.get().getFileName());
        return true;  // 如果所有处理都成功，返回 true
    }
    private ByteArrayOutputStream createThumbnail(Path imagePath) {
        try {
            // 读取图片文件
            File originalImageFile = imagePath.toFile();
            BufferedImage originalImage = ImageIO.read(originalImageFile);

            if (originalImage == null) {
                System.out.println("Failed to read the image.");
                return null;
            }

            // 设置缩略图尺寸（例如：宽度 100px，高度自动缩放）
            int width = 100;
            int height = (int) (originalImage.getHeight() * ((double) width / originalImage.getWidth()));

            // 创建缩略图
            Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);

            // 将缩略图转换为 BufferedImage
            BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();

            // 使用 ByteArrayOutputStream 直接生成字节流，避免中间文件存储
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", baos); // 可以选择其他格式，如 png 等
            baos.flush();

            return baos;

//            // 生成缩略图的路径（可以选择与原图文件名不同的名称，如加 "_thumb" 后缀）
//            String thumbnailFileName = "thumb_" + originalImageFile.getName();
//            Path thumbnailPath = imagePath.getParent().resolve(thumbnailFileName);
//
//            // 保存缩略图
//            ImageIO.write(thumbnail, "jpg", thumbnailPath.toFile());
//            System.out.println("Thumbnail created at: " + thumbnailPath);

        } catch (IOException e) {
            System.out.println("Error generating thumbnail: " + e.getMessage());
            return null;
        }
    }

    // 插入缩略图数据到数据库
    private Integer saveThumbnail(String workspaceId, String fileId, String objectKey) {
        MediaThumbnailEntity fileEntity = MediaThumbnailEntity.builder()
                .fileId(fileId)               // 设置 fileId
                .thumbnailId(UUID.randomUUID().toString())  // 使用 UUID 生成 thumbnailId
                .workspaceId(workspaceId)      // 设置 workspaceId
                .objectKey(objectKey)          // 设置 objectKey
                .build();

        return thumbnailMapper.insert(fileEntity);
    }

    // 获取缩略图的url 下载缩略图
    @Override
    public URL getThumbnailUrl(String workspaceId, String fileId) {
        Optional<MediaThumbnailEntity> mediaFileOpt = getThumbnaiByFileId(workspaceId, fileId);
        if (mediaFileOpt.isEmpty()) {
            throw new IllegalArgumentException("{} doesn't exist.");
        }
        return ossService.getObjectUrl(OssConfiguration.bucket, mediaFileOpt.get().getObjectKey());
    }

    private Optional<MediaThumbnailEntity> getThumbnaiByFileId(String workspaceId, String fileId) {
        MediaThumbnailEntity fileEntity = thumbnailMapper.selectOne(new LambdaQueryWrapper<MediaThumbnailEntity>()
                .eq(MediaThumbnailEntity::getWorkspaceId, workspaceId)
                .eq(MediaThumbnailEntity::getFileId, fileId));
        return Optional.ofNullable(fileEntity);
    }

}
