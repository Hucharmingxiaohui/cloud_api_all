package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 重庆电力下级平台 CUSTOM MQTT 上报解析（暂不落库，仅日志）
 */
@Slf4j
@Service
public class CqDockMqttMessageService {

    public void handleDroneOsd(String topic, String payload) {
        JSONObject json = parse(payload);
        String code = pathSegment(topic, 1);
        String sn = pathSegment(topic, 2);
        log.info("[cq-dock][drone/osd] topic={}, code={}, sn={}, taskId={}, dockId={}, droneId={}, messageId={}, currTime={}, modeCode={}, lat={}, lon={}, height={}, batteryPercent={}",
                topic, code, sn,
                json.getString("taskId"),
                json.getString("dockId"),
                json.getString("droneId"),
                json.getString("messageId"),
                json.getString("currTime"),
                nestedInt(json, "osdDockDrone", "modeCode"),
                nestedDouble(json, "osdDockDrone", "latitude"),
                nestedDouble(json, "osdDockDrone", "longitude"),
                nestedDouble(json, "osdDockDrone", "height"),
                nestedInt(json, "osdDockDrone", "battery", "capacityPercent"));
        log.info("[cq-dock][drone/osd] full payload={}", payload);
    }

    public void handleDockOsd(String topic, String payload) {
        JSONObject json = parse(payload);
        String code = pathSegment(topic, 1);
        String sn = pathSegment(topic, 2);
        log.info("[cq-dock][dock/osd] topic={}, code={}, sn={}, taskId={}, dockId={}, messageId={}, currTime={}, modeCode={}, flighttaskStepCode={}, droneInDock={}, lat={}, lon={}, temp={}, humidity={}",
                topic, code, sn,
                json.getString("taskId"),
                json.getString("dockId"),
                json.getString("messageId"),
                json.getString("currTime"),
                nestedInt(json, "osdDock", "modeCode"),
                nestedInt(json, "osdDock", "flighttaskStepCode"),
                nestedBoolean(json, "osdDock", "droneInDock"),
                nestedDouble(json, "osdDock", "latitude"),
                nestedDouble(json, "osdDock", "longitude"),
                nestedDouble(json, "osdDock", "temperature"),
                nestedDouble(json, "osdDock", "humidity"));
        log.info("[cq-dock][dock/osd] full payload={}", payload);
    }

    public void handleHms(String topic, String payload) {
        JSONObject json = parse(payload);
        String code = pathSegment(topic, 1);
        String sn = pathSegment(topic, 2);
        log.info("[cq-dock][push/hms] topic={}, code={}, sn={}, messageId={}, dockSn={}, uavSn={}, gateWayType={}, codeField={}, level={}, module={}, imminent={}, inTheSky={}, message={}",
                topic, code, sn,
                json.getString("messageId"),
                json.getString("dockSn"),
                json.getString("uavSn"),
                json.getString("gateWayType"),
                json.getString("code"),
                json.getInteger("level"),
                json.getInteger("module"),
                json.getBoolean("imminent"),
                json.getBoolean("inTheSky"),
                json.getString("message"));
        log.info("[cq-dock][push/hms] full payload={}", payload);
    }

    public void handlePicture(String topic, String payload) {
        JSONObject json = parse(payload);
        String code = pathSegment(topic, 1);
        log.info("[cq-dock][push/picture] topic={}, code={}, id={}, messageId={}, taskId={}, pictureName={}, pictureUrl={}, totalNum={}",
                topic, code,
                json.getString("id"),
                json.getString("messageId"),
                json.getString("taskId"),
                json.getString("pictureName"),
                json.getString("pictureUrl"),
                json.getString("totalNum"));
        log.info("[cq-dock][push/picture] full payload={}", payload);
    }

    private JSONObject parse(String payload) {
        try {
            if (payload == null || payload.isEmpty()) {
                return new JSONObject();
            }
            return JSON.parseObject(payload);
        } catch (Exception e) {
            log.error("[cq-dock] parse payload failed: {}", e.getMessage());
            return new JSONObject();
        }
    }

    /**
     * topic 以 / 开头时 index 从 1 起： /{code}/{sn}/...
     */
    private String pathSegment(String topic, int index) {
        if (topic == null || topic.isEmpty()) {
            return null;
        }
        String[] parts = topic.split("/");
        if (index >= 0 && index < parts.length) {
            return parts[index];
        }
        return null;
    }

    private Integer nestedInt(JSONObject root, String... path) {
        Object v = nested(root, path);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return null;
    }

    private Double nestedDouble(JSONObject root, String... path) {
        Object v = nested(root, path);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return null;
    }

    private Boolean nestedBoolean(JSONObject root, String... path) {
        Object v = nested(root, path);
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        return null;
    }

    private Object nested(JSONObject root, String... path) {
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
}
