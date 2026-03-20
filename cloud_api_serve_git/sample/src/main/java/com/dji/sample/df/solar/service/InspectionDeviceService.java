package com.dji.sample.df.solar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dji.sample.df.solar.model.entity.InspectionDevice;

import java.util.Map;

public interface InspectionDeviceService extends IService<InspectionDevice> {
    boolean saveInspectionDevice(InspectionDevice inspectionDevice);
    boolean updateInspectionDeviceById(InspectionDevice inspectionDevice);
    boolean removeInspectionDeviceById(Long id);
    InspectionDevice getInspectionDeviceById(Long id);
    Map<String, Object> selectList(Map<String, Object> params);
}
