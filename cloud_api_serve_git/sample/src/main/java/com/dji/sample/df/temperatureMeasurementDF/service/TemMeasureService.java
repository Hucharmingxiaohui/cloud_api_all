package com.dji.sample.df.temperatureMeasurementDF.service;

import com.dji.sample.df.temperatureMeasurementDF.modol.TemParamEntity;
import com.dji.sample.df.temperatureMeasurementDF.modol.TemResultEntity;

//红外测温接口
public interface TemMeasureService {
    //1.根据workspace_id file_id下载图片并返回温度，点测温返回温度（坐标），区域测温返回最大、最小温度坐标
    TemResultEntity getTemByWorkSpaceIdAndFileId(String workspace_id, String file_id, TemParamEntity temParamEntity) ;
    boolean bindPoint(TemParamEntity temParamEntity) ;
}
