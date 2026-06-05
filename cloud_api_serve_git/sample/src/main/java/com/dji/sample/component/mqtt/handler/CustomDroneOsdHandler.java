package com.dji.sample.component.mqtt.handler;

import com.dji.sdk.mqtt.ChannelName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class CustomDroneOsdHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomDroneOsdHandler.class);

    @ServiceActivator(inputChannel = ChannelName.INBOUND_CUSTOM_DRONE_OSD)
    public void handleMessage(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        byte[] payload = (byte[]) message.getPayload();
        log.info("Custom drone OSD - topic: {}, payload: {}", topic, new String(payload));
    }
}
