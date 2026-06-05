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

    @Resource(name = ChannelName.INBOUND_CUSTOM_DRONE_OSD)
    private MessageChannel customDroneOsdChannel;

    @ServiceActivator(inputChannel = ChannelName.CUSTOM_INBOUND)
    public void handleMessage(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        byte[] payload = (byte[]) message.getPayload();

        if (topic != null && topic.matches("/[^/]+/[A-Za-z0-9]+/drone/osd")) {
            customDroneOsdChannel.send(message);
            return;
        }

        log.debug("Custom mqtt received - topic: {}, payload: {}", topic, new String(payload));
    }
}
