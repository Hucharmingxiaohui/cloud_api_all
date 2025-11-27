package com.dji.sdk.mqtt.state;

import com.dji.sdk.cloudapi.device.*;
import com.dji.sdk.cloudapi.livestream.DockLivestreamAbilityUpdate;
import com.dji.sdk.cloudapi.property.DockDroneCommanderFlightHeight;
import com.dji.sdk.cloudapi.property.DockDroneCommanderModeLostAction;
import com.dji.sdk.cloudapi.property.DockDroneOfflineMapEnable;
import com.dji.sdk.cloudapi.property.DockDroneRthMode;
import com.dji.sdk.exception.CloudSDKException;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.*;

/**
 *
 * @author sean.zhou
 * @date 2021/11/18
 * @version 0.1
 */
public enum DockStateDataKeyEnum {

    FIRMWARE_VERSION(Set.of("firmware_version"), DockFirmwareVersion.class),

    LIVE_CAPACITY(Set.of("live_capacity"), DockLivestreamAbilityUpdate.class),

    CONTROL_SOURCE(Set.of("control_source"), DockDroneControlSource.class),

    LIVE_STATUS(Set.of("live_status"), DockLiveStatus.class),

    WPMZ_VERSION(Set.of("wpmz_version"), DockDroneWpmzVersion.class),

    THERMAL_SUPPORTED_PALETTE_STYLE(PayloadModelConst.getAllIndexWithPosition(), DockDroneThermalSupportedPaletteStyle.class),

    RTH_MODE(Set.of("rth_mode"), DockDroneRthMode.class),

    CURRENT_RTH_MODE(Set.of("current_rth_mode"), DockDroneCurrentRthMode.class),

    COMMANDER_MODE_LOST_ACTION(Set.of("commander_mode_lost_action"), DockDroneCommanderModeLostAction.class),

    CURRENT_COMMANDER_FLIGHT_MODE(Set.of("current_commander_flight_mode"), DockDroneCurrentCommanderFlightMode.class),

    COMMANDER_FLIGHT_HEIGHT(Set.of("commander_flight_height"), DockDroneCommanderFlightHeight.class),

    MODE_CODE_REASON(Set.of("mode_code_reason"), DockDroneModeCodeReason.class),

    OFFLINE_MAP_ENABLE(Set.of("offline_map_enable"), DockDroneOfflineMapEnable.class),

    DONGLE_INFOS(Set.of("dongle_infos"), DongleInfos.class),

    SILENT_MODE(Set.of("silent_mode"), DockSilentMode.class),

    // 在 UNKNOWN 枚举值中添加所有遇到的未知数据键
    UNKNOWN(Set.of(
            "unknown",
            "wireless_link_topo",
            "camera_watermark_settings",
            "flysafe_database_version",
            "commander_flight_mode",
            "remaining_power_for_return_home",
            "payloads",
            "geo_caging_status",
            "is_beidou_version",
            "ai_model_list",
            "uom_real_name_state",
            "psdk_ui_resource",
            "psdk_widget_values"
    ), Object.class);
    ;

    private final Set<String> keys;

    private final Class classType;


    DockStateDataKeyEnum(Set<String> keys, Class classType) {
        this.keys = keys;
        this.classType = classType;
    }

    public Class getClassType() {
        return classType;
    }

    public Set<String> getKeys() {
        return keys;
    }

//    public static DockStateDataKeyEnum find(Set<String> keys) {
//        return Arrays.stream(values()).filter(keyEnum -> !Collections.disjoint(keys, keyEnum.keys)).findAny()
//                .orElseThrow(() -> new CloudSDKException(DockStateDataKeyEnum.class, keys));
//    }

    public static DockStateDataKeyEnum find(Set<String> keys) {
        return Arrays.stream(values())
                .filter(keyEnum -> !Collections.disjoint(keys, keyEnum.keys))
                .findAny()
                .orElse(UNKNOWN);
    }

    // 添加更健壮的 Jackson 反序列化方法
    @JsonCreator
    public static DockStateDataKeyEnum fromValue(Object value) {
        if (value == null) {
            return UNKNOWN;
        }

        Set<String> keySet = extractKeys(value);

        // 如果 keySet 为空或者是未知的键，直接返回 UNKNOWN
        if (keySet.isEmpty() || UNKNOWN.keys.stream().anyMatch(keySet::contains)) {
            return UNKNOWN;
        }

        return find(keySet);
    }

    private static Set<String> extractKeys(Object value) {
        Set<String> keySet = new HashSet<>();

        if (value == null) {
            return keySet;
        }

        try {
            // 处理 Collection 类型
            if (value instanceof Collection) {
                ((Collection<?>) value).stream()
                        .filter(item -> item instanceof String)
                        .forEach(item -> keySet.add((String) item));
            }
            // 处理字符串类型
            else if (value instanceof String) {
                keySet.add((String) value);
            }
            // 处理数组类型
            else if (value.getClass().isArray()) {
                Arrays.stream((Object[]) value)
                        .filter(item -> item instanceof String)
                        .forEach(item -> keySet.add((String) item));
            }
        } catch (Exception e) {
            // 如果解析过程中出现任何异常，返回空集合
            return new HashSet<>();
        }

        return keySet;
    }
}
