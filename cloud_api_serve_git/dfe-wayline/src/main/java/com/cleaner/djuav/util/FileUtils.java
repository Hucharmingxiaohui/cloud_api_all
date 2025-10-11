package com.cleaner.djuav.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

/**
 * Author:Cleaner
 * Date: 2025/5/29 17:16
 **/
public class FileUtils {
    public static File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        // 获取原始文件名
        String originalFilename = multipartFile.getOriginalFilename();
        // 创建临时文件（建议加上后缀）
        File file = File.createTempFile("temp-", originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : null);
        // 将 MultipartFile 转为 File
        multipartFile.transferTo(file);
        // JVM 退出时删除临时文件（可选）
        file.deleteOnExit();
        return file;
    }

    public static File downloadUrlToTempFile(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        File tempFile = Files.createTempFile("download-", "zip").toFile();
        try (InputStream in = url.openStream(); FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        tempFile.deleteOnExit(); // JVM 退出时删除
        return tempFile;
    }
}
