package com.dji.sample.df.cqDockDf.service;

import com.dji.sample.center.config.SwitchConfig;
import com.dji.sample.df.cqDockDf.model.entity.CqDockUavMonitoringEntity;
import com.dji.sample.manage.model.entity.DeviceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

//  重庆EUA平台上报给巡视无人机相关数据上报类
@Service
public class CqDockUavMonitoringReportService {

    @Autowired
    private SwitchConfig switchConfig;
    @Autowired
    private CqDockUavMonitoringService monitoringService;

    public boolean isEuaDataEnabled() {
        return "true".equalsIgnoreCase(switchConfig.getCenterEuaTcpDataEnable());
    }

    public CqDockUavMonitoringEntity findDroneMonitoring(DeviceEntity device) {
        if (!isEuaDataEnabled()) {
            return null;
        }
        if (device != null && StringUtils.hasText(device.getDeviceSn())) {
            return monitoringService.findLatestByDroneSn(device.getDeviceSn());
        }
        return null;
    }

    public CqDockUavMonitoringEntity findDockMonitoring(DeviceEntity device) {
        if (!isEuaDataEnabled()) {
            return null;
        }
        if (device != null && StringUtils.hasText(device.getDeviceSn())) {
            return monitoringService.findLatestByDockSn(device.getDeviceSn());
        }
        return null;
    }

    public CqDockUavMonitoringEntity findLatestAny() {
        if (!isEuaDataEnabled()) {
            return null;
        }
        return monitoringService.findLatestAny();
    }

    public String value(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

}
