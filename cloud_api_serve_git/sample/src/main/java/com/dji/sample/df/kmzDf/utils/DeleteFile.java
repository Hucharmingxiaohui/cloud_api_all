package com.dji.sample.df.kmzDf.utils;

import java.io.File;

//删除某个文件夹下的文件
public class DeleteFile {

    public static boolean deleteFileInFolder(String folderPath, String fileName) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("文件夹不存在");
            return false;
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().equals(fileName)) {
                    return file.delete();
                }
            }
        }
        System.out.println("未找到指定文件");
        return false;
    }
}
