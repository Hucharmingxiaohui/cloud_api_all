package com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.OrientedShoot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

//定向拍照
@Data
public class OrientedShoot {
    @JsonProperty("gimbalPitchRotateAngle")
    private float gimbalPitchRotateAngle=0;//云台Pitch转动角度
    @JsonProperty("gimbalRollRotateAngle")
    private float gimbalRollRotateAngle=0;//前端不需要传
    @JsonProperty("gimbalYawRotateAngle")
    private float gimbalYawRotateAngle=0;//云台Yaw转动角度
    @JsonProperty("focusX")
    private int focusX=0;//目标选中框中心水平坐标
    @JsonProperty("focusY")
    private int focusY=0;//目标选中框中心竖直坐标
    @JsonProperty("focusRegionWidth")
    private int focusRegionWidth=0;//目标选中框宽
    @JsonProperty("focusRegionHeight")
    private int focusRegionHeight=0;//目标选中框高
    @JsonProperty("focalLength")
    private float focalLength=48;//变焦焦距
    @JsonProperty("aircraftHeading")
    private float aircraftHeading=0;//飞行器目标偏航角（相对于地理北）
    @JsonProperty("accurateFrameValid")
    private int accurateFrameValid=0;//是否框选精准复拍目标
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex=0;//负载挂载位置
    @JsonProperty("payloadLensIndex")
    private String payloadLensIndex="ir,zoom,wide";//拍摄照片存储类型
    @JsonProperty("useGlobalPayloadLensIndex")
    private int useGlobalPayloadLensIndex=1;//是否使用全局存储类型
    @JsonProperty("targetAngle")
    private float targetAngle=0;//目标框角度
    @JsonProperty("actionUUID")
    private String actionUUID="0";//动作唯一标识
    @JsonProperty("imageWidth")
    private int imageWidth=0;//照片宽度
    @JsonProperty("imageHeight")
    private int imageHeight=0;//照片高度
    @JsonProperty("AFPos")
    private int AFPos=0;//AF电机位置
    @JsonProperty("gimbalPort")
    private int gimbalPort=0;//云台端口号
    @JsonProperty("orientedCameraType")
    private int orientedCameraType=53;//相机类型
    @JsonProperty("orientedFilePath")
    private String orientedFilePath="0";//照片文件路径
    @JsonProperty("orientedFileMD5")
    private String orientedFileMD5;//照片文件MD5
    @JsonProperty("orientedFileSuffix")
    private String orientedFileSuffix="航点";//照片文件后缀
    @JsonProperty("orientedCameraApertue")
    private int orientedCameraApertue=0;//光圈大小
    @JsonProperty("orientedCameraLuminance")
    private int orientedCameraLuminance=0;//环境亮度
    @JsonProperty("orientedCameraShutterTime")
    private float orientedCameraShutterTime=1;//快门时间
    @JsonProperty("orientedCameraISO")
    private int orientedCameraISO=0;//ISO
    @JsonProperty("orientedPhotoMode")
    private String orientedPhotoMode="normalPhoto";//拍照模式
}



