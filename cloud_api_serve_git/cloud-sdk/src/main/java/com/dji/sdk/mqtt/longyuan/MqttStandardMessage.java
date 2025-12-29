package com.dji.sdk.mqtt.longyuan;

import lombok.Data;
import java.util.Map;

/**
 * MQTT标准消息格式
 */
@Data
public class MqttStandardMessage {
    /**
     * 消息唯一标识
     */
    private String messageId;

    /**
     * 消息时间戳
     */
    private String timestamp;

    /**
     * 发送方标识
     */
    private String sender;

    /**
     * 站点编码
     */
    private String stationCode;

    /**
     * 消息类别
     */
    private String category;

    /**
     * 消息动作
     */
    private String action;

    /**
     * 业务数据
     */
    private Map<String, Object> data;
}
