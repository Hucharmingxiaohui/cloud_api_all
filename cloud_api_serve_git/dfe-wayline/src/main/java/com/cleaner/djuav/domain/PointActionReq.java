package com.cleaner.djuav.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class PointActionReq implements Serializable {

    /**
     * 动作编号
     */
    private Integer actionIndex;

    /**
     * 飞行器悬停等待时间
     */
    private Double hoverTime;

    /**
     * 飞行器目标偏航角
     */
    private Double aircraftHeading;

    private String aircraftPathMode;  //顺时针逆时针


    /**
     * 普通拍照：0，全景拍照：1
     */
    private Integer takePhotoType;

    /**
     * 是否使用全局拍照模式 0：不使用 1：使用
     */
    private Integer useGlobalImageFormat;

    /**
     * 拍照模式（字典）
     */
    private String imageFormat;


    /**
     * 云台偏航角
     */
    private Double gimbalYawRotateAngle;

    /**
     * 云台俯仰角
     */
    private Double gimbalPitchRotateAngle;

    /**
     * 变焦焦距
     */
    private Double zoom;

    /**
     * 开始录像
     */
    private Boolean startRecord;

    /**
     * 停止录像
     */
    private Boolean stopRecord;


    //2025/7/30 xtj  补充精准动作拍照orientedShoot相关参数
    private String orientedPhotoMode;

    private Double focalLength;

    private Double AFPos;

    private Double focusX;

    private Double focusY;

    private Double focusRegionWidth;

    private Double focusRegionHeight;

    private Integer imageWidth;

    private Integer imageHeight;

    private Integer orientedCameraApertue;

    private Integer orientedCameraLuminance;

    private Double orientedCameraShutterTime;

    private Integer orientedCameraISO;







}
