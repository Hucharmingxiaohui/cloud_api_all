package com.dji.sample.df.electricInspectionDf.service;

import com.dji.sample.df.electricInspectionDf.model.PubDeviceDfEntity;

import java.util.List;

public interface PubDeviceDfService {
    // 添加设备
    boolean addDevice(PubDeviceDfEntity pubDeviceDfEntity);

    // 根据主设备ID查询设备
    PubDeviceDfEntity getDeviceById(int id);

    // 根据间隔编码查询设备列表
    List<PubDeviceDfEntity> getDeviceListByBayId(String bay_id);

    // 更新设备信息
    boolean updateDeviceById(PubDeviceDfEntity pubDeviceDfEntity);

    // 删除设备
    boolean deleteDeviceById(int id);
}
