package com.dji.sample.df.temperatureMeasurementDF.service;

import com.dji.sample.df.temperatureMeasurementDF.modol.TemParamEntity;
import com.dji.sample.df.temperatureMeasurementDF.modol.TemResultEntity;

//红外测温接口
public interface TemMeasureService {
    //1.根据workspace_id file_id下载图片并返回温度，点测温返回温度（坐标），区域测温返回最大、最小温度坐标
    TemResultEntity getTemByWorkSpaceIdAndFileId(String workspace_id, String file_id, TemParamEntity temParamEntity) ;
    boolean bindPoint(TemParamEntity temParamEntity) ;

    /**
     * 直接对本地已下载的红外图片测温（重庆EUA图片链路使用，图片不在平台媒体库中）
     * @param imagePath 本地红外图片绝对路径（R-JPEG）
     * @param temParamEntity 测温参数（区域或点坐标）
     */
    TemResultEntity getTemByLocalImagePath(String imagePath, TemParamEntity temParamEntity) ;
}
