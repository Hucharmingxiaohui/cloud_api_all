package com.dji.sample.df.waylineDf.kmz.utils;

import com.dji.sample.df.waylineDf.kmz.entity.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
                //System.out.println(document1.getName());
                //missionConfig
                if("missionConfig".equals(document1.getName())){
                    List<Element> missionConfigList = document1.elements();
                    for(Element mission:missionConfigList){
                        if("finishAction".equals(mission.getName())){
                            missionConfig.setFinishAction(mission.getText());
                        }
                        if("takeOffSecurityHeight".equals(mission.getName())){
                            missionConfig.setTakeOffSecurityHeight(Float.parseFloat(mission.getText()));
                        }
                        if("globalTransitionalSpeed".equals(mission.getName())){
                            missionConfig.setGlobalTransitionalSpeed(Float.parseFloat(mission.getText()));
                        }
                        if("droneInfo".equals(mission.getName())){
                            List<Element> droneInfoList = mission.elements();
                            for(Element drone:droneInfoList){
                                if("droneEnumValue".equals(drone.getName())){
                                    droneInfo.setDroneEnumValue(Integer.parseInt(drone.getText()));
                                }
                                if("droneSubEnumValue".equals(drone.getName())){
                                    droneInfo.setDroneSubEnumValue(Integer.parseInt(drone.getText()));
                                }
                            }
                        }
                        if("payloadInfo".equals(mission.getName())){
                            List<Element> payloadInfoList = mission.elements();
                            for(Element payload:payloadInfoList){
                                if("payloadEnumValue".equals(payload.getName())){
                                    payloadInfo.setPayloadEnumValue(Integer.parseInt(payload.getText()));
                                }
                                if("payloadSubEnumValue".equals(payload.getName())){
                                    payloadInfo.setPayloadSubEnumValue(Integer.parseInt(payload.getText()));
                                }
                                if("payloadPositionIndex".equals(payload.getName())){
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
                        if("autoFlightSpeed".equals(folderElement.getName())){
                            folder.setAutoFlightSpeed(Float.parseFloat(folderElement.getText()));
                        }
                        if("globalHeight".equals(folderElement.getName())){
                            folder.setGlobalHeight(Float.parseFloat(folderElement.getText()));
                        }
                        if("globalWaypointTurnMode".equals(folderElement.getName())){
                            folder.setGlobalWaypointTurnMode(folderElement.getText());
                        }
                        //Placemark
                        if("Placemark".equals(folderElement.getName())){
                            Placemark placemark = new Placemark();
                            List<Element> placemarkList = folderElement.elements();
                            for (Element placemarkElement:placemarkList){
                                if("Point".equals(placemarkElement.getName())){
                                    List<Element> pointList = placemarkElement.elements();
                                    for(Element pointElement:pointList){
                                        if("coordinates".equals(pointElement.getName())){
                                            placemark.setCoordinates(pointElement.getText());
                                        }
                                    }
                                }
                                if("height".equals(placemarkElement.getName())){
                                    placemark.setHeight(Float.parseFloat(placemarkElement.getText()));
                                }
                                if("ellipsoidHeight".equals(placemarkElement.getName())){
                                    placemark.setEllipsoidHeight(Float.parseFloat(placemarkElement.getText()));
                                }
                                if("waypointSpeed".equals(placemarkElement.getName())){
                                    placemark.setWaypointSpeed(Float.parseFloat(placemarkElement.getText()));
                                }
                                if("waypointHeadingParam".equals(placemarkElement.getName())){
                                    List<Element> waypointHeadList = placemarkElement.elements();
                                    for(Element headingElement:waypointHeadList){
                                        if("waypointHeadingAngle".equals(headingElement.getName())){
                                            placemark.setWaypointHeadingAngle(Integer.parseInt(headingElement.getText()));
                                        }
                                    }
                                }
                                if("gimbalPitchAngle".equals(placemarkElement.getName())){
                                    placemark.setGimbalPitchAngle(Integer.parseInt(placemarkElement.getText()));
                                }
                                if("actionGroup".equals(placemarkElement.getName())){
                                    //动作组
                                    List<Action> actionGroup = new ArrayList<Action>();
                                    List<Element> actionGroupList = placemarkElement.elements();
                                    for(Element actionsElemnt:actionGroupList){
                                        if("action".equals(actionsElemnt.getName())){
                                            Action action = new Action();
                                            List<Element> actionList = actionsElemnt.elements();
                                            for(Element actionElement:actionList){
                                                if("actionActuatorFunc".equals(actionElement.getName())){
                                                    action.setActionActuatorFunc(actionElement.getText());
                                                    actionGroup.add(action);
                                                }
                                            }
                                        }
                                    }
                                    placemark.setActionGroup(actionGroup);
                                }
                            }
                            placeMarks.add(placemark);
                        }
                    }
                }
            }
        }
        folder.setPlaceMarks(placeMarks);
        wayline.setMissionConfig(missionConfig);
        wayline.setFolder(folder);
        System.out.println(wayline);
        return wayline;

    }
}
