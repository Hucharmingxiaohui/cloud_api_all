package com.dji.sample.df.kmzDf.entity.wayline.MissionConfig;


import com.dji.sample.df.kmzDf.entity.wayline.MissionConfig.DroneInfo.DroneInfo;
import com.dji.sample.df.kmzDf.entity.wayline.MissionConfig.PayloadInfo.PayloadInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MissionConfig {
    @JsonProperty("fileName")
    private String fileName; //航线名称
    @JsonProperty("flyToWaylineMode")
    private String flyToWaylineMode="safely";//飞向首航点模式:safely：安全模式 、pointToPoint：倾斜飞行模式
    @JsonProperty("finishAction")
    private String finishAction="goHome";//航线结束动作
    @JsonProperty("exitOnRCLost")//失控是否继续执行航线
    private String exitOnRCLost="executeLostAction";//
    @JsonProperty("executeRCLostAction")
    private String executeRCLostAction="goBack";    //失控动作类型:
    @JsonProperty("takeOffSecurityHeight")
    private float takeOffSecurityHeight;    //安全起飞高度,无默认值
    @JsonProperty("globalTransitionalSpeed")
    private float globalTransitionalSpeed;    //飞向首航点速度、起飞速度，无默认值
    @JsonProperty("droneInfo")
    private DroneInfo droneInfo; //无人机信息,无默认值
    @JsonProperty("payloadInfo")
    private PayloadInfo payloadInfo; //负载信息，无默认值

}
