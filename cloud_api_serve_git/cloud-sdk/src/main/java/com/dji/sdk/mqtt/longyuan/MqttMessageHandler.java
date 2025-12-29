package com.dji.sdk.mqtt.longyuan;

/**
 * MQTT消息处理器接口
 */
public interface MqttMessageHandler {

    /**
     * 处理原始MQTT消息
     */
    void handleMessage(String topic, String payload);

    /**
     * 处理标准格式消息
     */
    default void handleStandardMessage(String topic, MqttStandardMessage message) {
        // 默认实现，子类可以重写
    }

    /**
     * 是否支持该主题
     */
    default boolean supports(String topic) {
        return true; // 默认支持所有主题
    }

    /**
     * 处理器名称
     */
    default String getHandlerName() {
        return this.getClass().getSimpleName();
    }
}
