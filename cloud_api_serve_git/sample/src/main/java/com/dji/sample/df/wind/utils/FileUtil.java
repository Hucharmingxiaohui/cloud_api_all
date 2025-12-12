package com.dji.sample.df.wind.utils;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.*;

public class FileUtil {

    public static MultipartFile convert(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在：" + filePath);
        }
        // 创建DiskFileItem（commons-fileupload核心类）
        FileItem fileItem = new DiskFileItem(
                "file", // form表单字段名
                "application/vnd.google-earth.kmz", // KMZ文件MIME类型
                false, // 是否为表单字段（false表示文件）
                file.getName(), // 文件名
                (int) file.length(), // 文件大小
                file.getParentFile() // 临时文件存储目录
        );

        // 将文件内容写入FileItem
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = fileItem.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        }

        // 包装为CommonsMultipartFile（Spring兼容的MultipartFile实现）
        return new CommonsMultipartFile(fileItem);
    }


}
