package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam;

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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
//动作参数
@Data
public class ActionActuatorFuncParam {
    @JsonProperty("takePhoto")
    private TakePhoto takePhoto; //拍照参数
    @JsonProperty("orientedShoot")
    private OrientedShoot orientedShoot; //定向拍照参数
    @JsonProperty("startRecord")
    private StartRecord startRecord; //开始录像
    @JsonProperty("stopRecord")
    private StopRecord stopRecord; //停止录像
    @JsonProperty("panoShot")
    private PanoShot panoShot; //全景拍照
    @JsonProperty("gimbalRotate")
    private GimbalRotate gimbalRotate; //转动云台
    @JsonProperty("customDirName")
    private CustomDirName customDirName; //创建文件夹
    @JsonProperty("focus")
    private Focus focus; //对焦
    @JsonProperty("hover")
    private Hover hover; //悬停
    @JsonProperty("recordPointCloud")
    private RecordPointCloud recordPointCloud; //记录点云
    @JsonProperty("rotateYaw")
    private RotateYaw rotateYaw; //偏航
    @JsonProperty("zoom")
    private Zoom zoom; //变焦
}
