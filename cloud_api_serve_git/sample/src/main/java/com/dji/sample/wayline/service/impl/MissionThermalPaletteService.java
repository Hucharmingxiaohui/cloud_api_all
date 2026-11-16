package com.dji.sample.wayline.service.impl;

import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.manage.service.IDeviceService;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sdk.cloudapi.debug.api.AbstractDebugService;
import com.dji.sdk.cloudapi.debug.RemoteDebugResponse;
import com.dji.sdk.cloudapi.device.*;
import com.dji.sdk.cloudapi.property.PropertySetEnum;
import com.dji.sdk.cloudapi.property.ThermalCurrentPaletteStyleSet;
import com.dji.sdk.cloudapi.property.api.AbstractPropertyService;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.config.version.GatewayManager;
import com.dji.sdk.mqtt.property.PropertySetReplyResultEnum;
import com.dji.sdk.mqtt.services.ServicesReplyData;
import com.dji.sdk.mqtt.services.TopicServicesResponse;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@ConfigurationProperties(prefix = "mission.thermal-palette")
public class MissionThermalPaletteService {
    private static final long DRONE_START_TIMEOUT_SECONDS = 90;
    private static final long DEBUG_MODE_TIMEOUT_SECONDS = 15;
    private static final long OSD_POLL_INTERVAL_MILLIS = 1000;
    private final IDeviceRedisService devices;
    private final AbstractPropertyService properties;
    private final AbstractDebugService debugService;
    private final IDeviceService deviceService;
    private boolean enabled;
    private ThermalPaletteStyleEnum style = ThermalPaletteStyleEnum.IRONBOW1;

