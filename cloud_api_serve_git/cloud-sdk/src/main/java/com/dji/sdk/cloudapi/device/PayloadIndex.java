package com.dji.sdk.cloudapi.device;

import com.dji.sdk.exception.CloudSDKErrorEnum;
import com.dji.sdk.exception.CloudSDKException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author sean
 * @version 1.7
 * @date 2023/6/29
 */
public class PayloadIndex {

    @NotNull
    private DeviceTypeEnum type;

    @NotNull
    private DeviceSubTypeEnum subType;

    @NotNull
    private PayloadPositionEnum position;

    public PayloadIndex() {
    }

    //    @JsonCreator
//    public PayloadIndex(String payloadIndex) {
//        Objects.requireNonNull(payloadIndex);
//        int[] payloadIndexArr = Arrays.stream(payloadIndex.split("-")).mapToInt(Integer::parseInt).toArray();
//        if (payloadIndexArr.length != 3) {
//            throw new CloudSDKException(CloudSDKErrorEnum.INVALID_PARAMETER);
//        }
//        this.type = DeviceTypeEnum.find(payloadIndexArr[0]);
//        this.subType = DeviceSubTypeEnum.find(payloadIndexArr[1]);
//        this.position = PayloadPositionEnum.find(payloadIndexArr[2]);
//    }
    @JsonCreator
    public PayloadIndex(String payloadIndex) {
        // 处理空值
        if (payloadIndex == null || payloadIndex.trim().isEmpty()) {
            setFirstAvailableValues();
            return;
        }

        try {
            Objects.requireNonNull(payloadIndex);
            int[] payloadIndexArr = Arrays.stream(payloadIndex.split("-")).mapToInt(Integer::parseInt).toArray();
            if (payloadIndexArr.length != 3) {
                setFirstAvailableValues();
                return;
            }

            // 安全地查找枚举值
            this.type = safeFind(DeviceTypeEnum.class, payloadIndexArr[0]);
            this.subType = safeFind(DeviceSubTypeEnum.class, payloadIndexArr[1]);
            this.position = safeFind(PayloadPositionEnum.class, payloadIndexArr[2]);

        } catch (Exception e) {
            // 如果出现任何异常，使用第一个可用的枚举值
            setFirstAvailableValues();
        }
    }

    // 安全查找枚举值的通用方法
    private <T extends Enum<T>> T safeFind(Class<T> enumClass, int value) {
        try {
            // 使用反射调用 find 方法
            java.lang.reflect.Method findMethod = enumClass.getMethod("find", int.class);
            return enumClass.cast(findMethod.invoke(null, value));
        } catch (Exception e) {
            // 如果查找失败，返回第一个枚举值
            return enumClass.getEnumConstants()[0];
        }
    }

    // 设置第一个可用的枚举值
    private void setFirstAvailableValues() {
        this.type = DeviceTypeEnum.values()[0];
        this.subType = DeviceSubTypeEnum.values()[0];
        this.position = PayloadPositionEnum.values()[0];
    }


    @Override
    @JsonValue
    public String toString() {
        return String.format("%s-%s-%s", type.getType(), subType.getSubType(), position.getPosition());
    }

    public DeviceTypeEnum getType() {
        return type;
    }

    public PayloadIndex setType(DeviceTypeEnum type) {
        this.type = type;
        return this;
    }

    public DeviceSubTypeEnum getSubType() {
        return subType;
    }

    public PayloadIndex setSubType(DeviceSubTypeEnum subType) {
        this.subType = subType;
        return this;
    }

    public PayloadPositionEnum getPosition() {
        return position;
    }

    public PayloadIndex setPosition(PayloadPositionEnum position) {
        this.position = position;
        return this;
    }
}
