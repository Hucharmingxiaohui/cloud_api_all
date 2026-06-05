package com.dji.sample.component.mqtt.publish;

import com.dji.sdk.common.Common;
import com.dji.sdk.mqtt.ICustomMqttMessageGateway;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class CustomMqttGatewayPublish {

    private static final Logger log = LoggerFactory.getLogger(CustomMqttGatewayPublish.class);

    @Resource
    private ICustomMqttMessageGateway customMessageGateway;

    public void publish(String topic, byte[] payload) {
        customMessageGateway.publish(topic, payload);
    }

    public void publish(String topic, byte[] payload, int qos) {
        customMessageGateway.publish(topic, payload, qos);
    }

    public void publish(String topic, Object body) {
        try {
            byte[] payload = Common.getObjectMapper().writeValueAsBytes(body);
            customMessageGateway.publish(topic, payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to publish custom mqtt message. {}", body);
        }
    }

    public void publish(String topic, Object body, int qos) {
        try {
            byte[] payload = Common.getObjectMapper().writeValueAsBytes(body);
            customMessageGateway.publish(topic, payload, qos);
        } catch (JsonProcessingException e) {
            log.error("Failed to publish custom mqtt message. {}", body);
        }
    }
}
