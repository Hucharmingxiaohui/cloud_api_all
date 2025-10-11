package com.dji.sample.df.electricInspectionDf.service;

import com.dji.sample.df.electricInspectionDf.model.PubComponentDfEntity;

import java.util.List;

public interface PubComponentDfService {
    // 添加部件信息
    boolean addComponent(PubComponentDfEntity pubComponentDfEntity);

    // 根据设备ID查询部件列表
    List<PubComponentDfEntity> getComponentListByDeviceId(String device_id);

    // 更新部件信息
    boolean updateComponentById(PubComponentDfEntity pubComponentDfEntity);

    // 删除部件
    boolean deleteComponentById(int id);

}
