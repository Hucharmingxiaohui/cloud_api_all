package com.dji.sample.df.kmzDf.service.impl;


import com.dji.sample.df.kmzDf.utils.KmzKml;
import com.dji.sample.df.kmzDf.utils.DeleteFile;
import com.dji.sample.df.kmzDf.entity.wayline.Wayline;
import com.dji.sample.df.kmzDf.service.WaylineKmzService;
import com.dji.sample.df.kmzDf.utils.MyMultipartFile;
import com.dji.sample.df.kmzDf.utils.GetWaylineInfo;
import com.dji.sample.wayline.service.IWaylineFileService;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.dji.sample.df.kmzDf.utils.CreateKmz.*;

@Service
public class WaylineKmzServiceImpl implements WaylineKmzService {
    @Value("${createKmzUrl}")
    private String createKmzUrl;
    @Autowired
    IWaylineFileService waylineFileService;
    @Override//创建航点航线
    public String CreateWaypointWaylineKmz(String workspaceId, Wayline wayline, String creator) throws IOException {
        String kmzFileUrl = craeteKmz(wayline,createKmzUrl);
        System.out.println(kmzFileUrl);
        MyMultipartFile myMultipartFile = new MyMultipartFile(kmzFileUrl);
        waylineFileService.importKmzFile(myMultipartFile, workspaceId, creator);
        //将文件夹的数据删除
        DeleteFile deleteFile = new DeleteFile();
        deleteFile.deleteFileInFolder(createKmzUrl,wayline.getMissionConfig().getFileName()+".kmz");
        deleteFile.deleteFileInFolder(createKmzUrl+"/wpmz","template.kml");
        deleteFile.deleteFileInFolder(createKmzUrl+"/wpmz","waylines.wpml");
        return "success";
    }

    @Override
    public Wayline GetKmzWaypointWayline(String workspaceId, String  waylineId, URL url, String targetFolder) throws Exception {
        // 原有的方法内容
        //1.下载航线到指定位置
        String targetFileName = "航点飞行";
        try {
            try (InputStream in = url.openStream()) {
                Path outputPath = Path.of(targetFolder, targetFileName + ".kmz");
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //2.解析下载的航线文件为kml格式
        KmzKml kmz = new KmzKml();
        Document unzipKmzToKml = kmz.unzipKmzToKml(targetFolder+targetFileName + ".kmz");

        // 将dom4j 的document对象转换成String
        // String asXML = unzipKmzToKml.asXML();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
        Date date = new Date();
        String fileName = sdf.format(date);
        String strkml = targetFolder+targetFileName + ".kml";

        // 创建kml到本地
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("utf-8");
        XMLWriter xmlWriter = new XMLWriter(new FileOutputStream(strkml),format);
        xmlWriter.write(unzipKmzToKml);
        xmlWriter.close();

        System.out.println("\n**********************     【KMZ转kml成功】kml路径：       **********************\n"+ strkml);
        //3.将kml格式的坐标提取出来
        File file = new File(strkml);
        InputStream in  = new FileInputStream(file);
        GetWaylineInfo getWayLineInfo =new GetWaylineInfo();
        Wayline wayline = getWayLineInfo.parseXmlWithDom4j(in);
        //将文件夹的数据删除
        com.dji.sample.df.commonDf.util.DeleteFile deleteFile = new com.dji.sample.df.commonDf.util.DeleteFile();
        deleteFile.deleteFileInFolder(targetFolder,targetFileName+".kmz");
        deleteFile.deleteFileInFolder(targetFolder,targetFileName+".kml");
        return wayline;
    }
}
