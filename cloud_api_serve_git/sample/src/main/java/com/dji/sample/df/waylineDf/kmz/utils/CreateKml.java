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
        //文件创作者，非必须元素
        documentElement.addElement("wpml:author").addText("kml创建人");
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
        //过渡速度，起飞速度，飞向首航点wpml:globalTransitionalSpeed
        String globalTransitionalSpeed = Float.toString(missionConfig.getGlobalTransitionalSpeed());
        missionConfigElement.addElement("wpml:globalTransitionalSpeed").addText(globalTransitionalSpeed);
        //添加wpml:takeOffRefPoint，起飞参考点
        missionConfigElement.addElement("wpml:takeOffRefPoint").addText("23.98057,115.987663,100");
        //起飞参考高度wpml:takeOffRefPointAGLHeight
        missionConfigElement.addElement("wpml:takeOffRefPointAGLHeight").addText("35");

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
        folderElement.addElement("wpml:templateType").addText("waypoint");
        //给Folder添加子节点wpml:useGlobalTransitionalSpeed
        //folderElement.addElement("wpml:useGlobalTransitionalSpeed").addText("0");
        //给Folder添加子节点wpml:templateId
        folderElement.addElement("wpml:templateId").addText("0");

        //添加坐标系信息wpml:waylineCoordinateSysParam
        Element waylineCoordinateSysParam = folderElement.addElement("wpml:waylineCoordinateSysParam");
        waylineCoordinateSysParam.addElement("wpml:coordinateMode").addText("WGS84");
        //添加wpml:heightMode到waylineCoordinateSysParam
        waylineCoordinateSysParam.addElement("wpml:heightMode").addText("relativeToStartPoint");
        //添加wpml:positioningType到waylineCoordinateSysParam
        waylineCoordinateSysParam.addElement("wpml:positioningType").addText("GPS");

        //给Folder添加全局自动飞行速度wpml:autoFlightSpeed
        String autoFlightSpeed = Float.toString(folder.getAutoFlightSpeed());
        folderElement.addElement("wpml:autoFlightSpeed").addText(autoFlightSpeed);
        //给Folder添加全局飞行高度wpml:globalHeight
        String globalHeight = Float.toString(folder.getGlobalHeight());
        folderElement.addElement("wpml:globalHeight").addText(globalHeight);
        //是否开启标定飞行，350RTK，300RTK适用
        folderElement.addElement("wpml:caliFlightEnable").addText("0");
        //云台模式wpml:gimbalPitchMode
        folderElement.addElement("wpml:gimbalPitchMode").addText("manual");

        //全局偏航参数wpml:globalWaypointHeadingParam
        Element globalWaypointHeadingParam = folderElement.addElement("wpml:globalWaypointHeadingParam");
        //偏航方式飞行器偏航角模式
        globalWaypointHeadingParam.addElement("wpml:waypointHeadingMode").addText("followWayline");
        //偏航角度飞行器偏航角度
        globalWaypointHeadingParam.addElement("wpml:waypointHeadingAngle").addText("0");
        //兴趣点waypointPoiPoint
        globalWaypointHeadingParam.addElement("wpml:waypointPoiPoint").addText("0.000000,0.000000,0.000000");
        //偏航参数顺序
        globalWaypointHeadingParam.addElement("wpml:waypointHeadingPoiIndex").addText("0");

        //全局航点类型（全局航点转弯模式）
        String globalWaypointTurnMode = folder.getGlobalWaypointTurnMode();
        folderElement.addElement("wpml:globalWaypointTurnMode").addText(globalWaypointTurnMode);
        //全局航段轨迹是否尽量贴合直线wpml:globalUseStraightLine
        folderElement.addElement("wpml:globalUseStraightLine").addText("0");

        //循环添加每一个Placemark节点，有几个坐标点就有几个Placemark节点，这里仅作示例
        List<Placemark> placeMarks =folder.getPlaceMarks();
        for (int i = 0; i < placeMarks.size(); i++) {
            Element placeMarkElement = folderElement.addElement("Placemark");
            //航点坐标
            Element Point = placeMarkElement.addElement("Point");
            Placemark placemark = placeMarks.get(i);
            Point.addElement("coordinates").addText(placemark.getCoordinates());
            //航点序号
            placeMarkElement.addElement("wpml:index").addText(String.valueOf(i));
            //航点高度,84xi
            String height= Float.toString(placemark.getHeight());
            placeMarkElement.addElement("wpml:ellipsoidHeight").addText(height);
            //航点高度，96xi
            placeMarkElement.addElement("wpml:height").addText(height);
            //航点速度
            String waypointSpeed = Float.toString(placemark.getWaypointSpeed());
            placeMarkElement.addElement("wpml:waypointSpeed").addText(waypointSpeed);

            //偏航角
            Element waypointHeadingParam = placeMarkElement.addElement("wpml:waypointHeadingParam");
            waypointHeadingParam.addElement("wpml:waypointHeadingMode").addText("smoothTransition");
            String waypointHeadingAngle = Integer.toString(placemark.getWaypointHeadingAngle());
            waypointHeadingParam.addElement("wpml:waypointHeadingAngle").addText(waypointHeadingAngle);
            waypointHeadingParam.addElement("wpml:waypointPoiPoint").addText("0.000000,0.000000,0.000000");
            waypointHeadingParam.addElement("wpml:waypointHeadingPathMode").addText("followBadArc");
            waypointHeadingParam.addElement("wpml:waypointHeadingPoiIndex").addText("0");

            //航点模式，转弯类型waypointTurnParam
            Element waypointTurnParam = placeMarkElement.addElement("wpml:waypointTurnParam");
            waypointTurnParam.addElement("wpml:waypointTurnMode").addText("toPointAndStopWithDiscontinuityCurvature");
            waypointTurnParam.addElement("wpml:waypointTurnDampingDist").addText("0");

            //云台俯仰角度wpml:gimbalPitchAngle
            String gimbalPitchAngle = Integer.toString(placemark.getGimbalPitchAngle());
            placeMarkElement.addElement("wpml:gimbalPitchAngle").addText(gimbalPitchAngle);
            //是否贴合直线wpml:useStraightLine
            placeMarkElement.addElement("wpml:useStraightLine").addText("1");

            //添加动作组
            Element actionGroupElement = placeMarkElement.addElement("wpml:actionGroup");
            //动作组id
            actionGroupElement.addElement("wpml:actionGroupId").addText(String.valueOf(i));
            //动作组开始生效的航点wpml:actionGroupStartIndex
            actionGroupElement.addElement("wpml:actionGroupStartIndex").addText(String.valueOf(i));
            //动作组结束生效的航点wpml:actionGroupEndIndex
            actionGroupElement.addElement("wpml:actionGroupEndIndex").addText(String.valueOf(i));
            //动作执行模式wpml:actionGroupMode
            actionGroupElement.addElement("wpml:actionGroupMode").addText("sequence");
            //动作组触发器wpml:actionTrigger
            Element actionTriggerElement = actionGroupElement.addElement("wpml:actionTrigger");
            //动作触发器类型wpml:actionTriggerType
            actionTriggerElement.addElement("wpml:actionTriggerType").addText("reachPoint");

            //添加具体的动作执行参数
            List<Action> actionGroup = placemark.getActionGroup();
            for (int j =0; j < actionGroup.size(); j++) {
                Action action =actionGroup.get(j);
                if("takePhoto".equals(action.getActionActuatorFunc()))
                {
                    System.out.println("kkone");
                    Element actionElement =actionGroupElement.addElement("wpml:action");
                    actionElement.addElement("wpml:actionId").addText("0");
                    actionElement.addElement("wpml:actionActuatorFunc").addText("takePhoto");
                    Element actionActuatorFuncParamElement = actionElement.addElement("wpml:actionActuatorFuncParam");
                    String hangdian = "航点" + String.valueOf(j + 1);
                    actionActuatorFuncParamElement.addElement("wpml:fileSuffix").addText(hangdian);
                    actionActuatorFuncParamElement.addElement("wpml:payloadPositionIndex").addText("0");
                    actionActuatorFuncParamElement.addElement("wpml:useGlobalPayloadLensIndex").addText("0");
                }
            }

            //是否有风险
            placeMarkElement.addElement("wpml:isRisky").addText("0");
        }

        //添加负载参数
        Element payloadParamElement = folderElement.addElement("wpml:payloadParam");
        payloadParamElement.addElement("wpml:payloadPositionIndex").addText("0");
        payloadParamElement.addElement("wpml:meteringMode").addText("average");
        payloadParamElement.addElement("wpml:dewarpingEnable").addText("0");
        payloadParamElement.addElement("wpml:returnMode").addText("singleReturnStrongest");
        payloadParamElement.addElement("wpml:samplingRate").addText("240000");
        payloadParamElement.addElement("wpml:scanningMode").addText("nonRepetitive");
        payloadParamElement.addElement("wpml:modelColoringEnable").addText("0");

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
