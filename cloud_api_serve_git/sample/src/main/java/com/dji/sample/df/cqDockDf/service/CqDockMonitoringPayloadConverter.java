package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSONObject;
import com.dji.sample.df.cqDockDf.model.entity.CqDockUavMonitoringEntity;
import org.springframework.util.StringUtils;

public final class CqDockMonitoringPayloadConverter {

    private CqDockMonitoringPayloadConverter() {
    }

    public static void applyDroneOsd(CqDockUavMonitoringEntity entity, String topic, JSONObject payload, String rawData) {
        String[] parts = splitTopic(topic);
        entity.setUnitCode(firstText(part(parts, 1), entity.getUnitCode()));
        entity.setTopicDockSn(firstText(part(parts, 2), entity.getTopicDockSn()));
        entity.setTaskId(firstText(payload.getString("taskId"), entity.getTaskId()));
        entity.setDockSn(firstText(payload.getString("dockId"), entity.getDockSn(), entity.getTopicDockSn()));
        entity.setDroneSn(firstText(payload.getString("droneId"), entity.getDroneSn()));
        entity.setDroneMessageId(payload.getString("messageId"));
        entity.setDroneCurrTime(payload.getString("currTime"));
        entity.setDroneModeCode(nestedInt(payload, "osdDockDrone", "modeCode"));
        entity.setLatitude(nestedString(payload, "osdDockDrone", "latitude"));
        entity.setLongitude(nestedString(payload, "osdDockDrone", "longitude"));
        entity.setFlightHeight(nestedString(payload, "osdDockDrone", "height"));
        entity.setHorizontalSpeed(nestedString(payload, "osdDockDrone", "horizontalSpeed"));
        entity.setVerticalSpeed(nestedString(payload, "osdDockDrone", "verticalSpeed"));
        entity.setHomeDistance(nestedString(payload, "osdDockDrone", "homeDistance"));
        entity.setFlightDistance(nestedString(payload, "osdDockDrone", "totalFlightDistance"));
        entity.setTotalFlightTime(nestedString(payload, "osdDockDrone", "totalFlightTime"));
        entity.setBatteryLevel(nestedString(payload, "osdDockDrone", "battery", "capacityPercent"));
        entity.setCommunicationStatus(nestedString(payload, "osdDockDrone", "communicationStatus"));
        entity.setFaultAlarm(nestedString(payload, "osdDockDrone", "faultAlarm"));
        entity.setOperationStatus(resolveOperationStatus(entity.getDroneModeCode()));
        entity.setDroneRawData(rawData);
    }

    public static void applyDockOsd(CqDockUavMonitoringEntity entity, String topic, JSONObject payload, String rawData) {
        String[] parts = splitTopic(topic);
        entity.setUnitCode(firstText(part(parts, 1), entity.getUnitCode()));
        entity.setTopicDockSn(firstText(part(parts, 2), entity.getTopicDockSn()));
        entity.setTaskId(firstText(payload.getString("taskId"), entity.getTaskId()));
        entity.setDockSn(firstText(payload.getString("dockId"), entity.getDockSn(), entity.getTopicDockSn()));
        entity.setDockMessageId(payload.getString("messageId"));
        entity.setDockCurrTime(payload.getString("currTime"));
        entity.setDockModeCode(nestedInt(payload, "osdDock", "modeCode"));
        entity.setFlighttaskStepCode(nestedInt(payload, "osdDock", "flighttaskStepCode"));
        entity.setDroneInDock(nestedBoolean(payload, "osdDock", "droneInDock"));
        entity.setNestDoorStatus(resolveCoverState(nestedInt(payload, "osdDock", "coverState")));
        entity.setNestPlatformStatus(nestedString(payload, "osdDock", "nestPlatformStatus"));
        entity.setNestChargeStatus(nestedString(payload, "osdDock", "nestChargeStatus"));
        entity.setNestVoltage(nestedString(payload, "osdDock", "workingVoltage"));
        entity.setNestTemperature(nestedString(payload, "osdDock", "temperature"));
        entity.setNestHumidity(nestedString(payload, "osdDock", "humidity"));
        entity.setAmbientTemperature(nestedString(payload, "osdDock", "environmentTemperature"));
        entity.setWindSpeed(nestedString(payload, "osdDock", "windSpeed"));
        entity.setRainfall(resolveRainfall(nestedInt(payload, "osdDock", "rainfall")));
        entity.setDockRawData(rawData);
    }

    static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static String[] splitTopic(String topic) {
        return topic == null ? new String[0] : topic.split("/");
    }

    private static String part(String[] parts, int index) {
        return index >= 0 && index < parts.length ? parts[index] : null;
    }

    private static String nestedString(JSONObject root, String... path) {
        Object value = nested(root, path);
        return value == null ? null : String.valueOf(value);
    }

    private static Integer nestedInt(JSONObject root, String... path) {
        Object value = nested(root, path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null && StringUtils.hasText(String.valueOf(value))) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private static Boolean nestedBoolean(JSONObject root, String... path) {
        Object value = nested(root, path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value));
        }
        return null;
    }

    private static Object nested(JSONObject root, String... path) {
        Object cur = root;
        for (String key : path) {
            if (!(cur instanceof JSONObject)) {
                return null;
            }
            cur = ((JSONObject) cur).get(key);
            if (cur == null) {
                return null;
            }
        }
        return cur;
    }

    private static String resolveCoverState(Integer state) {
        if (state == null) {
            return null;
        }
        return (state == 2 || state == 3) ? "1" : String.valueOf(state);
    }

    private static String resolveRainfall(Integer rainfall) {
        if (rainfall == null) {
            return null;
        }
        switch (rainfall) {
            case 1:
                return "小雨";
            case 2:
                return "中雨";
            case 3:
                return "大雨";
            default:
                return String.valueOf(rainfall);
        }
    }

    private static String resolveOperationStatus(Integer modeCode) {
        if (modeCode == null) {
            return null;
        }
        return modeCode == 0 ? "1" : "2";
    }
}
