package com.dji.sample.df.kmzDf.utils;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

//将KMZ航线转成KML航线
public class KmzKml {
    public Document unzipKmzToKml(String strkmz) throws Exception, Exception {
        //航线下载存储路径
//        String strkmz="/home/ych/KmzKml/航点飞行测试.kmz";

        System.out.println("**********************     【KMZ转kml开始】kmz路径：       **********************\n"+ strkmz);

        File file = new File(strkmz);

        ZipFile zipFile = new ZipFile(file);

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file));
        InputStream inputStream = null;
        ZipEntry entry = null;
        Document doc = null;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            String zipEntryName = entry.getName();
            //获取所需文件的节点
            if (zipEntryName.equals("wpmz/template.kml")) {

                inputStream = zipFile.getInputStream(entry);
                SAXReader reader = new SAXReader();
                doc = (Document) reader.read(inputStream);

                inputStream.close();
            }
        }
        zipFile.close();
        zipInputStream.close();
        return doc;
    }

}
