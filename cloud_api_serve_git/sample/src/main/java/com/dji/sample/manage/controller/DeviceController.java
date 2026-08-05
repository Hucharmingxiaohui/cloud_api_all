package com.dji.sample.manage.controller;

import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.model.dto.DeviceFirmwareUpgradeDTO;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.manage.service.IDeviceService;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.config.version.GatewayManager;
import com.dji.sdk.exception.CloudSDKErrorEnum;
import com.dji.sdk.mqtt.property.PropertySetReplyResultEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @author sean.zhou
 * @version 0.1
 * @date 2021/11/15
 */
@RestController
@Slf4j
@RequestMapping("${url.manage.prefix}${url.manage.version}/devices")
public class DeviceController {

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IDeviceRedisService deviceRedisService;

    /**
     * Get the topology list of all online devices in one workspace.
     * @param workspaceId
     * @return
     */
    @GetMapping("/{workspace_id}/devices")
    public HttpResultResponse<List<DeviceDTO>> getDevices(@PathVariable("workspace_id") String workspaceId) {
        List<DeviceDTO> devicesList = deviceService.getDevicesTopoForWeb(workspaceId);

        return HttpResultResponse.success(devicesList);
    }

    /**
     * After binding the device to the workspace, the device data can only be seen on the web.
     * @param device
     * @param deviceSn
     * @return
     */
    @PostMapping("/{device_sn}/binding")
    public HttpResultResponse bindDevice(@RequestBody DeviceDTO device, @PathVariable("device_sn") String deviceSn) {
        device.setDeviceSn(deviceSn);
        boolean isUpd = deviceService.bindDevice(device);
        return isUpd ? HttpResultResponse.success() : HttpResultResponse.error();
    }

    /**
     * Obtain device information according to device sn.
     * @param workspaceId
     * @param deviceSn
     * @return
     */
    @GetMapping("/{workspace_id}/devices/{device_sn}")
    public HttpResultResponse getDevice(@PathVariable("workspace_id") String workspaceId,
                                        @PathVariable("device_sn") String deviceSn) {
        Optional<DeviceDTO> deviceOpt = deviceService.getDeviceBySn(deviceSn);
        return deviceOpt.isEmpty() ? HttpResultResponse.error("device not found.") : HttpResultResponse.success(deviceOpt.get());
    }

