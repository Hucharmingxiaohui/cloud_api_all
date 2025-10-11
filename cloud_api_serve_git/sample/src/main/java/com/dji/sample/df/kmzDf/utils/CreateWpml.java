package com.dji.sample.df.kmzDf.utils;


import com.dji.sample.df.kmzDf.entity.wayline.Folder.Folder;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.GlobalWaypointHeadingParam.GlobalWaypointHeadingParam;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.PayloadParam.PayloadParam;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.Action;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.ActionActuatorFuncParam;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.CustomDirName.CustomDirName;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.GimbalRotate.GimbalRotate;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.Hover.Hover;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.OrientedShoot.OrientedShoot;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.PanoShot.PanoShot;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.RotateYaw.RotateYaw;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.StartRecord.StartRecord;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.StopRecord.StopRecord;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.TakePhoto.TakePhoto;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.Zoom.Zoom;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.ActionGroup;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.ActionTrigger.ActionTrigger;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.Placemark;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.Point.Point;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.WaypointHeadingParam.WaypointHeadingParam;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.WaypointTurnParam.WaypointTurnParam;
import com.dji.sample.df.kmzDf.entity.wayline.MissionConfig.DroneInfo.DroneInfo;
import com.dji.sample.df.kmzDf.entity.wayline.MissionConfig.MissionConfig;
import com.dji.sample.df.kmzDf.entity.wayline.MissionConfig.PayloadInfo.PayloadInfo;
import com.dji.sample.df.kmzDf.entity.wayline.Wayline;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;


