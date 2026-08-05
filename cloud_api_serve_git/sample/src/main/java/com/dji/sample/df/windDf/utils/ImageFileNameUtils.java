package com.dji.sample.df.windDf.utils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片文件名处理工具类
 */
public class ImageFileNameUtils {

    /**
     * 将单个文件名或路径中的 .jpeg 后缀（不区分大小写）替换为 .jpg
     * 如果不是以 .jpeg 结尾，则原样返回
     *
     * @param fileName 原始文件名（可包含路径）
     * @return 转换后的文件名
     */
    public static String jpegToJpg(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }
        // 不区分大小写匹配以 .jpeg 结尾
        if (fileName.toLowerCase().endsWith(".jpeg")) {
            // 去掉最后5个字符 (.jpeg) 再加上 .jpg
            return fileName.substring(0, fileName.length() - 5) + ".jpg";
        }
        return fileName;
    }

    /**
     * 将文件列表中的每个文件名进行 jpeg -> jpg 转换
     *
     * @param fileNames 原始文件名列表
     * @return 转换后的文件名列表
     */
    public static List<String> jpegToJpg(List<String> fileNames) {
        if (fileNames == null) {
            return null;
        }
        return fileNames.stream()
                .map(ImageFileNameUtils::jpegToJpg)
                .collect(Collectors.toList());
    }

    /**
     * 将文件数组中的每个文件名进行 jpeg -> jpg 转换
     *
     * @param fileNames 原始文件名数组
     * @return 转换后的文件名数组
     */
    public static String[] jpegToJpg(String[] fileNames) {
        if (fileNames == null) {
            return null;
        }
        String[] result = new String[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            result[i] = jpegToJpg(fileNames[i]);
        }
        return result;
    }
}
