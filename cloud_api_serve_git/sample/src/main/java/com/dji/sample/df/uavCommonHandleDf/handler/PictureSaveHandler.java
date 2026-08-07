package com.dji.sample.df.uavCommonHandleDf.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.vo.Result;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.windDf.config.FjFileConfig;
import com.dji.sample.df.windDf.dao.FanWaylinePointsMapper;
import com.dji.sample.df.windDf.model.entity.FanWaylinePoints;
import com.dji.sample.manage.dao.IWorkspaceMapper;
import com.dji.sample.manage.model.entity.WorkspaceEntity;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PictureSaveHandler {
    @Resource
    IFileMapperDf iFileMapperDf;
    @Resource
    private IFileService fileService;
    @Autowired
    private FjFileConfig fileConfig;
    @Resource
    IWorkspaceMapper workspaceMapper;
    @Resource
    FanWaylinePointsMapper fanWaylinePointsMapper;
    @Autowired
    private IWaylineJobMapper waylineJobMapper;
    @Autowired
    UniPointMapper2 uniPointMapper2;
    @Autowired
    PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;

    public Result<Map> pictureSave(String jobId) throws IOException {
        List<MediaFileEntity> mediaFileEntities = iFileMapperDf.selectList(new LambdaQueryWrapper<MediaFileEntity>().eq(MediaFileEntity::getJobId, jobId));
        // 分离DJI文件和非DJI文件（应该都是DJI文件）
        List<MediaFileEntity> djiFiles = new ArrayList<>();
        List<MediaFileEntity> nonDjiFiles = new ArrayList<>();
        for (MediaFileEntity file : mediaFileEntities) {
            if (file.getFileName().startsWith("DJI")) {
                djiFiles.add(file);
            } else {
                nonDjiFiles.add(file);
            }
        }
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>().eq(WaylineJobEntity::getJobId, jobId));
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanId, waylineJobEntity.getPlanId()));
        WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());
        FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
                .eq(FanWaylinePoints::getJobId, jobId));
        JSONArray points =new JSONArray();
        if (fanWaylinePoints != null && pubWaylineJobPlanDfEntity.getPlanType()==1) {
//          1.风机图片保存
            points = JSON.parseArray(fanWaylinePoints.getDjiFanPoints());
            Map map = new HashMap();
            try {
                int index = 0;
                for (MediaFileEntity mediaFileEntity : djiFiles) {
                    URL url = fileService.getObjectUrl(workspaceEntity.getWorkspaceId(), mediaFileEntity.getFileId());
//                  按顺序以fanWaylinePoints存的点位名称给照片命名
                    String fileName;
                    if (index < points.size()) {
                        String pointName = points.getString(index);
                        fileName = pointName;
                    } else {
//                      如果多拍照了，则多出来的按原命名
                        fileName = mediaFileEntity.getFileName() != null ?
                                mediaFileEntity.getFileName() :
                                "file_" + mediaFileEntity.getFileId() + ".dat";
                    }
                    String filePictrueUrl = fileConfig.getFilePictrueUrl();
                    String localFilePath = downloadAndConvertToJpeg(url.toString(), fileName, filePictrueUrl, jobId);
                    map.put(fileName, localFilePath);
                    index++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Result.success(map);
        }else if(pubWaylineJobPlanDfEntity.getPlanType()==0){
//          2.航点航线图片保存
            Map map = new HashMap();
            try {
                int index = 0;
                for (MediaFileEntity mediaFileEntity : djiFiles) {
                    URL url = fileService.getObjectUrl(workspaceEntity.getWorkspaceId(), mediaFileEntity.getFileId());
                    String fileName;
                    if (index < points.size()) {
                        String pointName = points.getString(index);
                        fileName = pointName;
                    } else {
                        fileName = mediaFileEntity.getFileName() != null ?
                                mediaFileEntity.getFileName() :
                                "file_" + mediaFileEntity.getFileId() + ".dat";
                    }
                    Integer pointPos = extractWaypointNumber(fileName);
                    String picType = extractTOrV(fileName);
                    Integer picType1 = 0;
                    if(picType.equals("V")){
                        picType1 = 0;
                    }else if(picType.equals("T")){
                        picType1 = 1;
                    }
                    log.info(fileName+"的类型为"+picType);
                    UniPoint uniPoint = uniPointMapper2.selectOne(new LambdaQueryWrapper<UniPoint>()
                            .eq(UniPoint::getWaylineId, waylineJobEntity.getFileId())
                            .eq(UniPoint::getWaylinePointPos, pointPos)
                            .eq(UniPoint::getPicType,picType1));
                    if(uniPoint == null){
                        log.info("未查到对应点位-----");
                        continue;
                    }
//                  对接分析服务唯一标识
                    String regId=uniPoint.getPointCode()+picType;
//                  航点航线计划照片保存形式为：点位编码+照片类型+"_"+图片原名
                    String filePictrueUrl = fileConfig.getRecfilePath()+fileConfig.getRecfileNativePath();
                    String localFilePath = downloadAndConvertToJpeg2(regId,url.toString(), fileName, filePictrueUrl, jobId,fileConfig.getRecfileNativePath());
                    log.info("保存对应点位-----");
                    map.put(fileName, localFilePath);
                    index++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Result.success(map);
        }else if(pubWaylineJobPlanDfEntity.getPlanType()==4){
            // 光伏图片保存
            Map map = new HashMap();
            try {
                int index = 0;
                for (MediaFileEntity mediaFileEntity : djiFiles) {
                    URL url = fileService.getObjectUrl(workspaceEntity.getWorkspaceId(), mediaFileEntity.getFileId());
//                  按顺序以fanWaylinePoints存的点位名称给照片命名
                    String fileName;
                    if (index < points.size()) {
                        String pointName = points.getString(index);
                        fileName = pointName;
                    } else {
//                      如果多拍照了，则多出来的按原命名
                        fileName = mediaFileEntity.getFileName() != null ?
                                mediaFileEntity.getFileName() :
                                "file_" + mediaFileEntity.getFileId() + ".dat";
                    }
                    String filePictrueUrl = fileConfig.getFilePictrueUrl();
                    String localFilePath = downloadAndConvertToJpeg3(url.toString(), fileName, filePictrueUrl, jobId);
                    map.put(fileName, localFilePath);
                    index++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Result.success(map);
        }else if(pubWaylineJobPlanDfEntity.getPlanType()==3){
//          普通航线图片保存
            Map map = new HashMap();
            int index = 0;
            for (MediaFileEntity mediaFileEntity : djiFiles) {
                URL url = fileService.getObjectUrl(workspaceEntity.getWorkspaceId(), mediaFileEntity.getFileId());
                String fileName;
                if (index < points.size()) {
                    String pointName = points.getString(index);
                    fileName = pointName;
                } else {
                    fileName = mediaFileEntity.getFileName() != null ?
                            mediaFileEntity.getFileName() :
                            "file_" + mediaFileEntity.getFileId() + ".dat";
                }
                String regId="1";
//              普通照片regId保存形式为：1
                String filePictrueUrl = fileConfig.getRecfilePath()+fileConfig.getRecfileNativePath();
                String localFilePath = downloadAndConvertToJpeg2(regId,url.toString(), fileName, filePictrueUrl, jobId,fileConfig.getRecfileNativePath());
                log.info("保存对应点位-----");
                map.put(fileName, localFilePath);
                index++;
            }
            return Result.success(map);
        }else {
            return Result.success(new HashMap<>());
        }

    }

    public static String downloadAndConvertToJpeg(String url, String localFileName, String filePictrueUrl,String jobId) throws IOException {
        String tempDir = filePictrueUrl + "/" +jobId;
        Path tempPath = Paths.get(tempDir);
        if (!Files.exists(tempPath)) {
            Files.createDirectories(tempPath);
        }

        // 修复：检查文件名是否有后缀，如果没有则直接添加.jpg
        String fileNameWithoutExt;
        if (localFileName.contains(".")) {
            fileNameWithoutExt = localFileName.substring(0, localFileName.lastIndexOf('.'));
        } else {
            fileNameWithoutExt = localFileName;
        }
        localFileName = fileNameWithoutExt + ".jpg";

        String localFilePath = tempDir + "/" + localFileName;

        try {
            // 从URL读取图片
            URL imageUrl = new URL(url);
            BufferedImage image = ImageIO.read(imageUrl);

            if (image == null) {
                throw new IOException("无法从URL读取图片: " + url);
            }

            // 创建JPG文件并保存
            File outputFile = new File(localFilePath);
            boolean success = ImageIO.write(image, "jpg", outputFile);

            if (!success) {
                throw new IOException("无法将图片保存为JPEG格式");
            }

            log.info("JPEG图片保存成功: " + localFilePath);
            return localFilePath;

        } catch (IOException e) {
            System.err.println("图片转换失败: " + e.getMessage());
            throw e;
        }
    }

    public static String downloadAndConvertToJpeg2(String regId,String url, String localFileName,
                                                   String filePictrueUrl, String jobId, String nativePath) throws IOException {
        String tempDir = filePictrueUrl + "/" + jobId;
        Path tempPath = Paths.get(tempDir);
        if (!Files.exists(tempPath)) {
            Files.createDirectories(tempPath);
        }

        // 修复：检查文件名是否有后缀，如果没有则直接添加.jpg
        String fileNameWithoutExt;
        if (localFileName.contains(".")) {
            fileNameWithoutExt = localFileName.substring(0, localFileName.lastIndexOf('.'));
        } else {
            fileNameWithoutExt = localFileName;
        }
        localFileName = fileNameWithoutExt + ".jpg";

        String localFilePath = tempDir + "/" + regId+"_"+localFileName;

        try {
            // 检查URL是否以jpg/jpeg结尾（不区分大小写）
            String lowerUrl = url.toLowerCase();

            // 注意：有些URL可能包含查询参数，所以先去除查询参数再检查扩展名
            String urlWithoutQuery = lowerUrl.split("\\?")[0];

            if (urlWithoutQuery.endsWith(".jpg") || urlWithoutQuery.endsWith(".jpeg")) {
                // 如果是JPEG图片，直接下载保存，避免重新编码
                System.out.println("检测到JPEG格式，直接下载保存...");

                try (InputStream in = new URL(url).openStream();
                     FileOutputStream out = new FileOutputStream(localFilePath)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    System.out.println("JPEG图片直接下载成功: " + localFilePath);
                }

            } else {
                // 非JPEG格式，进行转换
                System.out.println("非JPEG格式，进行转换...");

                // 从URL读取图片
                URL imageUrl = new URL(url);
                BufferedImage image = ImageIO.read(imageUrl);

                if (image == null) {
                    throw new IOException("无法从URL读取图片: " + url);
                }

                // 创建JPG文件并保存
                File outputFile = new File(localFilePath);
                boolean success = ImageIO.write(image, "jpg", outputFile);

                if (!success) {
                    throw new IOException("无法将图片保存为JPEG格式");
                }

                System.out.println("图片成功转换为JPEG格式: " + localFilePath);
            }

            return nativePath + jobId + "/" + regId+"_"+localFileName;

        } catch (IOException e) {
            System.err.println("图片处理失败: " + e.getMessage());
            throw e;
        }
    }

    public static String downloadAndConvertToJpeg3(String url, String localFileName, String filePictrueUrl, String jobId) throws IOException {
        // 创建临时目录
        String tempDir = filePictrueUrl + "/" + jobId;
        Path tempPath = Paths.get(tempDir);
        if (!Files.exists(tempPath)) {
            Files.createDirectories(tempPath);
        }

        // 提取文件名（不含原始扩展名）
        String fileNameWithoutExt;
        if (localFileName.contains(".")) {
            fileNameWithoutExt = localFileName.substring(0, localFileName.lastIndexOf('.'));
        } else {
            fileNameWithoutExt = localFileName;
        }
        // 强制使用 .jpg 后缀（但文件内容保持原始字节不变）
        String finalFileName = fileNameWithoutExt + ".jpg";
        String localFilePath = tempDir + "/" + finalFileName;

        // 直接通过 HTTP 流下载并写入文件，不经过图像解码
        try (InputStream in = new URL(url).openStream();
             OutputStream out = new FileOutputStream(localFilePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.err.println("下载图片失败: " + e.getMessage());
            throw e;
        }

        System.out.println("图片保存成功（已保留所有原始信息）: " + localFilePath);
        return localFilePath;
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

}
