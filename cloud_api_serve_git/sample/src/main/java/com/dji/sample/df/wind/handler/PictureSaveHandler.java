package com.dji.sample.df.wind.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.ftp.FtpsHelper;
import com.df.framework.redis.RedisUtils;
import com.df.framework.vo.Result;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.wind.config.FjFileConfig;
import com.dji.sample.df.wind.dao.FanWaylinePointsMapper;
import com.dji.sample.df.wind.model.entity.FanWaylinePoints;
import com.dji.sample.manage.dao.IWorkspaceMapper;
import com.dji.sample.manage.model.entity.WorkspaceEntity;
import com.dji.sample.media.service.IFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PictureSaveHandler {

    @Resource
    IFileMapperDf iFileMapperDf;

    @Resource
    private IFileService fileService;

    @Autowired
    private FjFileConfig fileConfig;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    IWorkspaceMapper workspaceMapper;

    @Resource
    FanWaylinePointsMapper fanWaylinePointsMapper;

    public Result<Map> pictureSave(String jobId) {
        List<MediaFileEntity> mediaFileEntities = iFileMapperDf.selectList(new LambdaQueryWrapper<MediaFileEntity>().eq(MediaFileEntity::getJobId, jobId));
        // 分离DJI文件和非DJI文件
        List<MediaFileEntity> djiFiles = new ArrayList<>();
        List<MediaFileEntity> nonDjiFiles = new ArrayList<>();

        for (MediaFileEntity file : mediaFileEntities) {
            if (file.getFileName().startsWith("DJI")) {
                djiFiles.add(file);
            } else {
                nonDjiFiles.add(file);
            }
        }
        WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());
        // 从数据库获取图片命名规则
//        String fanPointsJson = redisUtils.get("fanPoints").toString();

        FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
                .eq(FanWaylinePoints::getJobId, jobId));
        JSONArray points =new JSONArray();
        if (fanWaylinePoints != null) {
//           风机存在本地
            points = JSON.parseArray(fanWaylinePoints.getDjiFanPoints());
            Map map = new HashMap();
            try {
                int index = 0;
                for (MediaFileEntity mediaFileEntity : djiFiles) {
                    URL url = fileService.getObjectUrl(workspaceEntity.getWorkspaceId(), mediaFileEntity.getFileId());
//               // 使用Redis中的命名规则
                    String fileName;
                    if (index < points.size()) {
                        String pointName = points.getString(index);
                        fileName = pointName;
                    } else {
                        // 如果图片数量超过Redis规则，使用原始文件名
                        fileName = mediaFileEntity.getFileName() != null ?
                                mediaFileEntity.getFileName() :
                                "file_" + mediaFileEntity.getFileId() + ".dat";
                    }
                    String filePictrueUrl = fileConfig.getFilePictrueUrl();
                    String localFileName = downloadAndConvertToJpeg(url.toString(), fileName, filePictrueUrl, jobId);
                    map.put(fileName, localFileName);
                    index++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Result.success(map);
        }else {
            Map map = new HashMap();
            try {
                int index = 0;
                for (MediaFileEntity mediaFileEntity : djiFiles) {
                    URL url = fileService.getObjectUrl(workspaceEntity.getWorkspaceId(), mediaFileEntity.getFileId());
//               // 使用Redis中的命名规则
                    String fileName;
                    if (index < points.size()) {
                        String pointName = points.getString(index);
                        fileName = pointName;
                    } else {
                        // 如果图片数量超过Redis规则，使用原始文件名
                        fileName = mediaFileEntity.getFileName() != null ?
                                mediaFileEntity.getFileName() :
                                "file_" + mediaFileEntity.getFileId() + ".dat";
                    }
                    String filePictrueUrl = fileConfig.getRecfilePath()+fileConfig.getRecfileNativePath();
                    String localFileName = downloadAndConvertToJpeg2(url.toString(), fileName, filePictrueUrl, jobId,fileConfig.getRecfileNativePath());
                    map.put(fileName, localFileName);
                    index++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Result.success(map);
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

            System.out.println("JPEG图片保存成功: " + localFilePath);
            return localFilePath;

        } catch (IOException e) {
            System.err.println("图片转换失败: " + e.getMessage());
            throw e;
        }
    }

    public static String downloadAndConvertToJpeg2(String url, String localFileName, String filePictrueUrl,String jobId,String nativePath) throws IOException {
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

            System.out.println("JPEG图片保存成功: " + localFilePath);
            return nativePath + jobId+ "/" + localFileName;

        } catch (IOException e) {
            System.err.println("图片转换失败: " + e.getMessage());
            throw e;
        }
    }

}
