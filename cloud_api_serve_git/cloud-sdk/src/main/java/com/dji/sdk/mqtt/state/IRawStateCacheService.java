package com.dji.sdk.mqtt.state;

import java.util.Optional;

/**
 * Raw state cache service for MQTT state payloads.
 */
public interface IRawStateCacheService {

    void setDeviceRawState(String sn, Object data);

    <T> Optional<T> getDeviceRawState(String sn, Class<T> clazz);

    Boolean delDeviceRawState(String sn);
}
