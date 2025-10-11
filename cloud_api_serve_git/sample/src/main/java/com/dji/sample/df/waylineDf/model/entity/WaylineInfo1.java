package com.dji.sample.df.waylineDf.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class WaylineInfo1 {
    private Map<String,Object> kmlmap = new HashMap<>();
    //任务信息
    private Map<String,Object> missionConfig = new HashMap<>();
    //无人机信息
    private Map<String,String> droneInfo = new HashMap<>();
    //负载信息
    private Map<String,String> payloadInfo = new HashMap<>();
    //添加Folder节点
    private Map<String,Object> Folder = new HashMap<>();
    //卫星系统
    private Map<String,String> waylineCoordinateSysParam = new HashMap<>();
    //全局偏航信息
    private Map<String,String> globalWaypointHeadingParam = new HashMap<>();
    //航点信息列表
    //存储多个Placemark数据的列表
    private List<Map<String, Object>> placemarks = new ArrayList<>();
    private Map<String,Object> Placemark = new HashMap<>();

    public WaylineInfo1() {
        //基本信息
        kmlmap.put("author", "Name");
        Instant currentInstant = Instant.now();
        kmlmap.put("createTime",currentInstant);
        kmlmap.put("updateTime",currentInstant);

        //无人机配置信息
        kmlmap.put("missionConfig",missionConfig);
            missionConfig.put("flyToWaylineMode","safely");
            missionConfig.put("finishAction","goHome");
            missionConfig.put("exitOnRCLost","executeLostAction");
            missionConfig.put("executeRCLostAction","goBack");
            //安全起飞高度
            missionConfig.put("takeOffSecurityHeight","10");
            //过渡速度
            missionConfig.put("globalTransitionalSpeed","2");
            //无人机信息
            missionConfig.put("droneInfo",droneInfo);
                droneInfo.put("droneEnumValue","77");
                droneInfo.put("droneSubEnumValue","0");
            missionConfig.put("payloadInfo",payloadInfo);
                payloadInfo.put("payloadEnumValue","66");
                payloadInfo.put("payloadSubEnumValue","0");
                payloadInfo.put("payloadPositionIndex","0");
        kmlmap.put("Folder",Folder);
            Folder.put("templateType","waypoint");
            Folder.put("templateId","0");
            Folder.put("waylineCoordinateSysParam",waylineCoordinateSysParam);
                waylineCoordinateSysParam.put("coordinateMode","WGS84");
                waylineCoordinateSysParam.put("heightMode","relativeToStartPoint");
                waylineCoordinateSysParam.put("positioningType","GPS");
            Folder.put("autoFlightSpeed","1");
            Folder.put("globalHeight","10");
            Folder.put("caliFlightEnable","0");
            Folder.put("gimbalPitchMode","manual");
            Folder.put("globalWaypointHeadingParam",globalWaypointHeadingParam);
                globalWaypointHeadingParam.put("waypointHeadingMode","followWayline");
                globalWaypointHeadingParam.put("waypointHeadingAngle","0");
                globalWaypointHeadingParam.put("waypointPoiPoint","0.000000,0.000000,0.000000");
                globalWaypointHeadingParam.put("waypointHeadingPoiIndex","0");
            Folder.put("globalWaypointTurnMode","toPointAndStopWithDiscontinuityCurvature");
            Folder.put("globalUseStraightLine","1");
            for (int i = 0;i<placemarks.size();i++)
            {
                Folder.put("Placemark",placemarks.get(i));
            }

    }
    private Map<String, Object> createActionGroup(int index) {
        Map<String, Object> actionGroup = new HashMap<>();
        actionGroup.put("actionGroupId", index);
        actionGroup.put("actionGroupStartIndex", index);
        actionGroup.put("actionGroupEndIndex", index);
        actionGroup.put("actionGroupMode", "sequence");
        actionGroup.put("actionTrigger", createActionTrigger());
        actionGroup.put("action", createAction(index));
        return actionGroup;
    }
    //创建一个ActionTrigger数据的方法
    private Map<String, Object> createActionTrigger() {
        Map<String, Object> actionTrigger = new HashMap<>();
        actionTrigger.put("actionTriggerType", "reachPoint");
        return actionTrigger;
    }

    //创建一个Action数据的方法
    private Map<String, Object> createAction(int index) {
        Map<String, Object> action = new HashMap<>();
        action.put("actionId", index);
        action.put("actionActuatorFunc", "takePhoto");
        action.put("actionActuatorFuncParam", createActionActuatorFuncParam(index));
        return action;
    }

    //创建一个ActionActuatorFuncParam数据的方法
    private Map<String, Object> createActionActuatorFuncParam(int index) {
        Map<String, Object> actionActuatorFuncParam = new HashMap<>();
        actionActuatorFuncParam.put("fileSuffix", "航点" + (index + 1));
        actionActuatorFuncParam.put("payloadPositionIndex", 0);
        actionActuatorFuncParam.put("useGlobalPayloadLensIndex", 0);
        return actionActuatorFuncParam;
    }

}
