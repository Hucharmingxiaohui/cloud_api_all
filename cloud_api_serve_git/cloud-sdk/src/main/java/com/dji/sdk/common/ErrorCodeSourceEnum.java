
package com.dji.sdk.common;

import com.dji.sdk.exception.CloudSDKException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * @author sean.zhou
 * @version 0.1
 * @date 2021/11/25
 */
public enum ErrorCodeSourceEnum {

    DEVICE(3),

    DOCK(5),

    PILOT(6),

    // 添加未知错误码来源
    UNKNOWN(0);

    private final int source;

    ErrorCodeSourceEnum(int source) {
        this.source = source;
    }

    @JsonValue
    public int getSource() {
        return source;
    }

    //    @JsonCreator
//    public static ErrorCodeSourceEnum find(int source) {
//        return Arrays.stream(values()).filter(error -> error.source == source).findAny()
//                .orElseThrow(() -> new CloudSDKException(ErrorCodeSourceEnum.class, source));
//    }
    @JsonCreator
    public static ErrorCodeSourceEnum find(int source) {
        return Arrays.stream(values())
                .filter(error -> error.source == source)
                .findAny()
                .orElse(UNKNOWN); // 返回 UNKNOWN 而不是抛出异常
    }

    // 可选：添加安全查找方法
    public static ErrorCodeSourceEnum safeFind(Integer source) {
        if (source == null) {
            return UNKNOWN;
        }
        return find(source);
    }
}
