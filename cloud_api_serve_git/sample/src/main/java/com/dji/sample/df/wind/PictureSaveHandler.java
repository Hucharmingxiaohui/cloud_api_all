package com.dji.sample.df.wind;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.config.FileConfig;
import com.df.framework.ftp.FtpsHelper;
import com.df.framework.redis.RedisUtils;
import com.df.framework.vo.Result;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.wind.config.FjFileConfig;
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
import java.util.List;

import static com.google.common.io.Files.getFileExtension;

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

    public Result pictureSave(String jobId) {
        List<MediaFileEntity> mediaFileEntities = iFileMapperDf.selectList(new LambdaQueryWrapper<MediaFileEntity>().eq(MediaFileEntity::getJobId, jobId));
        WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());
        FtpsHelper ftpClient = new FtpsHelper();
        // 从Redis获取图片命名规则
        String fanPointsJson = redisUtils.get("fanPoints").toString();
        JSONArray points = JSON.parseArray(fanPointsJson);

        if (points == null || points.isEmpty()) {
            System.err.println("错误：从Redis获取图片命名规则失败");
            return Result.error("错误：从Redis获取图片命名规则失败");
        }

        // 检查图片数量是否匹配
        if (mediaFileEntities.size() != points.size()) {
            System.err.println("警告：图片数量(" + mediaFileEntities.size() +
                    ")与Redis命名规则数量(" + points.size() + ")不匹配");
        }

        try {
            int index = 0;
            for (MediaFileEntity mediaFileEntity : mediaFileEntities) {
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
                index++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.success("保存结果成功");
    }

    public static String downloadFromUrl(String url, String localFileName,String filePictrueUrl,String jobId) throws IOException {
        // 创建临时目录,回头修改
//        String tempDir = "D:\\save";
        String tempDir = filePictrueUrl + "/" +jobId;
        Path tempPath = Paths.get(tempDir);
        if (!Files.exists(tempPath)) {
            Files.createDirectories(tempPath);
        }

        String localFilePath = tempDir + "/"+ localFileName;

        try (InputStream in = new URL(url).openStream();
             FileOutputStream out = new FileOutputStream(localFilePath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("文件下载完成: " + localFilePath);
        return localFilePath;
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


    public static void main(String[] args) {

    }
}