    public MissionThermalPaletteService(IDeviceRedisService devices, AbstractPropertyService properties,
                                        AbstractDebugService debugService, IDeviceService deviceService) {
        this.devices = devices;
        this.properties = properties;
        this.debugService = debugService;
        this.deviceService = deviceService;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getStyle() { return style.getStyle(); }
    public void setStyle(int style) { this.style = ThermalPaletteStyleEnum.find(style); }

    public void beforeExecute(String dockSn) {
        if (!enabled) return;
        DeviceDTO dock = boundDock(dockSn);
        GatewayManager gateway = checkedGateway(dock);
        OsdDockDrone osd = devices.getDeviceOsd(dock.getChildDeviceSn(), OsdDockDrone.class)
                .orElseThrow(() -> new IllegalStateException("Thermal palette: aircraft/camera is offline; execute cancelled."));
        ThermalCurrentPaletteStyleSet request = new ThermalCurrentPaletteStyleSet()
                .setPayloadIndex(thermalPayload(osd)).setThermalCurrentPaletteStyle(style);
        PropertySetReplyResultEnum ack = properties.propertySet(gateway,
                PropertySetEnum.THERMAL_CURRENT_PALETTE_STYLE, request);
        if (ack != PropertySetReplyResultEnum.SUCCESS) {
            throw new IllegalStateException("Thermal palette: missing or unsuccessful ACK (" + ack + "); execute cancelled.");
        }
    }

    public void powerOnBeforePrepare(String dockSn) {
        if (!enabled) return;
        DeviceDTO dock = boundDock(dockSn);
        GatewayManager gateway = checkedGateway(dock);
        if (devices.getDeviceOsd(dock.getChildDeviceSn(), OsdDockDrone.class).isPresent()) return;

        checkDebugReply(debugService.debugModeOpen(gateway), "debug_mode_open");
        try {
            awaitDockMode(dockSn, DockModeCodeEnum.REMOTE_DEBUGGING);
            awaitDroneOsd(gateway, dock.getChildDeviceSn());
        } finally {
            checkDebugReply(debugService.debugModeClose(gateway), "debug_mode_close");
        }
        awaitDockMode(dockSn, DockModeCodeEnum.IDLE);
    }

    private DeviceDTO boundDock(String dockSn) {
        DeviceDTO dock = deviceService.getDeviceBySn(dockSn)
                .orElseThrow(() -> new IllegalStateException("Thermal palette: dock not found: " + dockSn));
        if (dock.getDomain() != DeviceDomainEnum.DOCK || !Boolean.TRUE.equals(dock.getBoundStatus())
                || !StringUtils.hasText(dock.getChildDeviceSn())) {
            throw new IllegalStateException("Thermal palette: aircraft is not bound to dock " + dockSn);
        }
        DeviceDTO drone = deviceService.getDeviceBySn(dock.getChildDeviceSn())
                .orElseThrow(() -> new IllegalStateException("Thermal palette: bound aircraft not found for dock " + dockSn));
        if (!Boolean.TRUE.equals(drone.getBoundStatus()) || !StringUtils.hasText(dock.getWorkspaceId())
                || !dock.getWorkspaceId().equals(drone.getWorkspaceId())) {
            throw new IllegalStateException("Thermal palette: aircraft binding does not match dock " + dockSn);
        }
        return dock;
    }

    private GatewayManager checkedGateway(DeviceDTO dock) {
        GatewayManager gateway = SDKManager.getDeviceSDK(dock.getDeviceSn());
        String currentSn = gateway.getDroneSn();
        if (StringUtils.hasText(currentSn) && !currentSn.equals(dock.getChildDeviceSn())) {
            throw new IllegalStateException("Thermal palette: aircraft SN differs from bound dock " + dock.getDeviceSn());
        }
        if (StringUtils.hasText(currentSn)) return gateway;
        DeviceDTO drone = deviceService.getDeviceBySn(dock.getChildDeviceSn())
                .orElseThrow(() -> new IllegalStateException("Thermal palette: bound aircraft not found for dock " + dock.getDeviceSn()));
        return new GatewayManager(dock.getDeviceSn(), dock.getChildDeviceSn(), gateway.getType(),
                gateway.getGatewayThingVersion().getThingVersion(), drone.getThingVersion());
    }

    private void awaitDockMode(String dockSn, DockModeCodeEnum expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(DEBUG_MODE_TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (devices.getDeviceOsd(dockSn, OsdDock.class)
                    .map(OsdDock::getModeCode).filter(expected::equals).isPresent()) return;
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("Thermal palette: interrupted waiting for dock mode " + expected);
            }
            try {
                TimeUnit.MILLISECONDS.sleep(OSD_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thermal palette: interrupted waiting for dock mode " + expected, e);
            }
        }
        throw new IllegalStateException("Thermal palette: dock did not reach mode " + expected
                + " within " + DEBUG_MODE_TIMEOUT_SECONDS + " seconds; execute cancelled.");
    }

    private void checkDebugReply(TopicServicesResponse<ServicesReplyData<RemoteDebugResponse>> reply,
                                 String command) {
        if (reply == null || reply.getData() == null || reply.getData().getResult() == null
                || !reply.getData().getResult().isSuccess()) {
            throw new IllegalStateException("Thermal palette: " + command + " failed ("
                    + (reply == null || reply.getData() == null ? "no ACK" : reply.getData().getResult())
                    + "); execute cancelled.");
        }
    }

    OsdDockDrone awaitDroneOsd(GatewayManager gateway, String droneSn) {
        Optional<OsdDockDrone> osd = devices.getDeviceOsd(droneSn, OsdDockDrone.class);
        if (osd.isPresent()) {
            return osd.get();
        }

        var reply = debugService.droneOpen(gateway);
        if (reply == null || reply.getData() == null || reply.getData().getResult() == null) {
            throw new IllegalStateException("Thermal palette: drone_open was not acknowledged; execute cancelled.");
        }
        if (!reply.getData().getResult().isSuccess()) {
            throw new IllegalStateException("Thermal palette: drone_open failed ("
                    + reply.getData().getResult() + "); execute cancelled.");
        }

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(DRONE_START_TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thermal palette: interrupted waiting for aircraft OSD; execute cancelled.");
            }
            osd = devices.getDeviceOsd(droneSn, OsdDockDrone.class);
            if (osd.isPresent()) {
                return osd.get();
            }
            try {
                TimeUnit.MILLISECONDS.sleep(OSD_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thermal palette: interrupted waiting for aircraft OSD; execute cancelled.", e);
            }
        }
        throw new IllegalStateException("Thermal palette: aircraft OSD not received within "
                + DRONE_START_TIMEOUT_SECONDS + " seconds after drone_open; execute cancelled.");
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
