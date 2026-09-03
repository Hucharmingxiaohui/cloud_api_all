package com.dji.sample.manage.service.impl;

import com.dji.sample.component.mqtt.model.EventsReceiver;
import com.dji.sample.component.redis.RedisConst;
import com.dji.sample.component.redis.RedisOpsUtils;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.service.ICapacityCameraService;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sdk.mqtt.osd.IOsdCacheService;
import com.dji.sdk.mqtt.state.IRawStateCacheService;
import com.dji.sdk.cloudapi.firmware.OtaProgress;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author sean
 * @version 1.4
 * @date 2023/3/21
 */
@Service
public class DeviceRedisServiceImpl implements IDeviceRedisService, IRawStateCacheService, IOsdCacheService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    @Autowired
    private ICapacityCameraService capacityCameraService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Boolean checkDeviceOnline(String sn) {
        String key = RedisConst.DEVICE_ONLINE_PREFIX + sn;
        return RedisOpsUtils.checkExist(key) && RedisOpsUtils.getExpire(key) > 0;
    }

    @Override
    public Optional<DeviceDTO> getDeviceOnline(String sn) {
        return Optional.ofNullable((DeviceDTO) RedisOpsUtils.get(RedisConst.DEVICE_ONLINE_PREFIX + sn));
    }

    @Override
    public void setDeviceOnline(DeviceDTO device) {
        RedisOpsUtils.setWithExpire(RedisConst.DEVICE_ONLINE_PREFIX + device.getDeviceSn(), device, RedisConst.DEVICE_ALIVE_SECOND);
    }

    @Override
    public Boolean delDeviceOnline(String sn) {
        return RedisOpsUtils.del(RedisConst.DEVICE_ONLINE_PREFIX + sn);
    }

    @Override
    public void setDeviceOsd(String sn, Object data) {
        RedisOpsUtils.setWithExpire(RedisConst.OSD_PREFIX + sn, data, RedisConst.DEVICE_ALIVE_SECOND);
    }

    @Override
    public <T> Optional<T> getDeviceOsd(String sn, Class<T> clazz) {
        return Optional.ofNullable(clazz.cast(RedisOpsUtils.get(RedisConst.OSD_PREFIX + sn)));
    }

    @Override
    public void setDeviceRawOsd(String sn, Object data) {
        mergeAndSetRawCache(RedisConst.RAW_OSD_PREFIX + sn, data);
    }

    @Override
    public <T> Optional<T> getDeviceRawOsd(String sn, Class<T> clazz) {
        return Optional.ofNullable(clazz.cast(RedisOpsUtils.get(RedisConst.RAW_OSD_PREFIX + sn)));
    }

    @Override
    public void setDeviceRawState(String sn, Object data) {
        mergeAndSetRawCache(RedisConst.RAW_STATE_PREFIX + sn, data);
    }

    @Override
    public <T> Optional<T> getDeviceRawState(String sn, Class<T> clazz) {
        return Optional.ofNullable(clazz.cast(RedisOpsUtils.get(RedisConst.RAW_STATE_PREFIX + sn)));
    }

    @Override
    public Boolean delDeviceOsd(String sn) {
        return RedisOpsUtils.del(RedisConst.OSD_PREFIX + sn);
    }

    @Override
    public Boolean delDeviceRawOsd(String sn) {
        return RedisOpsUtils.del(RedisConst.RAW_OSD_PREFIX + sn);
    }

    @Override
    public Boolean delDeviceRawState(String sn) {
        return RedisOpsUtils.del(RedisConst.RAW_STATE_PREFIX + sn);
    }

    private void mergeAndSetRawCache(String key, Object newValue) {
        Object oldValue = RedisOpsUtils.get(key);
        if (oldValue == null) {
            RedisOpsUtils.set(key, newValue);
            return;
        }
        Map<String, Object> oldMap = toMap(oldValue);
        Map<String, Object> newMap = toMap(newValue);
        if (oldMap == null || newMap == null) {
            RedisOpsUtils.set(key, newValue);
            return;
        }
        mergeTopicRequest(oldMap, newMap);
        RedisOpsUtils.set(key, objectMapper.convertValue(oldMap, newValue.getClass()));
    }

    private void mergeTopicRequest(Map<String, Object> oldMap, Map<String, Object> newMap) {
        Object oldData = oldMap.get("data");
        Object newData = newMap.get("data");
        Map<String, Object> oldDataMap = toMap(oldData);
        Map<String, Object> newDataMap = toMap(newData);
        oldMap.putAll(newMap);
        if (oldDataMap != null && newDataMap != null) {
            mergeMap(oldDataMap, newDataMap);
            oldMap.put("data", oldDataMap);
        }
    }

    private void mergeMap(Map<String, Object> target, Map<String, Object> source) {
        source.forEach((key, value) -> {
            Map<String, Object> targetValue = asMap(target.get(key));
            Map<String, Object> sourceValue = asMap(value);
            if (targetValue != null && sourceValue != null) {
                mergeMap(targetValue, sourceValue);
                target.put(key, targetValue);
                return;
            }
            target.put(key, value);
        });
    }

    private Map<String, Object> toMap(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) value);
        }
        return objectMapper.convertValue(value, MAP_TYPE);
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) value);
        }
        return null;
    }

    @Override
    public void setFirmwareUpgrading(String sn, EventsReceiver<OtaProgress> events) {
        RedisOpsUtils.setWithExpire(RedisConst.FIRMWARE_UPGRADING_PREFIX + sn, events, RedisConst.DEVICE_ALIVE_SECOND * 20);
    }

    @Override
    public Optional<EventsReceiver<OtaProgress>> getFirmwareUpgradingProgress(String sn) {
        return Optional.ofNullable((EventsReceiver<OtaProgress>) RedisOpsUtils.get(RedisConst.FIRMWARE_UPGRADING_PREFIX + sn));
    }

    @Override
    public Boolean delFirmwareUpgrading(String sn) {
        return RedisOpsUtils.del(RedisConst.FIRMWARE_UPGRADING_PREFIX + sn);
    }

    @Override
    public void addEndHmsKeys(String sn, String... keys) {
        RedisOpsUtils.listRPush(RedisConst.HMS_PREFIX + sn, keys);
    }

    @Override
    public Set<String> getAllHmsKeys(String sn) {
        return RedisOpsUtils.listGetAll(RedisConst.HMS_PREFIX + sn).stream()
                .map(String::valueOf).collect(Collectors.toSet());
    }

    @Override
    public Boolean delHmsKeysBySn(String sn) {
        return RedisOpsUtils.del(RedisConst.HMS_PREFIX + sn);
    }

    @Override
    public void gatewayOffline(String gatewaySn) {
        delDeviceOnline(gatewaySn);
        delDeviceOsd(gatewaySn);
        delHmsKeysBySn(gatewaySn);
        capacityCameraService.deleteCapacityCameraByDeviceSn(gatewaySn);
    }

    @Override
    public void subDeviceOffline(String deviceSn) {
        delDeviceOnline(deviceSn);
        delDeviceOsd(deviceSn);
        delHmsKeysBySn(deviceSn);
        capacityCameraService.deleteCapacityCameraByDeviceSn(deviceSn);
    }
}
