package com.dji.sample.df.electricInspectionDf.service;

import com.dji.sample.df.electricInspectionDf.model.PubUavDeviceDfEntity;

import java.util.List;

public interface PubUavDeviceDfService {
    //添加设备
    boolean addDeviceToSub(PubUavDeviceDfEntity pubUavDeviceDfEntity);
    //通过场站id查询设备
    List<PubUavDeviceDfEntity> getUavDeviceBySubCode(String sub_code);
    //更新场站无人机信息
    boolean updateUavDeviceById(PubUavDeviceDfEntity pubUavDeviceDfEntity);
    //删除设备
    boolean deleteUavDeviceById(Integer id);
}
