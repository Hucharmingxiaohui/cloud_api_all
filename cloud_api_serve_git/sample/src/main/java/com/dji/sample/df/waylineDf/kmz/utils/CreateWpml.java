package com.dji.sample.df.waylineDf.kmz.utils;

import com.dji.sample.df.waylineDf.kmz.entity.*;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

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
        {
            //添加wpml:flyToWaylineMode节点，必须元素
            missionConfigElement.addElement("wpml:flyToWaylineMode").addText("safely");
            //添加wpml:finishAction节点，必须元素
            missionConfigElement.addElement("wpml:finishAction").addText(missionConfig.getFinishAction());
            //添加wpml:exitOnRCLost节点，必须元素
            missionConfigElement.addElement("wpml:exitOnRCLost").addText("executeLostAction");
            //添加wpml:exitOnRCLost节点，必须元素
            missionConfigElement.addElement("wpml:executeRCLostAction").addText("goBack");
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

        }


        Element folderElement = documentElement.addElement("Folder");//Folder节点
        Folder folder = wayline.getFolder();
        {
            //给Folder添加子节点wpml:templateId
            folderElement.addElement("wpml:templateId").addText("0");
            //给Folder添加wpml:executeHeightMode
            folderElement.addElement("wpml:executeHeightMode").addText("relativeToStartPoint");
            //给Folder添加航线id
            folderElement.addElement("wpml:waylineId").addText("0");
            //全局飞行速度
            String autoFlightSpeed = Float.toString(folder.getAutoFlightSpeed());
            folderElement.addElement("wpml:autoFlightSpeed").addText(autoFlightSpeed);

            //循环添加每一个Placemark节点，有几个坐标点就有几个Placemark节点，这里仅作示例
            List<Placemark> placeMarks =folder.getPlaceMarks();
            for (int i = 0; i <placeMarks.size(); i++) {
                Element placeMarkElement = folderElement.addElement("Placemark");
                //航点坐标
                Element Point = placeMarkElement.addElement("Point");
                Placemark placemark = placeMarks.get(i);
                {
                    Point.addElement("coordinates").addText(placemark.getCoordinates());
                }
                //航点序号
                placeMarkElement.addElement("wpml:index").addText(String.valueOf(i));
                //航点执行高度
                String height= Float.toString(placemark.getHeight());
                placeMarkElement.addElement("wpml:executeHeight").addText(height);
                //航点速度
                String waypointSpeed = Float.toString(placemark.getWaypointSpeed());
                placeMarkElement.addElement("wpml:waypointSpeed").addText(waypointSpeed);

                //偏航角参数
                Element waypointHeadingParam =  placeMarkElement.addElement("wpml:waypointHeadingParam");
                {
                    //偏航角模式
                    waypointHeadingParam.addElement("wpml:waypointHeadingMode").addText("smoothTransition");
                    //偏航角度
                    String waypointHeadingAngle = Integer.toString(placemark.getWaypointHeadingAngle());
                    waypointHeadingParam.addElement("wpml:waypointHeadingAngle").addText(waypointHeadingAngle);
                    //兴趣点
                    waypointHeadingParam.addElement("wpml:waypointPoiPoint").addText("0.000000,0.000000,0.000000");
                    //waypointHeadingAngleEnable
                    waypointHeadingParam.addElement("wpml:waypointHeadingAngleEnable").addText("1");
                    waypointHeadingParam.addElement("wpml:waypointHeadingPathMode").addText("followBadArc");
                    //waypointHeadingPoiIndex
                    waypointHeadingParam.addElement("wpml:waypointHeadingPoiIndex").addText("0");
                }

                //添加wpml:waypointTurnParam 航点类型航点转弯类型
                Element waypointTurnParam=placeMarkElement.addElement("wpml:waypointTurnParam");
                {
                    waypointTurnParam.addElement("wpml:waypointTurnParam").addText("toPointAndStopWithDiscontinuityCurvature");
                    waypointTurnParam.addElement("wpml:waypointTurnDampingDist").addText("0");
                }
                //useStraightLine
                placeMarkElement.addElement("useStraightLine").addText("1");

                //添加动作组
                Element actionGroupElement = placeMarkElement.addElement("wpml:actionGroup");
                {
                    actionGroupElement.addElement("wpml:actionGroupId").addText(String.valueOf(i));
                    actionGroupElement.addElement("wpml:actionGroupStartIndex").addText(String.valueOf(i));
                    actionGroupElement.addElement("wpml:actionGroupEndIndex").addText(String.valueOf(i));
                    actionGroupElement.addElement("wpml:actionGroupMode").addText("sequence");
                    //触发动作
                    Element actionTriggerElement = actionGroupElement.addElement("wpml:actionTrigger");
                    {
                        actionTriggerElement.addElement("wpml:actionTriggerType").addText("reachPoint");
                    }                    //调整云台府仰角
                    Element actionElement0 = actionGroupElement.addElement("wpml:action");
                    {
                        actionElement0.addElement("wpml:actionId").addText("0");
                        actionElement0.addElement("wpml:actionActuatorFunc").addText("gimbalRotate");
                        Element actionActuatorFuncParamElement0 = actionElement0.addElement("wpml:actionActuatorFuncParam");
                        {
                            actionActuatorFuncParamElement0.addElement("wpml:gimbalHeadingYawBase").addText("aircraft");
                            actionActuatorFuncParamElement0.addElement("wpml:gimbalRotateMode").addText("absoluteAngle");
                            actionActuatorFuncParamElement0.addElement("wpml:gimbalPitchRotateEnable").addText("1");
                            actionActuatorFuncParamElement0.addElement("wpml:gimbalPitchRotateAngle").addText(Integer.toString(placemark.getGimbalPitchAngle()));
                            actionActuatorFuncParamElement0.addElement("wpml:gimbalRollRotateEnable").addText("0");
                            actionActuatorFuncParamElement0.addElement("wpml:gimbalRollRotateAngle").addText("0");
                            actionActuatorFuncParamElement0.addElement("wpml:gimbalYawRotateEnable").addText("0");
                            actionActuatorFuncParamElement0.addElement("wpml:gimbalYawRotateAngle").addText("0");
                            actionActuatorFuncParamElement0.addElement("wpml:gimbalRotateTimeEnable").addText("0");
                            actionActuatorFuncParamElement0.addElement("wpml:gimbalRotateTime").addText("10");
                            actionActuatorFuncParamElement0.addElement("wpml:payloadPositionIndex").addText("0");
                        }                    }
                    //调整焦距
                    Element actionElement1 = actionGroupElement.addElement("wpml:action");
                    {
                        actionElement1.addElement("wpml:actionId").addText("1");
                        actionElement1.addElement("wpml:actionActuatorFunc").addText("focus");
                        Element actionActuatorFuncParamElement1 = actionElement1.addElement("wpml:actionActuatorFuncParam");
                        {
                            actionActuatorFuncParamElement1.addElement("wpml:focusX").addText("0.25");
                            actionActuatorFuncParamElement1.addElement("wpml:focusY").addText("0.25");
                            actionActuatorFuncParamElement1.addElement("wpml:focusRegionWidth").addText("0.5");
                            actionActuatorFuncParamElement1.addElement("wpml:focusRegionHeight").addText("0.5");
                            actionActuatorFuncParamElement1.addElement("wpml:isPointFocus").addText("0");
                            actionActuatorFuncParamElement1.addElement("wpml:isInfiniteFocus").addText("0");
                            actionActuatorFuncParamElement1.addElement("wpml:payloadPositionIndex").addText("0");
                        }
                    }
                    //获取具体的动作组
                    List<Action> actionGroup = placemark.getActionGroup();
                    for(int k = 0;k<actionGroup.size();k++ )
                    {
                        Element actionElement2 = actionTriggerElement.addElement("wpml:action");
                        actionElement2.addElement("wpml:actionId").addText(Integer.toString(2+k));
                        actionElement2.addElement("wpml:actionActuatorFunc").addText("takePhoto");
                        Element actionActionActuatorFuncParam2 = actionElement2.addElement("wpml:actionActuatorFuncParam");
                        {
                            actionActionActuatorFuncParam2.addElement("wpml:fileSuffix").addText("航点"+String.valueOf(i+1));
                            actionActionActuatorFuncParam2.addElement("wpml:payloadPositionIndex").addText("0");
                            actionActionActuatorFuncParam2.addElement("wpml:useGlobalPayloadLensIndex").addText("0");
                        }
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