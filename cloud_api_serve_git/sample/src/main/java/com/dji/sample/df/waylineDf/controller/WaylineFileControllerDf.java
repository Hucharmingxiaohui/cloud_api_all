package com.dji.sample.df.waylineDf.controller;

import com.dji.sample.df.commonDf.util.DeleteFile;
import com.dji.sample.df.commonDf.util.Kml;
import com.dji.sample.df.commonDf.util.KmzKml;
import com.dji.sample.df.waylineDf.kmz.entity.Placemark;
import com.dji.sample.df.waylineDf.kmz.entity.Wayline;
import com.dji.sample.df.waylineDf.kmz.utils.GetWaylineInfo;
import com.dji.sample.df.waylineDf.kmz.utils.MyMultipartFile;
import com.dji.sample.df.waylineDf.service.IWaylineFileServiceDf;
import com.dji.sdk.common.HttpResultResponse;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.dji.sample.df.waylineDf.kmz.utils.CreateKmz.craeteKmz;


/**
 * @author sean
 * @version 0.3
 * @date 2021/12/22
 */
@RestController
@RequestMapping("wayline/api/v1")
public class WaylineFileControllerDf {
///wayline/api/v1/workspaces/e3dea0f5-37f2-4d79-ae58-490af3228069/waylines
// /57d8730e-7fb1-4cb3-a862-650c4aee4b8f/llPoint
    @Autowired
    private IWaylineFileServiceDf waylineFileService;

    //对航点飞行文件加锁，只让一个用户调用
    private final Object lock = new Object();
    //获取文件夹路径
    @Value("${singlePointUrl}")
    private String singlePointUrl;

    //解析航点飞行文件
    @GetMapping("/workspaces/{workspace_id}/waylines/{wayline_id}/llPoint")
    public HttpResultResponse<?> llPoint(@PathVariable(name = "workspace_id") String workspaceId,
                                         @PathVariable(name = "wayline_id") String waylineId) throws Exception {
        //对航点飞行文件加锁，只让一个用户调用
        synchronized (lock) {
            // 原有的方法内容
            //1.下载航线到指定位置
            URL url = waylineFileService.getObjectUrl(workspaceId, waylineId);
            String targetFileName = "航点飞行";
            String targetFolder = singlePointUrl;
            System.out.println(singlePointUrl);
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
            Kml kml = new Kml();
            Collection<? extends String> coordinatesList = kml.parseXmlWithDom4j(in);
            List<?>  list = (List)coordinatesList;

            System.out.println("获取到的坐标值：");
            for (String coordinates : coordinatesList) {
                System.out.println(coordinates);
            }
            //将文件夹的数据删除
            DeleteFile deleteFile = new DeleteFile();
            deleteFile.deleteFileInFolder(targetFolder,targetFileName+".kmz");
            deleteFile.deleteFileInFolder(targetFolder,targetFileName+".kml");


            return HttpResultResponse.success(list);
        }

    }

    //解析航点飞行文件,获取经纬度、椭球高度
    @RequestMapping("/workspaces/{workspace_id}/waylines/{wayline_id}/xyzPoint")
    public HttpResultResponse<?> xyzPoint(@PathVariable(name = "workspace_id") String workspaceId,
                                          @PathVariable(name = "wayline_id") String waylineId) throws Exception {
        //对航点飞行文件加锁，只让一个用户调用
        //对航点飞行文件加锁，只让一个用户调用
        synchronized (lock) {
            // 原有的方法内容
            //1.下载航线到指定位置
            URL url = waylineFileService.getObjectUrl(workspaceId, waylineId);
            String targetFileName = "航点飞行";
            String targetFolder = singlePointUrl;
            System.out.println(singlePointUrl);
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
//            System.out.println(wayline);
            List<Placemark> placemarks=wayline.getFolder().getPlaceMarks();
            List<String[]> xyzList=new ArrayList<>();
            for(int i=0;i<placemarks.size();i++)
            {
                Placemark placemark=placemarks.get(i);
                String s=placemark.getCoordinates()+","+placemark.getEllipsoidHeight();
                String [] a=new String[]{s};
                xyzList.add(a);
            }
            //将文件夹的数据删除
            DeleteFile deleteFile = new DeleteFile();
            deleteFile.deleteFileInFolder(targetFolder,targetFileName+".kmz");
            deleteFile.deleteFileInFolder(targetFolder,targetFileName+".kml");
            return HttpResultResponse.success(xyzList);
        }

    }

    @Value("${createKmzUrl}")
    private String createKmzUrl;
    //创建航线
    @PostMapping("/workspaces/{workspace_id}/waylines/wayPointRoutePlanning")
    public HttpResultResponse<?> wayPointRoutePlanning (@PathVariable(name = "workspace_id") String workspaceId,
                                                        @RequestBody Wayline wayline,
                                                        @RequestParam String creator
                                                        ) throws IOException {
        String kmzFileUrl = craeteKmz(wayline,createKmzUrl);
        System.out.println(kmzFileUrl);
        MyMultipartFile myMultipartFile = new MyMultipartFile(kmzFileUrl);
        waylineFileService.importKmzFile(myMultipartFile, workspaceId, creator);
        //将文件夹的数据删除
        DeleteFile deleteFile = new DeleteFile();
        deleteFile.deleteFileInFolder(createKmzUrl,wayline.getMissionConfig().getFileName()+".kmz");
        deleteFile.deleteFileInFolder(createKmzUrl+"/wpmz","template.kml");
        deleteFile.deleteFileInFolder(createKmzUrl+"/wpmz","waylines.wpml");
        return HttpResultResponse.success();
    }
    //获取完整的航线信息进行编辑
    @GetMapping("/workspaces/{workspace_id}/waylines/{wayline_id}/getWayLineInfo")
    public HttpResultResponse<?> getWayLineInfo(@PathVariable(name = "workspace_id") String workspaceId,
                                         @PathVariable(name = "wayline_id") String waylineId) throws Exception {
        //对航点飞行文件加锁，只让一个用户调用
        synchronized (lock) {
            // 原有的方法内容
            //1.下载航线到指定位置
            URL url = waylineFileService.getObjectUrl(workspaceId, waylineId);
            String targetFileName = "航点飞行";
            String targetFolder = singlePointUrl;
            System.out.println(singlePointUrl);
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
//            System.out.println(wayline);
            //将文件夹的数据删除
            DeleteFile deleteFile = new DeleteFile();
            deleteFile.deleteFileInFolder(targetFolder,targetFileName+".kmz");
            deleteFile.deleteFileInFolder(targetFolder,targetFileName+".kml");
            return HttpResultResponse.success(wayline);
        }

    }



}
