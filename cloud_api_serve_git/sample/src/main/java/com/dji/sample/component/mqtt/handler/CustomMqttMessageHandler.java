package com.dji.sample.component.mqtt.handler;

import com.dji.sdk.mqtt.ChannelName;
import com.dji.sample.df.cqDockDf.service.CqDockMqttTopic;
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
            CqDockMqttTopic parsed = CqDockMqttTopic.parse(topic);
            if (parsed == null) return;
            if ("drone/osd".equals(parsed.getKind())) {
                customDroneOsdChannel.send(message);
                return;
            }
            if ("dock/osd".equals(parsed.getKind())) {
                customDockOsdChannel.send(message);
                return;
            }
            if ("push/hms".equals(parsed.getKind())) {
                customHmsChannel.send(message);
                return;
            }
            if ("push/picture".equals(parsed.getKind())) {
                customPictureChannel.send(message);
                return;
            }
            log.info("[cq-dock] custom mqtt unhandled topic: {}, payload: {}", topic, new String(payload));
        } catch (Exception e) {
            log.error("[cq-dock] custom mqtt dispatch failed, topic={}, err={}", topic, e.getMessage(), e);
        }
    }
}
