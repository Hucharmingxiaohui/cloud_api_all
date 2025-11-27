package com.dji.sdk.cloudapi.livestream;

import com.dji.sdk.exception.CloudSDKException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * @author sean
 * @version 1.7
 * @date 2023/6/25
 */
public enum VideoTypeEnum {

    ZOOM("zoom"),

    WIDE("wide"),

    THERMAL("thermal"),

    NORMAL("normal"),

    IR("ir"),

    // 添加未知类型处理
    UNKNOWN("unknown");

    private final String type;

    VideoTypeEnum(String type) {
        this.type = type;
    }

    @JsonValue
    public String getType() {
        return type;
    }

//    @JsonCreator
//    public static VideoTypeEnum find(String videoType) {
//        return Arrays.stream(values()).filter(typeEnum -> typeEnum.type.equals(videoType)).findAny()
//                .orElseThrow(() -> new CloudSDKException(VideoTypeEnum.class , videoType));
//    }

    @JsonCreator
    public static VideoTypeEnum find(String videoType) {
        // 处理空值、空数组等情况
        if (videoType == null || videoType.trim().isEmpty() || "[]".equals(videoType)) {
            return UNKNOWN;
        }

        return Arrays.stream(values())
                .filter(typeEnum -> typeEnum.type.equals(videoType))
                .findAny()
                .orElse(UNKNOWN); // 返回 UNKNOWN 而不是抛出异常
    }

    // 可选：添加一个安全查找方法，不抛出异常
    public static VideoTypeEnum safeFind(String videoType) {
        try {
            return find(videoType);
        } catch (Exception e) {
            return UNKNOWN;
        }
    }
}
