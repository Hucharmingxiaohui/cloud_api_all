package com.dji.sdk.mqtt.osd;

import java.util.Optional;

/**
 * Raw OSD cache service for MQTT OSD payloads.
 */
public interface IOsdCacheService {

    void setDeviceRawOsd(String sn, Object data);

    <T> Optional<T> getDeviceRawOsd(String sn, Class<T> clazz);

    Boolean delDeviceRawOsd(String sn);
}
