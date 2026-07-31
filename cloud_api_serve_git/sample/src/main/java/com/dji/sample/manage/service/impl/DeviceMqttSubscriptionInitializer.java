package com.dji.sample.manage.service.impl;

import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.model.param.DeviceQueryParam;
import com.dji.sample.manage.service.IDeviceService;
import com.dji.sdk.mqtt.IMqttTopicService;
import com.dji.sdk.cloudapi.device.DeviceDomainEnum;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.config.version.GatewayManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@Slf4j
public class DeviceMqttSubscriptionInitializer {

    private static final String DEFAULT_DOCK3_THING_VERSION = "1.2.7";

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IMqttTopicService mqttTopicService;

    @EventListener(ApplicationReadyEvent.class)
    public void subscribeBoundGatewayTopics() {
        log.info("MQTT topics before startup compensation: {}", String.join(",", mqttTopicService.getSubscribedTopic()));
        List<DeviceDTO> gateways = deviceService.getDevicesByParams(DeviceQueryParam.builder()
                .domains(List.of(DeviceDomainEnum.DOCK.getDomain()))
                .boundStatus(true)
                .build());
        log.info("Bound dock count for startup MQTT compensation: {}", gateways.size());
        for (DeviceDTO gateway : gateways) {
            if (!StringUtils.hasText(gateway.getDeviceSn())) {
                continue;
            }
            try {
                GatewayManager gatewayManager = SDKManager.registerDevice(
                        gateway.getDeviceSn(),
                        gateway.getChildDeviceSn(),
                        gateway.getDomain(),
                        gateway.getType(),
                        gateway.getSubType(),
                        StringUtils.hasText(gateway.getThingVersion()) ? gateway.getThingVersion() : DEFAULT_DOCK3_THING_VERSION,
                        DEFAULT_DOCK3_THING_VERSION);
                if (StringUtils.hasText(gateway.getChildDeviceSn())) {
                    deviceService.subDeviceOnlineSubscribeTopic(gatewayManager);
                } else {
                    deviceService.gatewayOnlineSubscribeTopic(gatewayManager);
                }
                log.info("Subscribed gateway MQTT topics after application startup. gatewaySn={}, childSn={}",
                        gateway.getDeviceSn(), gateway.getChildDeviceSn());
            } catch (Exception e) {
                log.warn("Failed to subscribe gateway MQTT topics after startup. gatewaySn={}", gateway.getDeviceSn(), e);
            }
        }
        log.info("MQTT topics after startup compensation: {}", String.join(",", mqttTopicService.getSubscribedTopic()));
    }
}
