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
import com.dji.sample.df.kmzDf.entity.wayline.Folder.WaylineCoordinateSysParam.WaylineCoordinateSysParam;
import com.dji.sample.df.kmzDf.entity.wayline.MissionConfig.DroneInfo.DroneInfo;
import com.dji.sample.df.kmzDf.entity.wayline.MissionConfig.PayloadInfo.PayloadInfo;
import com.dji.sample.df.kmzDf.entity.wayline.MissionConfig.MissionConfig;
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

public class CreateKml {
    public String createKml(Wayline wayline, String createKmzUrl) throws IOException {
        //根节点kml
        Element root = DocumentHelper.createElement("kml");
        Document document = DocumentHelper.createDocument(root);
        //给根节点kml添加属性
        root.addNamespace("","http://www.opengis.net/kml/2.2");
        //root.addAttribute("xmlns:wpml", "http://www.dji.com/wpmz/1.0.2");
        root.addNamespace("wpml","http://www.dji.com/wpmz/1.0.6");
        //给根节点kml添加子节点  Document
        Element documentElement = root.addElement("Document");
        //文件创作时间，非必须元素
        long currentTimeMillis = System.currentTimeMillis();
        String createTime = Long.toString(currentTimeMillis);
        documentElement.addElement("wpml:createTime").addText(createTime);
        //文件更新时间，非必须元素
        documentElement.addElement("wpml:updateTime").addText(createTime);

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
        //过渡速度，起飞速度，飞向首航点wpml:globalTransitionalSpeed
        String globalTransitionalSpeed = Float.toString(missionConfig.getGlobalTransitionalSpeed());
        missionConfigElement.addElement("wpml:globalTransitionalSpeed").addText(globalTransitionalSpeed);

        //获取无人机信息
        DroneInfo droneInfo = missionConfig.getDroneInfo();
        //无人机信息
        Element droneInfoElement = missionConfigElement.addElement("wpml:droneInfo");
        //无人机型号
        String droneEnumValue = Integer.toString(droneInfo.getDroneEnumValue());
        droneInfoElement.addElement("wpml:droneEnumValue").addText(droneEnumValue);
        //无人机子型号
        String droneSubEnumValue = Integer.toString(droneInfo.getDroneSubEnumValue());
        droneInfoElement.addElement("wpml:droneSubEnumValue").addText(droneSubEnumValue);

        //无人机负载信息
        Element payloadInfoElement = missionConfigElement.addElement("wpml:payloadInfo");
        PayloadInfo payloadInfo = missionConfig.getPayloadInfo();
        //负载型号
        String payloadEnumValue = Integer.toString(payloadInfo.getPayloadEnumValue());
        payloadInfoElement.addElement("wpml:payloadEnumValue").addText(payloadEnumValue);
        //
        String payloadSubEnumValue = Integer.toString(payloadInfo.getPayloadSubEnumValue());
        payloadInfoElement.addElement("wpml:payloadSubEnumValue").addText(payloadSubEnumValue);
        //负载挂载位置
        String payloadPositionIndex = Integer.toString(payloadInfo.getPayloadPositionIndex());
        payloadInfoElement.addElement("wpml:payloadPositionIndex").addText(payloadPositionIndex);


        Element folderElement = documentElement.addElement("Folder");//Folder节点
        Folder folder = wayline.getFolder();
        //给Folder节点添加子节点,wpml:templateType,航点类型
        folderElement.addElement("wpml:templateType").addText(folder.getTemplateType());
        //给Folder添加子节点wpml:templateId
        folderElement.addElement("wpml:templateId").addText(Integer.toString(folder.getTemplateId()));

        //添加坐标系信息wpml:waylineCoordinateSysParam
        WaylineCoordinateSysParam waylineCoordinateSysParam=folder.getWaylineCoordinateSysParam();
        Element waylineCoordinateSysParamElement = folderElement.addElement("wpml:waylineCoordinateSysParam");
        waylineCoordinateSysParamElement.addElement("wpml:coordinateMode").addText(waylineCoordinateSysParam.getCoordinateMode());
        //添加wpml:heightMode到waylineCoordinateSysParam
        waylineCoordinateSysParamElement.addElement("wpml:heightMode").addText(waylineCoordinateSysParam.getHeightMode());
        //添加wpml:positioningType到waylineCoordinateSysParam
        waylineCoordinateSysParamElement.addElement("wpml:positioningType").addText(waylineCoordinateSysParam.getPositioningType());

        //给Folder添加全局自动飞行速度wpml:autoFlightSpeed
        String autoFlightSpeed = Float.toString(folder.getAutoFlightSpeed());
        folderElement.addElement("wpml:autoFlightSpeed").addText(autoFlightSpeed);
        //给Folder添加全局飞行高度wpml:globalHeight
        String globalHeight = Float.toString(folder.getGlobalHeight());
        folderElement.addElement("wpml:globalHeight").addText(globalHeight);
        //是否开启标定飞行，350RTK，300RTK适用
        folderElement.addElement("wpml:caliFlightEnable").addText(Integer.toString(folder.getCaliFlightEnable()));
        //云台模式wpml:gimbalPitchMode
        folderElement.addElement("wpml:gimbalPitchMode").addText(folder.getGimbalPitchMode());

        //全局偏航参数wpml:globalWaypointHeadingParam
        GlobalWaypointHeadingParam globalWaypointHeadingParam = folder.getGlobalWaypointHeadingParam();
        Element globalWaypointHeadingParamElement = folderElement.addElement("wpml:globalWaypointHeadingParam");
        //偏航方式飞行器偏航角模式
        globalWaypointHeadingParamElement.addElement("wpml:waypointHeadingMode").addText(globalWaypointHeadingParam.getWaypointHeadingMode());
        //偏航角度飞行器偏航角度
        globalWaypointHeadingParamElement.addElement("wpml:waypointHeadingAngle").addText(Float.toString(globalWaypointHeadingParam.getWaypointHeadingAngle()));
        //兴趣点waypointPoiPoint
        globalWaypointHeadingParamElement.addElement("wpml:waypointPoiPoint").addText(globalWaypointHeadingParam.getWaypointPoiPoint());
        //偏航参数顺序
        globalWaypointHeadingParamElement.addElement("wpml:waypointHeadingPoiIndex").addText(Integer.toString(globalWaypointHeadingParam.getWaypointHeadingPoiIndex()));

        //全局航点类型（全局航点转弯模式）
        String globalWaypointTurnMode = folder.getGlobalWaypointTurnMode();
        folderElement.addElement("wpml:globalWaypointTurnMode").addText(globalWaypointTurnMode);
        //全局航段轨迹是否尽量贴合直线wpml:globalUseStraightLine
        folderElement.addElement("wpml:globalUseStraightLine").addText(Integer.toString(folder.getGlobalUseStraightLine()));

        //循环添加每一个Placemark节点，有几个坐标点就有几个Placemark节点，这里仅作示例
        List<Placemark> placeMarks =folder.getPlaceMarks();
        for (int i = 0; i < placeMarks.size(); i++) {
            Element placeMarkElement = folderElement.addElement("Placemark");
            //航点坐标

            Element PointElement = placeMarkElement.addElement("Point");
            Placemark placemark = placeMarks.get(i);
            Point point = placemark.getPoint();
            PointElement.addElement("coordinates").addText(point.getCoordinates());
            //航点序号
            placeMarkElement.addElement("wpml:index").addText(String.valueOf(i));

            //是否使用全局高度
            if(placemark.getUseGlobalHeight()==1)
            {
                //航点高度,84xi
                String height= Float.toString(folder.getGlobalHeight());
                placeMarkElement.addElement("wpml:ellipsoidHeight").addText(height);
                //航点高度，96xi
                placeMarkElement.addElement("wpml:height").addText(height);
                placeMarkElement.addElement("wpml:useGlobalHeight").addText("1");
            }else{
                //航点高度,84xi
                String height= Float.toString(placemark.getHeight());
                placeMarkElement.addElement("wpml:ellipsoidHeight").addText(height);
                //航点高度，96xi
                placeMarkElement.addElement("wpml:height").addText(height);
            }
            //是否使用全局速度
            if(placemark.getUseGlobalSpeed()==1)
            {
                placeMarkElement.addElement("wpml:useGlobalSpeed").addText("1");
            }else
            {
                //航点速度
                String waypointSpeed = Float.toString(placemark.getWaypointSpeed());
                placeMarkElement.addElement("wpml:waypointSpeed").addText(waypointSpeed);
            }

            //是否使用全局偏航角
            if(placemark.getUseGlobalHeadingParam()==1)
            {
                placeMarkElement.addElement("wpml:useGlobalHeadingParam").addText("1");
            }else
            {
                //偏航角
                WaypointHeadingParam waypointHeadingParam = placemark.getWaypointHeadingParam();
                Element waypointHeadingParamElement = placeMarkElement.addElement("wpml:waypointHeadingParam");
                waypointHeadingParamElement.addElement("wpml:waypointHeadingMode").addText(waypointHeadingParam.getWaypointHeadingMode());
                String waypointHeadingAngle = Float.toString(waypointHeadingParam.getWaypointHeadingAngle());
                waypointHeadingParamElement.addElement("wpml:waypointHeadingAngle").addText(waypointHeadingAngle);
                waypointHeadingParamElement.addElement("wpml:waypointPoiPoint").addText(waypointHeadingParam.getWaypointPoiPoint());
                waypointHeadingParamElement.addElement("wpml:waypointHeadingPathMode").addText(waypointHeadingParam.getWaypointHeadingPathMode());
                waypointHeadingParamElement.addElement("wpml:waypointHeadingPoiIndex").addText(Integer.toString(waypointHeadingParam.getWaypointHeadingPoiIndex()));
            }
            //是否使用全局航点类型转弯模式
            if(placemark.getUseGlobalTurnParam()==1)
            {
                placeMarkElement.addElement("wpml:useGlobalTurnParam").addText("1");
            }else {
                //航点模式，转弯类型waypointTurnParam
                WaypointTurnParam waypointTurnParam = placemark.getWaypointTurnParam();
                Element waypointTurnParamElement = placeMarkElement.addElement("wpml:waypointTurnParam");
                waypointTurnParamElement.addElement("wpml:waypointTurnMode").addText(waypointTurnParam.getWaypointTurnMode());
                waypointTurnParamElement.addElement("wpml:waypointTurnDampingDist").addText(Double.toString(waypointTurnParam.getWaypointTurnDampingDist()));
            }

            //判断是否添加云台府仰角
            if("usePointSetting".equals(folder.getGimbalPitchMode())){
                //云台俯仰角度wpml:gimbalPitchAngle
                String gimbalPitchAngle = Integer.toString(placemark.getGimbalPitchAngle());
                placeMarkElement.addElement("wpml:gimbalPitchAngle").addText(gimbalPitchAngle);
            }

            //是否贴合直线wpml:useStraightLine
            placeMarkElement.addElement("wpml:useStraightLine").addText(Integer.toString(placemark.getUseStraightLine()));

            //添加动作组
            ActionGroup actionGroup=placemark.getActionGroup();
            //判断动作组是否为空
            if(actionGroup!=null)
            {
                Element actionGroupElement = placeMarkElement.addElement("wpml:actionGroup");
                //动作组id
                actionGroupElement.addElement("wpml:actionGroupId").addText(String.valueOf(i));
                //动作组开始生效的航点wpml:actionGroupStartIndex
                actionGroupElement.addElement("wpml:actionGroupStartIndex").addText(Integer.toString(actionGroup.getActionGroupStartIndex()));
                //动作组结束生效的航点wpml:actionGroupEndIndex
                actionGroupElement.addElement("wpml:actionGroupEndIndex").addText(Integer.toString(actionGroup.getActionGroupEndIndex()));
                //动作执行模式wpml:actionGroupMode
                actionGroupElement.addElement("wpml:actionGroupMode").addText(actionGroup.getActionGroupMode());

                //动作触发器参数
                ActionTrigger actionTrigger=actionGroup.getActionTrigger();
                //动作组触发器wpml:actionTrigger
                Element actionTriggerElement = actionGroupElement.addElement("wpml:actionTrigger");
                //动作触发器类型wpml:actionTriggerType
                actionTriggerElement.addElement("wpml:actionTriggerType").addText(actionTrigger.getActionTriggerType());
                //判断是否为间隔拍照
                if(actionTrigger.getActionTriggerType().equals("multipleTiming")||actionTrigger.getActionTriggerType().equals("multipleDistance"))
                {
                    actionTriggerElement.addElement("actionTriggerParam").addText(Float.toString(actionTrigger.getActionTriggerParam()));
                }
                //添加具体的动作执行参数
                List<Action> actionList = actionGroup.getActionList();
                for (int j =0; j < actionList.size(); j++) {
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
                            actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText(Integer.toString(takePhoto.getUseGlobalPayloadLensIndex()));

                        }else {
                            String hangdian = takePhoto.getFileSuffix() + String.valueOf(i + 1);
                            actionActuatorFuncParamElement.addElement("wpml:fileSuffix").addText(hangdian);//航点后缀
                            actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText(Integer.toString(takePhoto.getPayloadPositionIndex()));//挂载位置
                            //是否使用全局负载存储类型
                            if(takePhoto.getUseGlobalPayloadLensIndex()==1){
                                actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("1");
                            }else {
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
                        }else{
                            actionActuatorFuncParamElement.addElement("wpml:payloadLensIndex").addText(orientedShoot.getPayloadLensIndex());
                        }
                        actionActuatorFuncParamElement.addElement("wpml:targetAngle").addText("0");
                        UUID actionUUID = UUID.randomUUID();
                        actionActuatorFuncParamElement.addElement("wpml:actionUUID").addText(actionUUID.toString());
                        actionActuatorFuncParamElement.addElement("wpml:imageWidth").addText("0");
                        actionActuatorFuncParamElement.addElement("wpml:imageHeight").addText("0");
                        actionActuatorFuncParamElement.addElement("wpml:AFPos").addText("0");
                        actionActuatorFuncParamElement.addElement("wpml:gimbalPort").addText("0");
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
                        }
                        else {
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
                        }else{
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

                }
            }
            //是否有风险
            placeMarkElement.addElement("wpml:isRisky").addText(Integer.toString(placemark.getIsRisky()));
        }

        //添加负载参数
        PayloadParam payloadParam = folder.getPayloadParam();
        Element payloadParamElement = folderElement.addElement("wpml:payloadParam");
        payloadParamElement.addElement("wpml:payloadPositionIndex").addText(Integer.toString(payloadParam.getPayloadPositionIndex()));
        payloadParamElement.addElement("wpml:meteringMode").addText(payloadParam.getMeteringMode());
        payloadParamElement.addElement("wpml:dewarpingEnable").addText(Integer.toString(payloadParam.getDewarpingEnable()));
        payloadParamElement.addElement("wpml:returnMode").addText(payloadParam.getReturnMode());
        payloadParamElement.addElement("wpml:samplingRate").addText(Integer.toString(payloadParam.getSamplingRate()));
        payloadParamElement.addElement("wpml:scanningMode").addText(payloadParam.getScanningMode());
        payloadParamElement.addElement("wpml:modelColoringEnable").addText(Integer.toString(payloadParam.getModelColoringEnable()));

        //将kml写出本地
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("UTF-8");//设置编码格式
        XMLWriter xmlWriter = new XMLWriter(new FileOutputStream(createKmzUrl + "/wpmz/template.kml"),format);
        xmlWriter.write(document);
        xmlWriter.close();
        System.out.println("success");

        return "success";
    }

}

