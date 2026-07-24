package com.dji.sample.component.mqtt.handler;

import com.dji.sample.df.cqDockDf.service.CqDockMqttMessageService;
import com.dji.sdk.mqtt.ChannelName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class CustomPictureHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomPictureHandler.class);

    @Resource
    private CqDockMqttMessageService cqDockMqttMessageService;

    @ServiceActivator(inputChannel = ChannelName.INBOUND_CUSTOM_PICTURE)
    public void handleMessage(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        byte[] payload = (byte[]) message.getPayload();
        try {
            cqDockMqttMessageService.handlePicture(topic, new String(payload));
        } catch (Exception e) {
            log.error("[cq-dock] handle picture failed, topic={}, err={}", topic, e.getMessage(), e);
        }
    }
}
