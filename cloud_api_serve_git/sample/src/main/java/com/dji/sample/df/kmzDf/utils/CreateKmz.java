package com.dji.sample.df.kmzDf.utils;




import com.dji.sample.df.kmzDf.entity.wayline.Wayline;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CreateKmz {
    public static String craeteKmz(Wayline wayline, String createKmzUrl) throws IOException {
        CreateKml kml = new CreateKml();
        CreateWpml wpml = new CreateWpml();
        kml.createKml(wayline,createKmzUrl);
        wpml.createWpml(wayline,createKmzUrl);
        String sourceDirPath = createKmzUrl + "/wpmz"; // 源文件夹路径
        String  kmzName = wayline.getMissionConfig().getFileName() + ".kmz";
        String zipFilePath =  createKmzUrl + "/" + kmzName; // 压缩后的文件路径

        try {
            zipDirectory(sourceDirPath, zipFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return zipFilePath;
    }
    private static void zipDirectory(String sourceDirPath, String zipFilePath) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zos = new ZipOutputStream(fos);
        File fileToZip = new File(sourceDirPath);

        zipFile(fileToZip, fileToZip.getName(), zos);
        zos.close();
        fos.close();
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zos.putNextEntry(new ZipEntry(fileName));
                zos.closeEntry();
            } else {
                zos.putNextEntry(new ZipEntry(fileName + "/"));
                zos.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zos);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        fis.close();
    }

}

