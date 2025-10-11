package com.dji.sample.df.thirdKmzDf.service.impl;


import com.dji.sample.df.commonDf.util.DeleteFile;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointCount.PointCount;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointResult;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.PayloadParam.PayloadParam;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.Action;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.OrientedShoot.OrientedShoot;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.PanoShot.PanoShot;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.TakePhoto.TakePhoto;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.ActionGroup;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.Placemark;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Wayline;
import com.dji.sample.df.thirdKmzDf.utils.GetWaylineInfo;
import com.dji.sample.df.thirdKmzDf.utils.KmzKml;
import com.dji.sample.df.thirdKmzDf.service.WaylineKmzThirdService;
import com.dji.sample.wayline.service.IWaylineFileService;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
public class WaylineKmzThirdServiceImpl implements WaylineKmzThirdService {
    @Autowired
    IWaylineFileService waylineFileService;

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
        DeleteFile deleteFile = new DeleteFile();
        deleteFile.deleteFileInFolder(targetFolder,targetFileName+".kmz");
        deleteFile.deleteFileInFolder(targetFolder,targetFileName+".kml");
        return wayline;
    }

    @Override
    public PointResult getPointResult(String workspaceId, String waylineId, URL url, String targetFolder) throws Exception {
        Wayline wayline = this.GetKmzWaypointWayline(workspaceId,waylineId,url,targetFolder);
        List<Placemark> placeMarks=wayline.getFolder().getPlaceMarks();//所有的点位信息
        PayloadParam payloadParam = wayline.getFolder().getPayloadParam();
        String str = payloadParam.getImageFormat();

        PointResult pointResult=new PointResult();//全局航点照片信息

        int kindsCount = 1;//全局类型数量
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ',') {
                kindsCount++;
            }
        }
        //存储整条航线的照片数量
        int allPointPic = 0;
        //存储所有航点照片信息
        List<PointCount> pointCountList = new ArrayList<>();
        for(int i = 0;i<placeMarks.size();i++){
            Placemark placemark=placeMarks.get(i);//单个航点信息
            PointCount pointCount=new PointCount();//单个航点照片信息
            pointCount.setPointPosition(placemark.getIndex());//位置
            int pointPhotoNum=0;
            //判断每一个航点的照片数量
            ActionGroup actionGroup=placemark.getActionGroup();
            if (placemark.getActionGroup()!=null){
                List<Action> actionList= actionGroup.getActionList();
                //单个航点处理
                for(int j=0;j<actionList.size();j++){
                    Action action = actionList.get(j);
                    //判断是否是拍照
                    if(action.getActionActuatorFunc().equals("takePhoto")){

                        TakePhoto takePhoto = action.getActionActuatorFuncParam().getTakePhoto();
                        if (takePhoto.getUseGlobalPayloadLensIndex()==1){
                            pointPhotoNum=pointPhotoNum+kindsCount;
                        }else {
                            String str1=takePhoto.getPayloadLensIndex();
                            int p1Count=1;
                            for (int p1 = 0;p1<str1.length();p1++){
                                if (str1.charAt(p1) == ',') {
                                    p1Count++;
                                }
                            }
                            pointPhotoNum=pointPhotoNum+p1Count;
                        }

                    }
                    //判断是否是定向拍照
                    if(action.getActionActuatorFunc().equals("orientedShoot")){
                        OrientedShoot orientedShoot = action.getActionActuatorFuncParam().getOrientedShoot();
                        if (orientedShoot.getUseGlobalPayloadLensIndex()==1){
                            pointPhotoNum=pointPhotoNum+kindsCount;
                        }else {
                            String str1=orientedShoot.getPayloadLensIndex();
                            int p1Count=1;
                            for (int p1 = 0;p1<str1.length();p1++){
                                if (str1.charAt(p1) == ',') {
                                    p1Count++;
                                }
                            }
                            pointPhotoNum=pointPhotoNum+p1Count;
                        }

                    }
                    //判断是否是全景拍照
                    if(action.getActionActuatorFunc().equals("panoShot")){
                        PanoShot panoShot = action.getActionActuatorFuncParam().getPanoShot();
                        if (panoShot.getUseGlobalPayloadLensIndex()==1){
                            pointPhotoNum=pointPhotoNum+kindsCount;
                        }else {
                            String str1=panoShot.getPayloadLensIndex();
                            int p1Count=1;
                            for (int p1 = 0;p1<str1.length();p1++){
                                if (str1.charAt(p1) == ',') {
                                    p1Count++;
                                }
                            }
                            pointPhotoNum=pointPhotoNum+p1Count;
                        }
                    }


                }
            }
            pointCount.setCount(pointPhotoNum);//每个航点的照片数量
            pointCountList.add(pointCount);
            allPointPic = allPointPic+pointPhotoNum;
        }
//        pointResult.setCounts(allPointPic);
        pointResult.setPointCountList(pointCountList);
        return pointResult;
    }
}