public class CreateWpml {
    public String createWpml(Wayline wayline, String createKmzUrl) throws IOException {
        //根节点kml
        Element root = DocumentHelper.createElement("kml");
        Document document = DocumentHelper.createDocument(root);
        //给根节点kml添加属性
        root.addNamespace("","http://www.opengis.net/kml/2.2");
        root.addNamespace("wpml","http://www.dji.com/wpmz/1.0.6");
        //给根节点kml添加子节点  Document
        Element documentElement = root.addElement("Document");
        //获取全局任务信息
        MissionConfig missionConfig = wayline.getMissionConfig();
        //添加wpml:missionConfig节点，必须元素
        Element missionConfigElement = documentElement.addElement("wpml:missionConfig");
            //添加wpml:flyToWaylineMode节点，必须元素
            missionConfigElement.addElement("wpml:flyToWaylineMode").addText(missionConfig.getFlyToWaylineMode());
            //添加wpml:finishAction节点，必须元素
            missionConfigElement.addElement("wpml:finishAction").addText(missionConfig.getFinishAction());
            //添加wpml:exitOnRCLost节点，必须元素
            missionConfigElement.addElement("wpml:exitOnRCLost").addText(missionConfig.getExitOnRCLost());
            //添加wpml:exitOnRCLost节点，必须元素
            missionConfigElement.addElement("wpml:executeRCLostAction").addText(missionConfig.getExecuteRCLostAction());
            //添加wpml:takeOffSecurityHeight节点，安全起飞高度
            String takeOffSecurityHeight = Float.toString(missionConfig.getTakeOffSecurityHeight());
            missionConfigElement.addElement("wpml:takeOffSecurityHeight").addText(takeOffSecurityHeight);
            //飞向首航点速度wpml:globalTransitionalSpeed
            String globalTransitionalSpeed = Float.toString(missionConfig.getGlobalTransitionalSpeed());
            missionConfigElement.addElement("wpml:globalTransitionalSpeed").addText(globalTransitionalSpeed);

            //无人机信息
            DroneInfo droneInfo = missionConfig.getDroneInfo();
            Element droneInfoElement = missionConfigElement.addElement("wpml:droneInfo");
            {
                //无人机型号
                String droneEnumValue = Integer.toString(droneInfo.getDroneEnumValue());
                droneInfoElement.addElement("wpml:droneEnumValue").addText(droneEnumValue);
                //无人机子型号
                String droneSubEnumValue = Integer.toString(droneInfo.getDroneSubEnumValue());
                droneInfoElement.addElement("wpml:droneSubEnumValue").addText(droneSubEnumValue);
            }
            //无人机负载信息
            Element payloadInfoElement = missionConfigElement.addElement("wpml:payloadInfo");
            PayloadInfo payloadInfo = missionConfig.getPayloadInfo();
            {
                //负载型号
                String payloadEnumValue = Integer.toString(payloadInfo.getPayloadEnumValue());
                payloadInfoElement.addElement("wpml:payloadEnumValue").addText(payloadEnumValue);
                //挂载负载子型号
                String payloadSubEnumValue = Integer.toString(payloadInfo.getPayloadSubEnumValue());
                payloadInfoElement.addElement("wpml:payloadSubEnumValue").addText(payloadSubEnumValue);
                //负载挂载位置
                String payloadPositionIndex = Integer.toString(payloadInfo.getPayloadPositionIndex());
                payloadInfoElement.addElement("wpml:payloadPositionIndex").addText(payloadPositionIndex);
            }

        Element folderElement = documentElement.addElement("Folder");//Folder节点
        Folder folder = wayline.getFolder();
        PayloadParam payloadParam =folder.getPayloadParam();//获取无人机负载参数
                {
            //给Folder添加子节点wpml:templateId
            folderElement.addElement("wpml:templateId").addText(Integer.toString(folder.getTemplateId()));
            //给Folder添加wpml:executeHeightMode
            folderElement.addElement("wpml:executeHeightMode").addText(folder.getExecuteHeightMode());
            //给Folder添加航线id
            folderElement.addElement("wpml:waylineId").addText(Integer.toString(folder.getWaylineId()));
            //全局飞行速度
            String autoFlightSpeed = Float.toString(folder.getAutoFlightSpeed());
            folderElement.addElement("wpml:autoFlightSpeed").addText(autoFlightSpeed);

            //循环添加每一个Placemark节点，有几个坐标点就有几个Placemark节点，这里仅作示例
            List<Placemark> placeMarks =folder.getPlaceMarks();
            for (int i = 0; i <placeMarks.size(); i++) {
                Element placeMarkElement = folderElement.addElement("Placemark");
                //航点坐标
                Element PointElement = placeMarkElement.addElement("Point");
                Placemark placemark = placeMarks.get(i);
                Point point = placemark.getPoint();
                {
                    PointElement.addElement("coordinates").addText(point.getCoordinates());
                }
                //航点序号
                placeMarkElement.addElement("wpml:index").addText(String.valueOf(i));
                //航点执行高度
                //判断是否使用全局高度
                if(placemark.getUseGlobalHeight()==1){
                    String height= Float.toString(folder.getGlobalHeight());
                    placeMarkElement.addElement("wpml:executeHeight").addText(height);
                }else {
                    String height= Float.toString(placemark.getHeight());
                    placeMarkElement.addElement("wpml:executeHeight").addText(height);
                }

                //航点速度
                //判断有没有使用全局速度
                if(placemark.getUseGlobalSpeed()==1)
                {
                    String waypointSpeed = Float.toString(folder.getAutoFlightSpeed());
                    placeMarkElement.addElement("wpml:waypointSpeed").addText(waypointSpeed);
                }else{
                    String waypointSpeed = Float.toString(placemark.getWaypointSpeed());
                    placeMarkElement.addElement("wpml:waypointSpeed").addText(waypointSpeed);
                }


                //偏航角参数
                Element waypointHeadingParamElement =  placeMarkElement.addElement("wpml:waypointHeadingParam");

                //是否使用全局偏航角参数
                if(placemark.getUseGlobalHeadingParam()==1)
                {
                    GlobalWaypointHeadingParam globalWaypointHeadingParam=folder.getGlobalWaypointHeadingParam();
                    //偏航角模式
                    waypointHeadingParamElement.addElement("wpml:waypointHeadingMode").addText(globalWaypointHeadingParam.getWaypointHeadingMode());
                    //偏航角度
                    String waypointHeadingAngle = Float.toString(globalWaypointHeadingParam.getWaypointHeadingAngle());
                    waypointHeadingParamElement.addElement("wpml:waypointHeadingAngle").addText(waypointHeadingAngle);
                    //兴趣点
                    waypointHeadingParamElement.addElement("wpml:waypointPoiPoint").addText(globalWaypointHeadingParam.getWaypointPoiPoint());
                    //waypointHeadingAngleEnable
                    waypointHeadingParamElement.addElement("wpml:waypointHeadingAngleEnable").addText("0");
                    waypointHeadingParamElement.addElement("wpml:waypointHeadingPathMode").addText(globalWaypointHeadingParam.getWaypointHeadingPathMode());
                    //waypointHeadingPoiIndex
                    waypointHeadingParamElement.addElement("wpml:waypointHeadingPoiIndex").addText(Integer.toString(globalWaypointHeadingParam.getWaypointHeadingPoiIndex()));
                }else {
                    WaypointHeadingParam waypointHeadingParam=placemark.getWaypointHeadingParam();
                    //偏航角模式
                    waypointHeadingParamElement.addElement("wpml:waypointHeadingMode").addText(waypointHeadingParam.getWaypointHeadingMode());
                    //偏航角度
                    String waypointHeadingAngle = Float.toString(waypointHeadingParam.getWaypointHeadingAngle());
                    waypointHeadingParamElement.addElement("wpml:waypointHeadingAngle").addText(waypointHeadingAngle);
                    //兴趣点
                    waypointHeadingParamElement.addElement("wpml:waypointPoiPoint").addText(waypointHeadingParam.getWaypointPoiPoint());
                    //waypointHeadingAngleEnable
                    waypointHeadingParamElement.addElement("wpml:waypointHeadingAngleEnable").addText("0");
                    waypointHeadingParamElement.addElement("wpml:waypointHeadingPathMode").addText(waypointHeadingParam.getWaypointHeadingPathMode());
                    //waypointHeadingPoiIndex
                    waypointHeadingParamElement.addElement("wpml:waypointHeadingPoiIndex").addText(Integer.toString(waypointHeadingParam.getWaypointHeadingPoiIndex()));
                }


                //添加wpml:waypointTurnParam 航点类型航点转弯类型
                Element waypointTurnParamElement=placeMarkElement.addElement("wpml:waypointTurnParam");
                //判断是否使用全局转弯类型
                if(placemark.getUseGlobalTurnParam()==1)
                {
                    waypointTurnParamElement.addElement("wpml:waypointTurnMode").addText(folder.getGlobalWaypointTurnMode());
                    waypointTurnParamElement.addElement("wpml:waypointTurnDampingDist").addText("0");
                }else{
                    WaypointTurnParam waypointTurnParam = placemark.getWaypointTurnParam();
                    waypointTurnParamElement.addElement("wpml:waypointTurnMode").addText(waypointTurnParam.getWaypointTurnMode());
                    waypointTurnParamElement.addElement("wpml:waypointTurnDampingDist").addText(Double.toString(waypointTurnParam.getWaypointTurnDampingDist()));
                }

                //useStraightLine
                placeMarkElement.addElement("useStraightLine").addText("1");

                //添加动作组
                ActionGroup actionGroup=placemark.getActionGroup();
                //判断动作组是否为空
                if(actionGroup!=null){
                    //添加航线动作
                    Element actionGroupElement = placeMarkElement.addElement("wpml:actionGroup");
                    {
                        actionGroupElement.addElement("wpml:actionGroupId").addText(String.valueOf(i));
                        actionGroupElement.addElement("wpml:actionGroupStartIndex").addText(Integer.toString(actionGroup.getActionGroupStartIndex()));
                        actionGroupElement.addElement("wpml:actionGroupEndIndex").addText(Integer.toString(actionGroup.getActionGroupEndIndex()));
                        actionGroupElement.addElement("wpml:actionGroupMode").addText(actionGroup.getActionGroupMode());
                        //触发动作
                        Element actionTriggerElement = actionGroupElement.addElement("wpml:actionTrigger");
                        ActionTrigger actionTrigger=actionGroup.getActionTrigger();
                        actionTriggerElement.addElement("wpml:actionTriggerType").addText(actionTrigger.getActionTriggerType());
                        //判断是否是间隔拍照
                        if("multipleTiming".equals(actionTrigger.getActionTriggerType())||"multipleDistance".equals(actionTrigger.getActionTriggerType())){
                            //间隔拍照
                            actionTriggerElement.addElement("wpml:actionTriggerParam").addText(Float.toString(actionTrigger.getActionTriggerParam()));
                        }
                        //循环添加具体的动作
                        List<Action> actionList = actionGroup.getActionList();
                        for (int j =0; j < actionList.size(); j++){
                            Action action =actionList.get(j);//获取动作对象
                            //拍照
                            if("takePhoto".equals(action.getActionActuatorFunc()))
                            {
                                Element actionElement =actionGroupElement.addElement("wpml:action");
                                actionElement.addElement("wpml:actionId").addText(Integer.toString(j));
                                actionElement.addElement("wpml:actionActuatorFunc").addText("takePhoto");
                                ActionActuatorFuncParam actionActuatorFuncParam=action.getActionActuatorFuncParam();//动作参数
                                TakePhoto takePhoto = actionActuatorFuncParam.getTakePhoto();//获取拍照参数

                                Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                                //判断是不是间隔拍照
                                if(actionTrigger.getActionTriggerType().equals("multipleTiming")||actionTrigger.getActionTriggerType().equals("multipleDistance"))
                                {
                                    actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText(Integer.toString(takePhoto.getPayloadPositionIndex()));//挂载位置
                                    //是否使用全局负载存储类型
                                    if(takePhoto.getUseGlobalPayloadLensIndex()==1){//是
                                        actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("1");
                                        actionActuatorFuncParamElement.addElement("wpml:payloadLensIndex").addText(payloadParam.getImageFormat());
                                    }else {
                                        actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("0");
                                        actionActuatorFuncParamElement.addElement("wpml:payloadLensIndex").addText(takePhoto.getPayloadLensIndex());
                                    }

                                }else {
                                    String hangdian = takePhoto.getFileSuffix() + String.valueOf(i + 1);
                                    actionActuatorFuncParamElement.addElement("wpml:fileSuffix").addText(hangdian);//航点后缀
                                    actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText(Integer.toString(takePhoto.getPayloadPositionIndex()));//挂载位置
                                    //是否使用全局负载存储类型
                                    if(takePhoto.getUseGlobalPayloadLensIndex()==1){
                                        actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("1");
                                        actionActuatorFuncParamElement.addElement("wpml:payloadLensIndex").addText(takePhoto.getPayloadLensIndex());
                                    }else {
                                        actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("0");
                                        actionActuatorFuncParamElement.addElement("wpml:payloadLensIndex").addText(takePhoto.getPayloadLensIndex());
                                    }
                                }


                            }
                            //定向拍照
                            if("orientedShoot".equals(action.getActionActuatorFunc())){
                                Element actionElement =actionGroupElement.addElement("wpml:action");
                                actionElement.addElement("wpml:actionId").addText(Integer.toString(j));
                                actionElement.addElement("wpml:actionActuatorFunc").addText("orientedShoot");
                                ActionActuatorFuncParam actionActuatorFuncParam=action.getActionActuatorFuncParam();//动作参数
                                OrientedShoot orientedShoot = actionActuatorFuncParam.getOrientedShoot();//获取定向拍照参数

                                Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                                //云台府仰角
                                actionActuatorFuncParamElement.addElement("wpml:gimbalPitchRotateAngle").addText(Float.toString(orientedShoot.getGimbalPitchRotateAngle()));
                                //云台横滚角
                                actionActuatorFuncParamElement.addElement("wpml:gimbalRollRotateAngle").addText(Float.toString(orientedShoot.getGimbalRollRotateAngle()));
                                //云台偏航角
                                actionActuatorFuncParamElement.addElement("wpml:gimbalYawRotateAngle").addText(Float.toString(orientedShoot.getGimbalYawRotateAngle()));
                                actionActuatorFuncParamElement.addElement("wpml:focusX").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:focusY").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:focusRegionWidth").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:focusRegionHeight").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:focalLength").addText(Float.toString(orientedShoot.getFocalLength()));//焦距
                                actionActuatorFuncParamElement.addElement("wpml:aircraftHeading").addText(Float.toString(orientedShoot.getAircraftHeading()));//飞行器偏航角
                                actionActuatorFuncParamElement.addElement("wpml:accurateFrameValid").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText("0");
                                if(orientedShoot.getUseGlobalPayloadLensIndex()==1){
                                    actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("1");
                                    actionActuatorFuncParamElement.addElement("wpml:payloadLensIndex").addText(payloadParam.getImageFormat());

                                }else{
                                    actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("0");
                                    actionActuatorFuncParamElement.addElement("wpml:payloadLensIndex").addText(orientedShoot.getPayloadLensIndex());
                                }
                                actionActuatorFuncParamElement.addElement("wpml:targetAngle").addText("0");
                                UUID actionUUID = UUID.randomUUID();
                                actionActuatorFuncParamElement.addElement("wpml:actionUUID").addText(actionUUID.toString());
                                actionActuatorFuncParamElement.addElement("wpml:imageWidth").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:imageHeight").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:AFPos").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalPort").addText("0");//payloadInfo wpml:payloadEnumValue
                                actionActuatorFuncParamElement.addElement("wpml:orientedCameraType").addText(Integer.toString(payloadInfo.getPayloadEnumValue()));//相机型号
                                actionActuatorFuncParamElement.addElement("wpml:orientedFilePath").addText(actionUUID.toString());
                                actionActuatorFuncParamElement.addElement("wpml:orientedFileMD5");
                                actionActuatorFuncParamElement.addElement("wpml:orientedFileSize").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:fileSuffix").addText(orientedShoot.getOrientedFileSuffix()+i+1);
                                actionActuatorFuncParamElement.addElement("wpml:orientedPhotoMode").addText(orientedShoot.getOrientedPhotoMode());
                            }
                            //全景拍照
                            if("panoShot".equals(action.getActionActuatorFunc()))
                            {
                                Element actionElement =actionGroupElement.addElement("wpml:action");
                                actionElement.addElement("wpml:actionId").addText(Integer.toString(j));
                                actionElement.addElement("wpml:actionActuatorFunc").addText("panoShot");
                                ActionActuatorFuncParam actionActuatorFuncParam=action.getActionActuatorFuncParam();//动作参数
                                PanoShot panoShot = actionActuatorFuncParam.getPanoShot();//获取全景拍照参数

                                Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                                actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText(Integer.toString(panoShot.getPayloadPositionIndex()));//摄像头位置
                                //判断是否使用全局存储类型
                                if(panoShot.getUseGlobalPayloadLensIndex()==1)
                                {
                                    actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("1");
                                    actionActuatorFuncParamElement.addElement("wpml:payloadLensIndex").addText(payloadParam.getImageFormat());
                                }
                                else {
                                    actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("0");
                                    actionActuatorFuncParamElement.addElement("wpml:payloadLensIndex").addText(panoShot.getPayloadLensIndex());
                                }
                                actionActuatorFuncParamElement.addElement("wpml:panoShotSubMode").addText(panoShot.getPanoShotSubMode());
                            }
                            //开始录像
                            if("startRecord".equals(action.getActionActuatorFunc()))
                            {
                                Element actionElement =actionGroupElement.addElement("wpml:action");
                                actionElement.addElement("wpml:actionId").addText(Integer.toString(j));
                                actionElement.addElement("wpml:actionActuatorFunc").addText("startRecord");
                                ActionActuatorFuncParam actionActuatorFuncParam=action.getActionActuatorFuncParam();//动作参数
                                StartRecord startRecord = actionActuatorFuncParam.getStartRecord();//获取开始录像参数

                                Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                                String hangdian = startRecord.getFileSuffix() + String.valueOf(i + 1);
                                actionActuatorFuncParamElement.addElement("wpml:fileSuffix").addText(hangdian);
                                actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText(Integer.toString(startRecord.getPayloadPositionIndex()));
                                if(startRecord.getUseGlobalPayloadLensIndex()==1)
                                {
                                    actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("1");
                                    actionActuatorFuncParamElement.addText("wpml:payloadLensIndex").addText(payloadParam.getImageFormat());
                                }else{
                                    actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("0");
                                    actionActuatorFuncParamElement.addText("wpml:payloadLensIndex").addText(startRecord.getPayloadLensIndex());
                                }

                            }
                            //停止录像
                            if("stopRecord".equals(action.getActionActuatorFunc())){
                                Element actionElement =actionGroupElement.addElement("wpml:action");
                                actionElement.addElement("wpml:actionId").addText(Integer.toString(j));
                                actionElement.addElement("wpml:actionActuatorFunc").addText("stopRecord");
                                ActionActuatorFuncParam actionActuatorFuncParam=action.getActionActuatorFuncParam();//动作参数
                                StopRecord stopRecord = actionActuatorFuncParam.getStopRecord();//获取停止录像参数

                                Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                                actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText(Integer.toString(stopRecord.getPayloadPositionIndex()));
                            }
                            //云台府仰偏航角
                            if("gimbalRotate".equals(action.getActionActuatorFunc())){
                                Element actionElement =actionGroupElement.addElement("wpml:action");
                                actionElement.addElement("wpml:actionId").addText(Integer.toString(j));
                                actionElement.addElement("wpml:actionActuatorFunc").addText("gimbalRotate");
                                ActionActuatorFuncParam actionActuatorFuncParam=action.getActionActuatorFuncParam();//动作参数
                                GimbalRotate gimbalRotate = actionActuatorFuncParam.getGimbalRotate();//获取云台角度参数

                                Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalRotateMode").addText(gimbalRotate.getGimbalRotateMode());
                                actionActuatorFuncParamElement.addElement("wpml:gimbalPitchRotateEnable").addText(Integer.toString(gimbalRotate.getGimbalPitchRotateEnable()));
                                actionActuatorFuncParamElement.addElement("wpml:gimbalPitchRotateAngle").addText(Float.toString(gimbalRotate.getGimbalPitchRotateAngle()));
                                actionActuatorFuncParamElement.addElement("wpml:gimbalRollRotateEnable").addText(Integer.toString(gimbalRotate.getGimbalRollRotateEnable()));
                                actionActuatorFuncParamElement.addElement("wpml:gimbalRollRotateAngle").addText(Float.toString(gimbalRotate.getGimbalRollRotateAngle()));
                                actionActuatorFuncParamElement.addElement("wpml:gimbalYawRotateEnable").addText(Integer.toString(gimbalRotate.getGimbalYawRotateEnable()));
                                actionActuatorFuncParamElement.addElement("wpml:gimbalYawRotateAngle").addText(Float.toString(gimbalRotate.getGimbalYawRotateAngle()));
                                actionActuatorFuncParamElement.addElement("wpml:gimbalRotateTimeEnable").addText(Integer.toString(gimbalRotate.getGimbalRotateTimeEnable()));
                                actionActuatorFuncParamElement.addElement("wpml:gimbalRotateTime").addText(Float.toString(gimbalRotate.getGimbalRotateTime()));
                                actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText(Integer.toString(gimbalRotate.getPayloadPositionIndex()));
                            }
                            //相机变焦
                            if("zoom".equals(action.getActionActuatorFunc())){
                                Element actionElement =actionGroupElement.addElement("wpml:action");
                                actionElement.addElement("wpml:actionId").addText(Integer.toString(j));
                                actionElement.addElement("wpml:actionActuatorFunc").addText("zoom");
                                ActionActuatorFuncParam actionActuatorFuncParam=action.getActionActuatorFuncParam();//动作参数
                                Zoom zoom = actionActuatorFuncParam.getZoom();//获取云台角度参数

                                Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                                actionActuatorFuncParamElement.addElement("wpml:focalLength").addText(Float.toString(zoom.getFocalLength()));
                                actionActuatorFuncParamElement.addElement("wpml:isUseFocalFactor").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText(Integer.toString(zoom.getPayloadPositionIndex()));

                            }
                            //创建文件夹
                            if("customDirName".equals(action.getActionActuatorFunc()))
                            {
                                Element actionElement =actionGroupElement.addElement("wpml:action");
                                actionElement.addElement("wpml:actionId").addText(Integer.toString(j));
                                actionElement.addElement("wpml:actionActuatorFunc").addText("customDirName");
                                ActionActuatorFuncParam actionActuatorFuncParam=action.getActionActuatorFuncParam();//动作参数
                                CustomDirName customDirName = actionActuatorFuncParam.getCustomDirName();//获取文件夹参数

                                Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                                actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText(Integer.toString(customDirName.getPayloadPositionIndex()));
                                actionActuatorFuncParamElement.addElement("wpml:directoryName").addText(customDirName.getDirectoryName());
                            }
                            //悬停
                            if("hover".equals(action.getActionActuatorFunc())){
                                Element actionElement =actionGroupElement.addElement("wpml:action");
                                actionElement.addElement("wpml:actionId").addText(Integer.toString(j));
                                actionElement.addElement("wpml:actionActuatorFunc").addText("hover");
                                ActionActuatorFuncParam actionActuatorFuncParam=action.getActionActuatorFuncParam();//动作参数
                                Hover hover = actionActuatorFuncParam.getHover();//获取悬停参数

                                Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                                actionActuatorFuncParamElement.addElement("wpml:hoverTime").addText(Float.toString(hover.getHoverTime()));
                            }
                            //飞行器偏航
                            if("rotateYaw".equals(action.getActionActuatorFunc()))
                            {
                                Element actionElement =actionGroupElement.addElement("wpml:action");
                                actionElement.addElement("wpml:actionId").addText(Integer.toString(j));
                                actionElement.addElement("wpml:actionActuatorFunc").addText("rotateYaw");
                                ActionActuatorFuncParam actionActuatorFuncParam=action.getActionActuatorFuncParam();//动作参数
                                RotateYaw rotateYaw = actionActuatorFuncParam.getRotateYaw();//获取飞行器偏航参数

                                Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                                actionActuatorFuncParamElement.addElement("wpml:aircraftHeading").addText(Float.toString(rotateYaw.getAircraftHeading()));
                                actionActuatorFuncParamElement.addElement("wpml:aircraftPathMode").addText(rotateYaw.getAircraftPathMod());

                            }
                            //判断是否为第一个航点
                            if(i==0){//如果是就添加如下动作
                                Element actionElement =actionGroupElement.addElement("wpml:action");
                                actionElement.addElement("wpml:actionId").addText(Integer.toString(j));
                                actionElement.addElement("wpml:actionActuatorFunc").addText("gimbalRotate");

                                Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalHeadingYawBase").addText("aircraft");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalRotateMode").addText("absoluteAngle");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalPitchRotateEnable").addText("1");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalPitchRotateAngle").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalRollRotateEnable").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalRollRotateAngle").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalYawRotateEnable").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalYawRotateAngle").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalRotateTimeEnable").addText("0");
                                actionActuatorFuncParamElement.addElement("wpml:gimbalRotateTime").addText("10");
                                actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText("0");
                            }
                        }
                    }
                    //判断是否是最后一个航点，不是就添加gimbalEvenlyRotate
                    if(i!=placeMarks.size()-1)
                    {
                        Element actionGroupElement1 = placeMarkElement.addElement("wpml:actionGroup");

                        actionGroupElement1.addElement("wpml:actionGroupId").addText(Integer.toString(placeMarks.size()+1+i));
                        actionGroupElement1.addElement("wpml:actionGroupStartIndex").addText(Integer.toString(i));//起始航点
                        actionGroupElement1.addElement("wpml:actionGroupEndIndex").addText(Integer.toString(i+1));//结束航点
                        actionGroupElement1.addElement("wpml:actionGroupMode").addText("sequence");

                        Element actionTriggerElement = actionGroupElement1.addElement("wpml:actionTrigger");
                        actionTriggerElement.addElement("wpml:actionTriggerType").addText("betweenAdjacentPoints");

                        Element actionElement =actionGroupElement1.addElement("wpml:action");
                        actionElement.addElement("wpml:actionId").addText("0");
                        actionElement.addElement("wpml:actionActuatorFunc").addText("gimbalEvenlyRotate");

                        Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");//下一个航点的府仰角
                        actionActuatorFuncParamElement.addElement("wpml:gimbalPitchRotateAngle").addText(Float.toString(placeMarks.get(i+1).getGimbalPitchAngle()));
                        actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText("0");

                    }
                }
                //调整云台
                Element waypointGimbalHeadingParam = placeMarkElement.addElement("waypointGimbalHeadingParam");
                waypointGimbalHeadingParam.addElement("waypointGimbalPitchAngle").addText("0");
                waypointGimbalHeadingParam.addElement("waypointGimbalYawAngle").addText("0");
                //是否为风险点
                placeMarkElement.addElement("isRisky").addText("0");
                placeMarkElement.addElement("waypointWorkType").addText("0");
            }
        }


        //将wpml写出本地
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");//设置编码格式
        XMLWriter xmlWriter = new XMLWriter(new FileOutputStream(  createKmzUrl + "/wpmz/waylines.wpml"),format);
        xmlWriter.write(document);
        xmlWriter.close();
        System.out.println("success");
        return "success";
    }
}
