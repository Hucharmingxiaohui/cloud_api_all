package com.dji.sample.df.kmzDf.utils;

import com.dji.sample.df.kmzDf.entity.wayline.Folder.PayloadParam.PayloadParam;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.Action;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Folder;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.GlobalWaypointHeadingParam.GlobalWaypointHeadingParam;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.ActionActuatorFuncParam;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.CustomDirName.CustomDirName;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.Focus.Focus;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.GimbalRotate.GimbalRotate;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.Hover.Hover;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.OrientedShoot.OrientedShoot;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.PanoShot.PanoShot;
import com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.RecordPointCloud.RecordPointCloud;
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
import com.dji.sample.df.kmzDf.entity.wayline.MissionConfig.MissionConfig;
import com.dji.sample.df.kmzDf.entity.wayline.MissionConfig.PayloadInfo.PayloadInfo;
import com.dji.sample.df.kmzDf.entity.wayline.Wayline;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
//大疆航点航线解析(M30T,mavic 3E)
public class GetWaylineInfo {
    public static Wayline parseXmlWithDom4j(InputStream input) throws Exception {
        SAXReader reader = new SAXReader();
        Document document = null;
        try {
            document = reader.read(input);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        Element rootElement = document.getRootElement();//获取kml文件的根结点
        List<Element> documentList = rootElement.elements();
        //航线类
        Wayline wayline=new Wayline();
        //任务信息
        MissionConfig missionConfig =new MissionConfig();
        //无人机信息
        DroneInfo droneInfo = new DroneInfo();
        //负载信息
        PayloadInfo payloadInfo = new PayloadInfo();
        //folder信息
        Folder folder = new Folder();
        //Placemarks
        List<Placemark> placeMarks = new ArrayList<Placemark>();
        for(Element document0 : documentList){
            List<Element> document0List = document0.elements();
            for(Element document1 : document0List){
                //missionConfig获取配置信息
                if("missionConfig".equals(document1.getName())){
                    List<Element> missionConfigList = document1.elements();
                    for(Element mission:missionConfigList){
                        if("flyToWaylineMode".equals(mission.getName()))//飞向首航点模式
                        {
                            missionConfig.setFlyToWaylineMode(mission.getText());
                        }
                        if("finishAction".equals(mission.getName())){//完成航线任务后执行的动作
                            missionConfig.setFinishAction(mission.getText());
                        }
                        if("exitOnRCLost".equals(mission.getName())){//失控是否继续执行航线
                            missionConfig.setExitOnRCLost(mission.getText());
                        }
                        if("executeRCLostAction".equals(mission.getName())){//失控动作

                        }
                        if("takeOffSecurityHeight".equals(mission.getName())){//安全起飞高度，起飞高度
                            missionConfig.setTakeOffSecurityHeight(Float.parseFloat(mission.getText()));
                        }
                        if("globalTransitionalSpeed".equals(mission.getName())){//起飞速度，飞向首航点速度
                            missionConfig.setGlobalTransitionalSpeed(Float.parseFloat(mission.getText()));
                        }
                        if("droneInfo".equals(mission.getName())){//无人机信息
                            List<Element> droneInfoList = mission.elements();
                            for(Element drone:droneInfoList){
                                if("droneEnumValue".equals(drone.getName())){//飞行器机型主类型
                                    droneInfo.setDroneEnumValue(Integer.parseInt(drone.getText()));
                                }
                                if("droneSubEnumValue".equals(drone.getName())){//飞行器机型子类型
                                    droneInfo.setDroneSubEnumValue(Integer.parseInt(drone.getText()));
                                }
                            }
                        }
                        if("payloadInfo".equals(mission.getName())){
                            List<Element> payloadInfoList = mission.elements();
                            for(Element payload:payloadInfoList){
                                if("payloadEnumValue".equals(payload.getName())){//负载机型主类型
                                    payloadInfo.setPayloadEnumValue(Integer.parseInt(payload.getText()));
                                }
                                if("payloadSubEnumValue".equals(payload.getName())){
                                    payloadInfo.setPayloadSubEnumValue(Integer.parseInt(payload.getText()));
                                }
                                if("payloadPositionIndex".equals(payload.getName())){//负载挂载位置
                                    payloadInfo.setPayloadPositionIndex(Integer.parseInt(payload.getText()));
                                }
                            }
                        }
                    }
                    missionConfig.setDroneInfo(droneInfo);
                    missionConfig.setPayloadInfo(payloadInfo);

                }
                if("Folder".equals(document1.getName())){
                    List<Element> folderList = document1.elements();
                    for(Element folderElement:folderList){
                        if("templateType".equals(folderElement.getName())){//航点类型
                            folder.setTemplateType(folderElement.getText());
                        }
                        if("templateId".equals(folderElement.getName())){//模板ID,在一个kmz文件内该ID唯一
                            folder.setTemplateId(Integer.parseInt(folderElement.getText()));
                        }
                        if("waylineCoordinateSysParam".equals(folderElement.getName())){//坐标系
                            WaylineCoordinateSysParam waylineCoordinateSysParam = new WaylineCoordinateSysParam();
                            List<Element> waylineCoordinateSysParamList = folderElement.elements();
                            for(Element waylineCoordinateSysParamElement:waylineCoordinateSysParamList){
                                if("coordinateMode".equals(waylineCoordinateSysParamElement.getName())){//坐标系类型
                                    waylineCoordinateSysParam.setCoordinateMode(waylineCoordinateSysParamElement.getText());
                                }
                                if("heightMode".equals(waylineCoordinateSysParamElement.getName())){//高度模式
                                    waylineCoordinateSysParam.setHeightMode(waylineCoordinateSysParamElement.getText());
                                }
                                if("positioningType".equals(waylineCoordinateSysParamElement.getName())){//坐标来源
                                    waylineCoordinateSysParam.setPositioningType(waylineCoordinateSysParamElement.getText());
                                }
                            }
                            folder.setWaylineCoordinateSysParam(waylineCoordinateSysParam);
                        }
                        if("autoFlightSpeed".equals(folderElement.getName())){//全局速度
                            folder.setAutoFlightSpeed(Float.parseFloat(folderElement.getText()));
                        }
                        if("globalHeight".equals(folderElement.getName())){//全局高度
                            folder.setGlobalHeight(Float.parseFloat(folderElement.getText()));
                        }
                        if("caliFlightEnable".equals(folderElement.getName())){//是否启用标定飞行
                            folder.setCaliFlightEnable(Integer.parseInt(folderElement.getText()));
                        }
                        if("gimbalPitchMode".equals(folderElement.getName())){//云台控制模式
                            folder.setGimbalPitchMode(folderElement.getText());
                        }
                        if("globalWaypointHeadingParam".equals(folderElement.getName())){//全局偏航参数
                            GlobalWaypointHeadingParam globalWaypointHeadingParam=new GlobalWaypointHeadingParam();
                            List<Element> globalWaypointHeadingParamList = folderElement.elements();
                            for(Element globalWaypointHeadingParamElement:globalWaypointHeadingParamList ){
                                if("waypointHeadingMode".equals(globalWaypointHeadingParamElement.getName())){//偏航模式
                                    globalWaypointHeadingParam.setWaypointHeadingMode(globalWaypointHeadingParamElement.getText());
                                }
                                if("waypointHeadingAngle".equals(globalWaypointHeadingParamElement.getName())){//偏航角度
                                    globalWaypointHeadingParam.setWaypointHeadingAngle(Float.parseFloat(globalWaypointHeadingParamElement.getText()));
                                }
                                if("waypointPoiPoint".equals(globalWaypointHeadingParamElement.getName())){//兴趣点
                                    globalWaypointHeadingParam.setWaypointPoiPoint(globalWaypointHeadingParamElement.getText());
                                }
                                if("waypointHeadingPoiIndex".equals(globalWaypointHeadingParamElement.getName())){//负载位置
                                    globalWaypointHeadingParam.setWaypointHeadingPoiIndex(Integer.parseInt(globalWaypointHeadingParamElement.getText()));
                                }
                            }
                            folder.setGlobalWaypointHeadingParam(globalWaypointHeadingParam);
                        }
                        if("globalWaypointTurnMode".equals(folderElement.getName())){//转弯模式，航点类型
                            folder.setGlobalWaypointTurnMode(folderElement.getText());
                        }
                        if("globalUseStraightLine".equals(folderElement.getName())){//是否贴合直线
                            folder.setGlobalUseStraightLine(Integer.parseInt(folderElement.getText()));
                        }
                        //Placemark
                        if("Placemark".equals(folderElement.getName())){
                            Placemark placemark = new Placemark();
                            System.out.println(placemark);
                            List<Element> placemarkList = folderElement.elements();
                            for (Element placemarkElement:placemarkList){
                                if("Point".equals(placemarkElement.getName())){//航点坐标信息
                                    Point point = new Point();
                                    List<Element> pointList = placemarkElement.elements();
                                    for(Element pointElement:pointList){
                                        if("coordinates".equals(pointElement.getName())){
                                            point.setCoordinates(pointElement.getText());
                                            placemark.setPoint(point);
                                        }
                                    }
                                }
                                if("index".equals(placemarkElement.getName())){//航点序号
                                    placemark.setIndex(Integer.parseInt(placemarkElement.getText()));
                                }
                                if("ellipsoidHeight".equals(placemarkElement.getName())){//航点高度（WGS84椭球高度）
                                    placemark.setEllipsoidHeight(Float.parseFloat(placemarkElement.getText()));
                                }
                                if("height".equals(placemarkElement.getName())){//航点高度（EGM96海拔高度/相对起飞点高度/AGL相对地面高度）
                                    placemark.setHeight(Float.parseFloat(placemarkElement.getText()));
                                }
                                //是否使用全局高度
                                if("useGlobalHeight".equals(placemarkElement.getName())){
                                    placemark.setUseGlobalHeight(Integer.parseInt(placemarkElement.getText()));
                                }
                                //是否使用全局速度
                                if("useGlobalSpeed".equals(placemarkElement.getName())){
                                    placemark.setUseGlobalSpeed(Integer.parseInt(placemarkElement.getText()));
                                }
                                //是否使用全局偏航参数
                                if("useGlobalHeadingParam".equals(placemarkElement.getName())){
                                    placemark.setUseGlobalHeadingParam(Integer.parseInt(placemarkElement.getText()));
                                }
                                //是否使用全局航点模式
                                if("useGlobalTurnParam".equals(placemarkElement.getName())){
                                    placemark.setUseGlobalTurnParam(Integer.parseInt(placemarkElement.getText()));
                                }
                                if("waypointSpeed".equals(placemarkElement.getName())){//航点速度
                                    placemark.setWaypointSpeed(Float.parseFloat(placemarkElement.getText()));
                                }
                                if("waypointHeadingParam".equals(placemarkElement.getName())){//航点偏航参数
                                    WaypointHeadingParam waypointHeadingParam=new WaypointHeadingParam();
                                    List<Element> waypointHeadingParamList = placemarkElement.elements();
                                    for(Element waypointHeadingParamElement:waypointHeadingParamList ){
                                        if("waypointHeadingMode".equals(waypointHeadingParamElement.getName())){//偏航模式
                                            waypointHeadingParam.setWaypointHeadingMode(waypointHeadingParamElement.getText());
                                        }
                                        if("waypointHeadingAngle".equals(waypointHeadingParamElement.getName())){//偏航角度
                                            waypointHeadingParam.setWaypointHeadingAngle(Float.parseFloat(waypointHeadingParamElement.getText()));
                                        }
                                        if("waypointPoiPoint".equals(waypointHeadingParamElement.getName())){//兴趣点
                                            waypointHeadingParam.setWaypointPoiPoint(waypointHeadingParamElement.getText());
                                        }
                                        if("waypointHeadingPathMode".equals(waypointHeadingParamElement.getName())){//飞行器偏航角转动方向
                                            waypointHeadingParam.setWaypointHeadingPathMode(waypointHeadingParamElement.getText());
                                        }
                                        if("waypointHeadingPoiIndex".equals(waypointHeadingParamElement.getName())){//负载位置
                                            waypointHeadingParam.setWaypointHeadingPoiIndex(Integer.parseInt(waypointHeadingParamElement.getText()));

                                        }
                                    }
                                    placemark.setWaypointHeadingParam(waypointHeadingParam);
                                }
                                if("waypointTurnParam".equals(placemarkElement.getName())){//航点类型、转弯模式
                                    WaypointTurnParam waypointTurnParam = new WaypointTurnParam();
                                    List<Element> waypointTurnParamList = placemarkElement.elements();
                                    for(Element waypointTurnParamElement:waypointTurnParamList){
                                        if("waypointTurnMode".equals(waypointTurnParamElement.getName())){//转弯模式
                                            waypointTurnParam.setWaypointTurnMode(waypointTurnParamElement.getText());
                                        }
                                        if("waypointTurnDampingDist".equals(waypointTurnParamElement.getName())){
                                            waypointTurnParam.setWaypointTurnDampingDist(Double.parseDouble(waypointTurnParamElement.getText()));
                                        }

                                    }
                                    placemark.setWaypointTurnParam(waypointTurnParam);
                                }
                                if("gimbalPitchAngle".equals(placemarkElement.getName())){//云台角度
                                    placemark.setGimbalPitchAngle(Integer.parseInt(placemarkElement.getText()));
                                }
                                if("useStraightLine".equals(placemarkElement.getName())){//是否贴合直线
                                    placemark.setUseStraightLine(Integer.parseInt(placemarkElement.getText()));
                                }
                                if("actionGroup".equals(placemarkElement.getName())){//动作组
                                    ActionGroup actionGroup = new ActionGroup();
                                    List<Element> actionGroupList = placemarkElement.elements();
                                    List<Action> actions = new ArrayList<Action>();

                                    for(Element actionGroupElemnt:actionGroupList){
                                        if("actionGroupId".equals(actionGroupElemnt.getName())){//动作组id
                                            actionGroup.setActionGroupId(Integer.parseInt(actionGroupElemnt.getText()));
                                        }
                                        if("actionGroupStartIndex".equals(actionGroupElemnt.getName())){//动作开始航点
                                            actionGroup.setActionGroupStartIndex(Integer.parseInt(actionGroupElemnt.getText()));
                                        }
                                        if("actionGroupEndIndex".equals(actionGroupElemnt.getName())){//动作结束航点
                                            actionGroup.setActionGroupEndIndex(Integer.parseInt(actionGroupElemnt.getText()));
                                        }
                                        if("actionGroupMode".equals(actionGroupElemnt.getName())){//执行序列
                                            actionGroup.setActionGroupMode(actionGroupElemnt.getText());
                                        }
                                        if("actionTrigger".equals(actionGroupElemnt.getName())){
                                            ActionTrigger actionTrigger= new ActionTrigger();
                                            List<Element> actionTriggerList = actionGroupElemnt.elements();
                                            for (Element actionTriggerElement:actionTriggerList){
                                                if("actionTriggerType".equals(actionTriggerElement.getName())){//触发动作类型性
                                                    actionTrigger.setActionTriggerType(actionTriggerElement.getText());
                                                }
                                                if("actionTriggerParam".equals(actionGroupElemnt.getName())){//触发动作参数
                                                    actionTrigger.setActionTriggerParam(Float.parseFloat(actionTriggerElement.getText()));
                                                }
                                            }
                                            actionGroup.setActionTrigger(actionTrigger);
                                        }
                                        if("action".equals(actionGroupElemnt.getName())){
                                            Action action = new Action();
                                            List<Element> actionList = actionGroupElemnt.elements();
                                            ActionActuatorFuncParam actionActuatorFuncParam=new ActionActuatorFuncParam();
                                            for(Element actionElement:actionList){
                                                if("actionActuatorFunc".equals(actionElement.getName())){
                                                    action.setActionActuatorFunc(actionElement.getText());//存储动作类别
                                                }
                                                //拍照
                                                if("actionActuatorFuncParam".equals(actionElement.getName()) && "takePhoto".equals(action.getActionActuatorFunc())){//等待下一次循环根据动作类别判断是哪一类
                                                    TakePhoto takePhoto = new TakePhoto();
                                                    List<Element> takePhotoList = actionElement.elements();
                                                    for(Element takePhotoElement:takePhotoList){
                                                        if("fileSuffix".equals(takePhotoElement.getName())){//照片名称
                                                            takePhoto.setFileSuffix(takePhotoElement.getText());
                                                        }
                                                        if("payloadPositionIndex".equals(takePhotoElement.getName())){//负载位置
                                                            takePhoto.setPayloadPositionIndex(Integer.parseInt(takePhotoElement.getText()));
                                                        }
                                                        if("useGlobalPayloadLensIndex".equals(takePhotoElement.getName())){//使用存储类型
                                                            takePhoto.setUseGlobalPayloadLensIndex(Integer.parseInt(takePhotoElement.getText()));
                                                        }
                                                        if("payloadLensIndex".equals(takePhotoElement.getName())){//存储类型
                                                            takePhoto.setPayloadLensIndex(takePhotoElement.getText());
                                                        }
                                                    }
                                                    actionActuatorFuncParam.setTakePhoto(takePhoto);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                    //定向拍照
                                                }else if("actionActuatorFuncParam".equals(actionElement.getName())&& "orientedShoot".equals(action.getActionActuatorFunc())){
                                                    OrientedShoot orientedShoot = new OrientedShoot();
                                                    List<Element> orientedShootList = actionElement.elements();
                                                    for(Element orientedShootElement:orientedShootList){
                                                        if("gimbalPitchRotateAngle".equals(orientedShootElement.getName())){//云台府仰角
                                                            orientedShoot.setGimbalPitchRotateAngle(Float.parseFloat(orientedShootElement.getText()));
                                                        }
                                                        if("gimbalRollRotateAngle".equals(orientedShootElement.getName())){//云台横滚角度
                                                            orientedShoot.setGimbalRollRotateAngle(Float.parseFloat(orientedShootElement.getText()));
                                                        }
                                                        if("gimbalYawRotateAngle".equals(orientedShootElement.getName())){//云台偏航角
                                                            orientedShoot.setGimbalYawRotateAngle(Float.parseFloat(orientedShootElement.getText()));
                                                        }
                                                        if("focusX".equals(orientedShootElement.getName())){
                                                            orientedShoot.setFocusX(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("focusY".equals(orientedShootElement.getName())){
                                                            orientedShoot.setFocusY(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("focusRegionWidth".equals(orientedShootElement.getName())){
                                                            orientedShoot.setFocusRegionWidth(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("focusRegionHeight".equals(orientedShootElement.getName())){
                                                            orientedShoot.setFocusRegionHeight (Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("focalLength".equals(orientedShootElement.getName())){//焦距
                                                            orientedShoot.setFocalLength(Float.parseFloat(orientedShootElement.getText()));
                                                        }
                                                        if("aircraftHeading".equals(orientedShootElement.getName())){
                                                            orientedShoot.setAircraftHeading(Float.parseFloat(orientedShootElement.getText()));
                                                        }
                                                        if("accurateFrameValid".equals(orientedShootElement.getName())){
                                                            orientedShoot.setAccurateFrameValid(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("payloadPositionIndex".equals(orientedShootElement.getName())){
                                                            orientedShoot.setPayloadPositionIndex(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("payloadLensIndex".equals(orientedShootElement.getName())){
                                                            orientedShoot.setPayloadLensIndex(orientedShootElement.getText());
                                                        }
                                                        if("useGlobalPayloadLensIndex".equals(orientedShootElement.getName())){
                                                            orientedShoot.setUseGlobalPayloadLensIndex(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if(" targetAngle".equals(orientedShootElement.getName())){
                                                            orientedShoot.setTargetAngle(Float.parseFloat(orientedShootElement.getText()));
                                                        }
                                                        if("actionUUID".equals(orientedShootElement.getName())){
                                                            orientedShoot.setActionUUID(orientedShootElement.getText());
                                                        }
                                                        if("imageWidth".equals(orientedShootElement.getName())){
                                                            orientedShoot.setImageWidth(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("imageHeight".equals(orientedShootElement.getName())){
                                                            orientedShoot.setImageHeight(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("AFPos".equals(orientedShootElement.getName())){
                                                            orientedShoot.setAFPos(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("gimbalPort".equals(orientedShootElement.getName())){
                                                            orientedShoot.setGimbalPort(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("orientedCameraType".equals(orientedShootElement.getName())){
                                                            orientedShoot.setOrientedCameraType(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("orientedFilePath=".equals(orientedShootElement.getName())){
                                                            orientedShoot.setOrientedFilePath(orientedShootElement.getText());
                                                        }
                                                        if("orientedFileMD5".equals(orientedShootElement.getName())){
                                                            orientedShoot.setOrientedFileMD5(orientedShootElement.getText());
                                                        }
                                                        if("orientedFileSuffix".equals(orientedShootElement.getName())){
                                                            orientedShoot.setOrientedFileSuffix(orientedShootElement.getText());
                                                        }
                                                        if("orientedCameraApertue".equals(orientedShootElement.getName())){
                                                            orientedShoot.setOrientedCameraApertue(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("orientedCameraLuminance".equals(orientedShootElement.getName())){
                                                            orientedShoot.setOrientedCameraLuminance(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("orientedCameraShutterTime".equals(orientedShootElement.getName())){
                                                            orientedShoot.setOrientedCameraShutterTime(Float.parseFloat(orientedShootElement.getText()));
                                                        }
                                                        if("orientedCameraISO".equals(orientedShootElement.getName())){
                                                            orientedShoot.setOrientedCameraISO(Integer.parseInt(orientedShootElement.getText()));
                                                        }
                                                        if("orientedPhotoMode".equals(orientedShootElement.getName())){
                                                            orientedShoot.setOrientedPhotoMode(orientedShootElement.getText());
                                                        }
                                                    }
                                                    actionActuatorFuncParam.setOrientedShoot(orientedShoot);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                    //全景拍照
                                                }else if ("actionActuatorFuncParam".equals(actionElement.getName()) &&"panoShot".equals(action.getActionActuatorFunc())){
                                                    PanoShot panoShot =new PanoShot();
                                                    List<Element> panoShotList = actionElement.elements();
                                                    for(Element panoShotElement:panoShotList){
                                                        if("payloadPositionIndex".equals(panoShotElement.getName())){//负载位置
                                                            panoShot.setPayloadPositionIndex(Integer.parseInt(panoShotElement.getText()));
                                                        }
                                                        if("payloadLensIndex".equals(panoShotElement.getName())){//负载存储类型
                                                            panoShot.setPayloadLensIndex(panoShotElement.getText());
                                                        }
                                                        if("useGlobalPayloadLensIndex".equals(panoShotElement.getName())){//是否使用全局存储类型
                                                            panoShot.setUseGlobalPayloadLensIndex(Integer.parseInt(panoShotElement.getText()));
                                                        }
                                                        if("panoShotSubMode".equals(panoShotElement.getName())){//云台府仰角
                                                            panoShot.setPanoShotSubMode(panoShotElement.getText());
                                                        }
                                                    }
                                                    actionActuatorFuncParam.setPanoShot(panoShot);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                    //开始录像
                                                } else if ("actionActuatorFuncParam".equals(actionElement.getName()) &&"startRecord".equals(action.getActionActuatorFunc())) {
                                                    StartRecord startRecord =new StartRecord();
                                                    List<Element> startRecordList = actionElement.elements();
                                                    for (Element startRecordElement:startRecordList){
                                                        if("payloadPositionIndex".equals(startRecordElement.getName())){//负载位置
                                                            startRecord.setPayloadPositionIndex(Integer.parseInt(startRecordElement.getText()));
                                                        }
                                                        if("useGlobalPayloadLensIndex".equals(startRecordElement.getName())){//是否使用全局拍照
                                                            startRecord.setUseGlobalPayloadLensIndex(Integer.parseInt(startRecordElement.getText()));
                                                        }
                                                        if("fileSuffix".equals(startRecordElement.getName())){
                                                            startRecord.setFileSuffix(startRecordElement.getText());
                                                        }
                                                        if("payloadLensIndex".equals(startRecordElement.getName())){
                                                            startRecord.setPayloadLensIndex(startRecordElement.getText());
                                                        }
                                                    }
                                                    actionActuatorFuncParam.setStartRecord(startRecord);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                    //停止录像
                                                } else if ("actionActuatorFuncParam".equals(actionElement.getName()) &&"stopRecord".equals(action.getActionActuatorFunc())) {
                                                    StopRecord stopRecord =new StopRecord();
                                                    List<Element> stopRecordList = actionElement.elements();
                                                    for(Element stopRecordElement:stopRecordList){
                                                        if("payloadPositionIndex".equals(stopRecordElement.getName())){//负载位置
                                                            stopRecord.setPayloadPositionIndex(Integer.parseInt(stopRecordElement.getText()));
                                                        }
                                                        if("payloadLensIndex".equals(stopRecordElement.getName())){//存储类型
                                                            stopRecord.setPayloadLensIndex(stopRecordElement.getText());
                                                        }
                                                    }
                                                    actionActuatorFuncParam.setStopRecord(stopRecord);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                    //云台俯仰偏航角
                                                } else if ("actionActuatorFuncParam".equals(actionElement.getName()) &&"gimbalRotate".equals(action.getActionActuatorFunc())) {
                                                    GimbalRotate gimbalRotate = new GimbalRotate();
                                                    List<Element> gimbalRotateList=actionElement.elements();
                                                    for(Element gimbalRotateElement:gimbalRotateList){
                                                        if("payloadPositionIndex".equals(gimbalRotateElement.getName())){//位置
                                                            gimbalRotate.setPayloadPositionIndex(Integer.parseInt(gimbalRotateElement.getText()));
                                                        }
                                                        if("gimbalHeadingYawBase".equals(gimbalRotateElement.getName())){//坐标系
                                                            gimbalRotate.setGimbalHeadingYawBase(gimbalRotateElement.getText());
                                                        }
                                                        if("gimbalRotateMode".equals(gimbalRotateElement.getName())){//转动模式
                                                            gimbalRotate.setGimbalRotateMode(gimbalRotateElement.getText());
                                                        }
                                                        if("gimbalPitchRotateEnable".equals(gimbalRotateElement.getName())){//是否能转动俯仰角
                                                            gimbalRotate.setPayloadPositionIndex(Integer.parseInt(gimbalRotateElement.getText()));
                                                        }
                                                        if("gimbalPitchRotateAngle".equals(gimbalRotateElement.getName())){//府仰角
                                                            gimbalRotate.setGimbalPitchRotateAngle(Float.parseFloat(gimbalRotateElement.getText()));
                                                        }
                                                        if("gimbalRollRotateEnable".equals(gimbalRotateElement.getName())){//是否能转动横滚角度
                                                            gimbalRotate.setGimbalRollRotateEnable(Integer.parseInt(gimbalRotateElement.getText()));
                                                        }
                                                        if("gimbalRollRotateAngle".equals(gimbalRotateElement.getName())){//横滚角
                                                            gimbalRotate.setGimbalRollRotateAngle(Float.parseFloat(gimbalRotateElement.getText()));
                                                        }
                                                        if("gimbalYawRotateEnable".equals(gimbalRotateElement.getName())){//是否能转动云台偏航角度
                                                            gimbalRotate.setGimbalYawRotateEnable(Integer.parseInt(gimbalRotateElement.getText()));
                                                        }
                                                        if("gimbalYawRotateAngle".equals(gimbalRotateElement.getName())){//偏航
                                                            gimbalRotate.setGimbalYawRotateAngle(Float.parseFloat(gimbalRotateElement.getText()));
                                                        }
                                                        if("gimbalRotateTimeEnable".equals(gimbalRotateElement.getName())){//
                                                            gimbalRotate.setGimbalRotateTimeEnable(Integer.parseInt(gimbalRotateElement.getText()));
                                                        }
                                                        if("gimbalRotateTime".equals(gimbalRotateElement.getName())){//
                                                            gimbalRotate.setGimbalRotateTime(Float.parseFloat(gimbalRotateElement.getText()));
                                                        }

                                                    }
                                                    actionActuatorFuncParam.setGimbalRotate(gimbalRotate);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                    //创建文件夹
                                                } else if ("actionActuatorFuncParam".equals(actionElement.getName()) &&"customDirName".equals(action.getActionActuatorFunc())) {
                                                    CustomDirName customDirName=new CustomDirName();
                                                    List<Element> customDirNameList=actionElement.elements();
                                                    for(Element customDirNameElement:customDirNameList){
                                                        if("payloadPositionIndex".equals(customDirNameElement.getName())){//负载位置
                                                            customDirName.setPayloadPositionIndex(Integer.parseInt(customDirNameElement.getText()));
                                                        }
                                                        if("directoryName".equals(customDirNameElement.getName())){//文件夹名称
                                                            customDirName.setDirectoryName(customDirNameElement.getText());
                                                        }
                                                    }
                                                    actionActuatorFuncParam.setCustomDirName(customDirName);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                    //对焦
                                                } else if ("actionActuatorFuncParam".equals(actionElement.getName()) &&"focus".equals(action.getActionActuatorFunc())) {
                                                    Focus focus = new Focus();
                                                    List<Element> focusList = actionElement.elements();
                                                    for(Element focusElement:focusList){
                                                        if("payloadPositionIndex".equals(focusElement.getName())){
                                                            focus.setPayloadPositionIndex(Integer.parseInt(focusElement.getText()));
                                                        }
                                                        if("isPointFocus".equals(focusElement.getName())){
                                                            focus.setIsPointFocus(Integer.parseInt(focusElement.getText()));
                                                        }
                                                        if("focusX".equals(focusElement.getName())){
                                                            focus.setFocusX(Integer.parseInt(focusElement.getText()));
                                                        }
                                                        if("focusY".equals(focusElement.getName())){
                                                            focus.setFocusY(Integer.parseInt(focusElement.getText()));
                                                        }
                                                        if("focusRegionWidth".equals(focusElement.getName())){
                                                            focus.setFocusRegionWidth(Float.parseFloat(focusElement.getText()));
                                                        }
                                                        if("focusRegionHeight".equals(focusElement.getName())){
                                                            focus.setFocusRegionHeight(Float.parseFloat(focusElement.getText()));
                                                        }
                                                        if("isInfiniteFocus".equals(focusElement.getName())){
                                                            focus.setIsInfiniteFocus(Integer.parseInt(focusElement.getText()));
                                                        }
                                                    }
                                                    actionActuatorFuncParam.setFocus(focus);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                    //悬停
                                                } else if ("actionActuatorFuncParam".equals(actionElement.getName()) &&"hover".equals(action.getActionActuatorFunc())) {
                                                    Hover hover=new Hover();
                                                    List<Element> hoverList=actionElement.elements();
                                                    for(Element hoverElement:hoverList){
                                                        if("hoverTime".equals(hoverElement.getName())){
                                                            hover.setHoverTime(Float.parseFloat(hoverElement.getText()));
                                                        }
                                                    }
                                                    actionActuatorFuncParam.setHover(hover);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                    //记录点云
                                                }else if("actionActuatorFuncParam".equals(actionElement.getName()) &&"recordPointCloud".equals(action.getActionActuatorFunc())){
                                                    RecordPointCloud recordPointCloud=new RecordPointCloud();
                                                    List<Element> recordPointCloudList = actionElement.elements();
                                                    for(Element recordPointCloudElement:recordPointCloudList){
                                                        if("payloadPositionIndex".equals(recordPointCloudElement.getName())){
                                                            recordPointCloud.setPayloadPositionIndex(Integer.parseInt(recordPointCloudElement.getText()));
                                                        }
                                                        if("recordPointCloudOperate".equals(recordPointCloudElement.getName())){
                                                            recordPointCloud.setRecordPointCloudOperate(recordPointCloudElement.getText());
                                                        }
                                                    }
                                                    actionActuatorFuncParam.setRecordPointCloud(recordPointCloud);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                    //无人机偏航
                                                } else if ("actionActuatorFuncParam".equals(actionElement.getName()) &&"rotateYaw".equals(action.getActionActuatorFunc())) {
                                                    RotateYaw rotateYaw=new RotateYaw();
                                                    List<Element> rotateYawList=actionElement.elements();
                                                    for(Element rotateYawElement:rotateYawList){
                                                        if("aircraftHeading".equals(rotateYawElement.getName())){
                                                            rotateYaw.setAircraftHeading(Float.parseFloat(rotateYawElement.getText()));
                                                        }
                                                        if("aircraftPathMod".equals(rotateYawElement.getName())){
                                                            rotateYaw.setAircraftPathMod(rotateYawElement.getText());
                                                        }
                                                    }
                                                    actionActuatorFuncParam.setRotateYaw(rotateYaw);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                    //变焦
                                                }else if("actionActuatorFuncParam".equals(actionElement.getName()) &&"zoom".equals(action.getActionActuatorFunc())){
                                                    Zoom zoom=new Zoom();
                                                    List<Element> zoomList=actionElement.elements();
                                                    for(Element zoomElement:zoomList){
                                                        if("payloadPositionIndex".equals(zoomElement.getName())){//负载位置
                                                            zoom.setPayloadPositionIndex(Integer.parseInt(zoomElement.getText()));
                                                        }
                                                        if("focalLength".equals(zoomElement.getName())){//焦距
                                                            zoom.setFocalLength(Float.parseFloat(zoomElement.getText()));
                                                        }
                                                    }
                                                    actionActuatorFuncParam.setZoom(zoom);
                                                    action.setActionActuatorFuncParam(actionActuatorFuncParam);
                                                }
                                            }
                                            actions.add(action);
                                        }
                                    }
                                    actionGroup.setActionList(actions);
                                    placemark.setActionGroup(actionGroup);
                                }
                            }
                            placeMarks.add(placemark);
                        }
                        if("payloadParam".equals(folderElement.getName())){
                            //负载参数
                            PayloadParam payloadParam = new PayloadParam();
                            List<Element>  payloadParamList = folderElement.elements();
                            for (Element payloadParamElement:payloadParamList){
                                if("payloadPositionIndex".equals(payloadParamElement.getName())){
                                    payloadParam.setPayloadPositionIndex(Integer.parseInt(payloadParamElement.getText()));
                                }
                                if("meteringMode".equals(payloadParamElement.getName())){
                                    payloadParam.setMeteringMode(payloadParamElement.getText());
                                }
                                if("dewarpingEnable".equals(payloadParamElement.getName())){
                                    payloadParam.setDewarpingEnable(Integer.parseInt(payloadParamElement.getText()));
                                }
                                if("samplingRate".equals(payloadParamElement.getName())){
                                    payloadParam.setSamplingRate(Integer.parseInt(payloadParamElement.getText()));
                                }
                                if("scanningMode".equals(payloadParamElement.getName())){
                                    payloadParam.setScanningMode(payloadParamElement.getText());
                                }
                                if("modelColoringEnable".equals(payloadParamElement.getName())){
                                    payloadParam.setModelColoringEnable(Integer.parseInt(payloadParamElement.getText()));
                                }
                                if("imageFormat".equals(payloadParamElement.getName())){
                                    payloadParam.setImageFormat(payloadParamElement.getText());
                                }
                            }
                           folder.setPayloadParam(payloadParam);
                        }
                    }
                }
            }
        }
        folder.setPlaceMarks(placeMarks);
        wayline.setMissionConfig(missionConfig);
        wayline.setFolder(folder);
        System.out.println("successCreateWayline");
        return wayline;

    }
}
