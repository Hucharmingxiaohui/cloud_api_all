package com.dji.sdk.cloudapi.media;

import com.dji.sdk.exception.CloudSDKException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * @author sean
 * @version 1.7
 * @date 2023/6/27
 */
public enum StorageConfigGetModuleEnum {

    MEDIA(0),

    // 添加未知类型处理
    UNKNOWN(-1);

    private final int module;

    StorageConfigGetModuleEnum(int module) {
        this.module = module;
    }

    @JsonValue
    public int getModule() {
        return module;
    }

//    @JsonCreator
//    public StorageConfigGetModuleEnum find(int module) {
//        return Arrays.stream(values()).filter(moduleEnum -> moduleEnum.module == module).findAny()
//                .orElseThrow(() -> new CloudSDKException(StorageConfigGetModuleEnum.class, module));
//    }

    @JsonCreator
    public static StorageConfigGetModuleEnum find(int module) {
        return Arrays.stream(values())
                .filter(moduleEnum -> moduleEnum.module == module)
                .findAny()
                .orElse(UNKNOWN); // 返回 UNKNOWN 而不是抛出异常
    }

    // 可选：添加安全查找方法
    public static StorageConfigGetModuleEnum safeFind(Integer module) {
        if (module == null) {
            return UNKNOWN;
        }
        return find(module);
    }
}
