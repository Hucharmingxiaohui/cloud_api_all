package com.dji.sample.df.wind.mqtt;

import com.dji.sdk.mqtt.IMqttMessageGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class MqttMessageSender {

    @Resource
    private IMqttMessageGateway mqttGateway;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 发送消息到 /patrol/data/SubToLyGroup/guangxi_weilan
     */
    public void sendToPatrolData(Map<String, Object> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);

            // 使用你的网关发送，QoS=1
            mqttGateway.publish("/patrol/data/SubToLyGroup/guangxi_weilan", payload, 1);

            log.debug("发送消息成功: messageId={}, action={}",
                    message.get("messageId"), message.get("action"));

        } catch (Exception e) {
            log.error("发送MQTT消息失败", e);
            throw new RuntimeException("MQTT发送失败", e);
        }
    }

    /**
     * 发送JSON字符串
     */
    public void sendJson(String topic, String json) {
        try {
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);
            mqttGateway.publish(topic, payload, 1);
        } catch (Exception e) {
            log.error("发送JSON失败", e);
        }
    }
}
