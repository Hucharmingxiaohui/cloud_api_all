package com.dji.sample.component.mqtt.handler;

import com.dji.sdk.mqtt.ChannelName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class CustomMqttMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomMqttMessageHandler.class);

    private static final String DRONE_OSD = "/[^/]+/[^/]+/drone/osd";
    private static final String DOCK_OSD = "/[^/]+/[^/]+/dock/osd";
    private static final String PUSH_HMS = "/[^/]+/[^/]+/push/hms";
    private static final String PUSH_PICTURE = "/[^/]+/push/picture";

    @Resource(name = ChannelName.INBOUND_CUSTOM_DRONE_OSD)
    private MessageChannel customDroneOsdChannel;

    @Resource(name = ChannelName.INBOUND_CUSTOM_DOCK_OSD)
    private MessageChannel customDockOsdChannel;

    @Resource(name = ChannelName.INBOUND_CUSTOM_HMS)
    private MessageChannel customHmsChannel;

    @Resource(name = ChannelName.INBOUND_CUSTOM_PICTURE)
    private MessageChannel customPictureChannel;

    @ServiceActivator(inputChannel = ChannelName.CUSTOM_INBOUND)
    public void handleMessage(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        byte[] payload = (byte[]) message.getPayload();
        log.info("[cq-dock] custom mqtt inbound topic={}, payloadLen={}",
                topic, payload == null ? 0 : payload.length);

        if (topic == null) {
            log.warn("[cq-dock] custom mqtt topic is null");
            return;
        }

        try {
            if (topic.matches(DRONE_OSD)) {
                customDroneOsdChannel.send(message);
                return;
            }
            if (topic.matches(DOCK_OSD)) {
                customDockOsdChannel.send(message);
                return;
            }
            if (topic.matches(PUSH_HMS)) {
                customHmsChannel.send(message);
                return;
            }
            if (topic.matches(PUSH_PICTURE)) {
                customPictureChannel.send(message);
                return;
            }
            log.info("[cq-dock] custom mqtt unhandled topic: {}, payload: {}", topic, new String(payload));
        } catch (Exception e) {
            log.error("[cq-dock] custom mqtt dispatch failed, topic={}, err={}", topic, e.getMessage(), e);
        }
    }
}
