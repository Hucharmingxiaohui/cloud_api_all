package com.dji.sample.wayline.service.impl;

import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sdk.cloudapi.device.*;
import com.dji.sdk.cloudapi.property.PropertySetEnum;
import com.dji.sdk.cloudapi.property.ThermalCurrentPaletteStyleSet;
import com.dji.sdk.cloudapi.property.api.AbstractPropertyService;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.config.version.GatewayManager;
import com.dji.sdk.mqtt.property.PropertySetReplyResultEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

@Component
@ConfigurationProperties(prefix = "mission.thermal-palette")
public class MissionThermalPaletteService {
    private final IDeviceRedisService devices;
    private final AbstractPropertyService properties;
    private boolean enabled;
    private ThermalPaletteStyleEnum style = ThermalPaletteStyleEnum.IRONBOW1;

    public MissionThermalPaletteService(IDeviceRedisService devices, AbstractPropertyService properties) {
        this.devices = devices;
        this.properties = properties;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getStyle() { return style.getStyle(); }
    public void setStyle(int style) { this.style = ThermalPaletteStyleEnum.find(style); }

    public void beforeExecute(String dockSn) {
        if (!enabled) return;
        GatewayManager gateway = SDKManager.getDeviceSDK(dockSn);
        if (!StringUtils.hasText(gateway.getDroneSn())) {
            throw new IllegalStateException("Thermal palette: aircraft is not bound to dock " + dockSn);
        }
        OsdDockDrone osd = devices.getDeviceOsd(gateway.getDroneSn(), OsdDockDrone.class)
                .orElseThrow(() -> new IllegalStateException("Thermal palette: aircraft/camera is offline; execute cancelled."));
        ThermalCurrentPaletteStyleSet request = new ThermalCurrentPaletteStyleSet()
                .setPayloadIndex(thermalPayload(osd)).setThermalCurrentPaletteStyle(style);
        PropertySetReplyResultEnum ack = properties.propertySet(gateway,
                PropertySetEnum.THERMAL_CURRENT_PALETTE_STYLE, request);
        if (ack != PropertySetReplyResultEnum.SUCCESS) {
            throw new IllegalStateException("Thermal palette: missing or unsuccessful ACK (" + ack + "); execute cancelled.");
        }
    }

    public static PayloadIndex thermalPayload(OsdDockDrone osd) {
        Map<String, PayloadIndex> candidates = new LinkedHashMap<>();
        if (osd.getCameras() != null) {
            for (OsdCamera camera : osd.getCameras()) {
                if (camera == null || camera.getPayloadIndex() == null) continue;
                PayloadIndex index = camera.getPayloadIndex();
                if (EnumSet.of(DeviceTypeEnum.M30T_CAMERA, DeviceTypeEnum.M30_OR_M3T_CAMERA,
                        DeviceTypeEnum.M3TD_CAMERA, DeviceTypeEnum.M4TD_CAMERA, DeviceTypeEnum.H20T,
                        DeviceTypeEnum.H20N, DeviceTypeEnum.XT2, DeviceTypeEnum.XTS).contains(index.getType())) {
                    candidates.put(index.toString(), index);
                }
            }
        }
        if (osd.getPayloads() != null) {
            for (DockDronePayload payload : osd.getPayloads()) {
                if (payload != null && payload.getPayloadIndex() != null && payload.getThermalCurrentPaletteStyle() != null) {
                    candidates.put(payload.getPayloadIndex().toString(), payload.getPayloadIndex());
                }
            }
        }
        if (candidates.size() != 1) {
            throw new IllegalStateException("Thermal palette: exactly one online infrared camera is required; found "
                    + candidates.size() + ". Device unsupported, camera unavailable or multiple thermal payloads.");
        }
        return candidates.values().iterator().next();
    }
}
