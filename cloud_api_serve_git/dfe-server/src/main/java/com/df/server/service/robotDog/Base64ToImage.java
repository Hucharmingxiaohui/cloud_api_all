package com.df.server.service.robotDog;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class Base64ToImage {

    public static void decodeAndSave(String base64Image, String outputPath) throws IOException {
        // 1. 去除可能存在的MIME类型前缀
        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }

        // 2. 解码Base64字符串
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        // 3. 写入文件（使用try-with-resources确保流关闭）
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            fos.write(imageBytes);
        }
    }

    public static void main(String[] args) throws IOException {
        ImageToBase64Converter imageToBase64Converter = new ImageToBase64Converter();
        String base64Str = imageToBase64Converter.create();
        String content = new String(Files.readAllBytes(Paths.get("C:\\Users\\90828\\Desktop\\test.txt")), StandardCharsets.UTF_8);

//        String base64Str = "你的Base64字符串"; // 替换为实际Base64字符串
//        String outputPath = "C:/temp/restored_image.png";
        String outputPath = System.getProperty("user.home") + "/Downloads/restored.png";

        try {
            decodeAndSave(content, outputPath);
            System.out.println("图片还原成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
