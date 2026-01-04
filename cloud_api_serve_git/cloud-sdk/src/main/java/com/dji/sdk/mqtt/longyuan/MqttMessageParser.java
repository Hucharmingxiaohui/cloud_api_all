package com.dji.sdk.mqtt.longyuan;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
public class MqttMessageParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析JSON字符串为标准消息对象
     */
    public static MqttStandardMessage parse(String json) throws Exception {
        Map<String, Object> map = objectMapper.readValue(json,
                new TypeReference<Map<String, Object>>() {});

        MqttStandardMessage message = new MqttStandardMessage();
        message.setMessageId((String) map.get("messageId"));
        message.setTimestamp((String) map.get("timestamp"));
        message.setSender((String) map.get("sender"));
        message.setStationCode((String) map.get("stationCode"));
        message.setCategory((String) map.get("category"));
        message.setAction((String) map.get("action"));

        // 处理data字段
        Object dataObj = map.get("data");
        if (dataObj instanceof Map) {
            message.setData((Map<String, Object>) dataObj);
        } else if (dataObj != null) {
            // 如果data不是Map，尝试转换
            String dataJson = objectMapper.writeValueAsString(dataObj);
            Map<String, Object> dataMap = objectMapper.readValue(dataJson,
                    new TypeReference<Map<String, Object>>() {});
            message.setData(dataMap);
        }

        return message;
    }

    /**
     * 安全解析，解析失败返回null
     */
    public static MqttStandardMessage parseSafe(String json) {
        try {
            return parse(json);
        } catch (Exception e) {
            log.error("解析MQTT消息失败: {}", e.getMessage());
            return null;
        }
    }
}
