package com.dji.sample.control.model.dto;

import com.dji.sample.common.util.SpringBeanUtilsTest;
import com.dji.sample.control.service.impl.RemoteDebugHandler;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sdk.cloudapi.device.OsdDockDrone;

/**
 * @author sean
 * @version 1.4
 * @date 2023/4/19
 */

public class ReturnHomeState extends RemoteDebugHandler {

    @Override
    public boolean canPublish(String sn) {
        IDeviceRedisService deviceRedisService = SpringBeanUtilsTest.getBean(IDeviceRedisService.class);
        // 只要无人机在空中就允许返航（含蛙跳任务被停后无任务悬停 IDLE 的场景），
        // mode_code 白名单会把悬停 IDLE 误拦导致无法紧急返航；设备侧会做最终校验
        return deviceRedisService.getDeviceOnline(sn)
                .map(DeviceDTO::getChildDeviceSn)
                .flatMap(deviceSn -> deviceRedisService.getDeviceOsd(deviceSn, OsdDockDrone.class))
                .map(osd -> osd.getElevation() > 0)
                .orElse(false);
    }
}
