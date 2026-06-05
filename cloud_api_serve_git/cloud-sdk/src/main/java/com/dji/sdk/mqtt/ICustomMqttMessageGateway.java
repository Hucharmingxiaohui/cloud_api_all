package com.dji.sdk.mqtt;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@MessagingGateway(defaultRequestChannel = ChannelName.CUSTOM_OUTBOUND)
public interface ICustomMqttMessageGateway {

    void publish(@Header(MqttHeaders.TOPIC) String topic, byte[] payload);

    void publish(@Header(MqttHeaders.TOPIC) String topic, byte[] payload, @Header(MqttHeaders.QOS) int qos);
}
