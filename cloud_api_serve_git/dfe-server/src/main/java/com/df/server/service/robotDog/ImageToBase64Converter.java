package com.df.server.service.robotDog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class ImageToBase64Converter {

    public static String convert(String imagePath) throws IOException {
        // 路径标准化处理
        String normalizedPath = imagePath.replace("\\", "/");

        // 前置检查
        if (!Files.exists(Paths.get(normalizedPath))) {
            throw new IOException("文件不存在: " + normalizedPath);
        }
        if (!Files.isReadable(Paths.get(normalizedPath))) {
            throw new IOException("文件不可读: " + normalizedPath);
        }

        // 使用try-with-resources确保流关闭
        try (FileInputStream fis = new FileInputStream(normalizedPath)) {
            byte[] fileContent = new byte[(int) new File(normalizedPath).length()];
            fis.read(fileContent);

            String base64 = Base64.getEncoder().encodeToString(fileContent);
//            return "data:" + detectMimeType(normalizedPath) + ";base64," + base64;
            return  base64;
        }
    }

    private static String detectMimeType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        switch(extension) {
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "bmp": return "image/bmp";
            default: return "application/octet-stream";
        }
    }
    public String create() throws IOException {
        String path = "C:/Users/90828/Desktop/Snipaste_2025-07-31_10-47-20.png";
        return convert(path);
    }
    public static void main(String[] args) {
        try {
            // 测试时使用实际存在的文件路径
            String path = "C:/Users/90828/Desktop/Snipaste_2025-07-31_10-47-20.png";
            System.out.println(convert(path));
//            System.out.println(convert(path).substring(0, 50) + "...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