    /**
     * Get the binding devices list in one workspace.
     * @param workspaceId
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/{workspace_id}/devices/bound")
    public HttpResultResponse<PaginationData<DeviceDTO>> getBoundDevicesWithDomain(
            @PathVariable("workspace_id") String workspaceId, Integer domain,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(value = "page_size", defaultValue = "50") Long pageSize) {
        PaginationData<DeviceDTO> devices = deviceService.getBoundDevicesWithDomain(workspaceId, page, pageSize, domain);

        return HttpResultResponse.success(devices);
    }

    /**
     * todo(待测)临时修复接口：服务重启后机巢未重新上报update_topo时，手动补注册SDK设备并刷新在线缓存。
     */
    @PostMapping("/{workspace_id}/devices/{dock_sn}/repair-online")
    public HttpResultResponse repairDeviceOnline(@PathVariable("workspace_id") String workspaceId,
                                                 @PathVariable("dock_sn") String dockSn,
                                                 @RequestParam(value = "drone_sn", required = false) String droneSnParam) {
        Optional<DeviceDTO> dockOpt = deviceService.getDeviceBySn(dockSn);
        if (dockOpt.isEmpty()) {
            return HttpResultResponse.error("dock not found.");
        }

        DeviceDTO dock = dockOpt.get();
        String droneSn = StringUtils.hasText(droneSnParam) ? droneSnParam : dock.getChildDeviceSn();
        Optional<DeviceDTO> droneOpt = StringUtils.hasText(droneSn) ? deviceService.getDeviceBySn(droneSn) : Optional.empty();
        DeviceDTO drone = droneOpt.orElse(null);

        if (!StringUtils.hasText(dock.getWorkspaceId())) {
            dock.setWorkspaceId(workspaceId);
        }
        if (drone != null && !StringUtils.hasText(drone.getWorkspaceId())) {
            drone.setWorkspaceId(dock.getWorkspaceId());
        }

        GatewayManager gatewayManager = SDKManager.registerDevice(
                dock.getDeviceSn(),
                drone == null ? null : drone.getDeviceSn(),
                dock.getDomain(),
                dock.getType(),
                dock.getSubType(),
                dock.getThingVersion(),
                drone == null ? null : drone.getThingVersion()
        );

        deviceService.gatewayOnlineSubscribeTopic(gatewayManager);
        if (drone != null) {
            deviceService.subDeviceOnlineSubscribeTopic(gatewayManager);
        }

        dock.setStatus(true);
        dock.setLoginTime(LocalDateTime.now());
        dock.setChildDeviceSn(drone == null ? null : drone.getDeviceSn());
        deviceRedisService.setDeviceOnline(dock);

        if (drone != null) {
            drone.setStatus(true);
            drone.setLoginTime(LocalDateTime.now());
            drone.setParentSn(dock.getDeviceSn());
            deviceRedisService.setDeviceOnline(drone);
        }

        deviceService.pushDeviceOnlineTopo(dock.getWorkspaceId(), dock.getDeviceSn(), drone == null ? null : drone.getDeviceSn());
        log.warn("手动修复设备在线状态完成: dockSn={}, droneSn={}, workspaceId={}", dock.getDeviceSn(), droneSn, dock.getWorkspaceId());
        return HttpResultResponse.success();
    }

    /**
     * Removing the binding state of the device.
     * @param deviceSn
     * @return
     */
    @DeleteMapping("/{device_sn}/unbinding")
    public HttpResultResponse unbindingDevice(@PathVariable("device_sn") String deviceSn) {
        deviceService.unbindDevice(deviceSn);
        return HttpResultResponse.success();
    }

    /**
     * Update device information.
     * @param device
     * @param workspaceId
     * @param deviceSn
     * @return
     */
    @PutMapping("/{workspace_id}/devices/{device_sn}")
    public HttpResultResponse updateDevice(@RequestBody DeviceDTO device,
                                           @PathVariable("workspace_id") String workspaceId,
                                           @PathVariable("device_sn") String deviceSn) {
        device.setDeviceSn(deviceSn);
        boolean isUpd = deviceService.updateDevice(device);
        return isUpd ? HttpResultResponse.success() : HttpResultResponse.error();
    }

    /**
     * Delivers offline firmware upgrade tasks.
     * @param workspaceId
     * @param upgradeDTOS
     * @return
     */
    @PostMapping("/{workspace_id}/devices/ota")
    public HttpResultResponse createOtaJob(@PathVariable("workspace_id") String workspaceId,
                                           @RequestBody List<DeviceFirmwareUpgradeDTO> upgradeDTOS) {
        return deviceService.createDeviceOtaJob(workspaceId, upgradeDTOS);
    }

    /**
     * Set the property parameters of the drone.
     * @param workspaceId
     * @param dockSn
     * @param param
     * @return
     */
    @PutMapping("/{workspace_id}/devices/{device_sn}/property")
    public HttpResultResponse devicePropertySet(@PathVariable("workspace_id") String workspaceId,
                                                @PathVariable("device_sn") String dockSn,
                                                @RequestBody JsonNode param) {
        if (param.size() != 1) {
            return HttpResultResponse.error(CloudSDKErrorEnum.INVALID_PARAMETER);
        }

        int result = deviceService.devicePropertySet(workspaceId, dockSn, param);
        return PropertySetReplyResultEnum.SUCCESS.getResult() == result ?
                HttpResultResponse.success() : HttpResultResponse.error(result, String.valueOf(result));
    }
}
