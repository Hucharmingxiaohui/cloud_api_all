package com.dji.sdk.cloudapi.device;

import com.dji.sdk.exception.CloudSDKException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * @author sean
 * @version 1.7
 * @date 2023/6/30
 */
public enum AirConditionerStateEnum {

    IDLE(0),

    COOL(1),

    HEAT(2),

    DEHUMIDIFICATION(3),

    COOLING_EXIT(4),

    HEATING_EXIT(5),

    DEHUMIDIFICATION_EXIT(6),

    COOLING_PREPARATION(7),

    HEATING_PREPARATION(8),

    DEHUMIDIFICATION_PREPARATION(9),
    AIR_COOLING_PREPARING(10),//
    AIR_COOLING(11), //风冷中
    AIR_COOLING_EXITING(12),//风冷退出中
    DEFOGGING_PREPARING(13),//除雾准备中
    DEFOGGING(14),//除雾中
    DEFOGGING_EXITING(15),//除雾退出中
    DISCONNECTED(32767),
    ;

    private final int state;

    AirConditionerStateEnum(int state) {
        this.state = state;
    }

    @JsonValue
    public int getState() {
        return state;
    }

    @JsonCreator
    public static AirConditionerStateEnum find(int state) {
        return Arrays.stream(values()).filter(stateEnum -> stateEnum.state == state).findAny()
            .orElseThrow(() -> new CloudSDKException(AirConditionerStateEnum.class, state));
    }

}
