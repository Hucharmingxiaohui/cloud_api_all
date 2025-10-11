package com.cleaner.djuav.enums.kml;

/**
 * wpml:payloadPositionIndex	 负载挂载位置
 */
public enum PayloadPositionIndexEnums {

    ZERO(0, "飞行器1号挂载位置。M300 RTK，M350 RTK机型，对应机身左前方。其它机型，对应主云台"),
    ONE(1, "飞行器2号挂载位置。M300 RTK，M350 RTK机型，对应机身右前方"),
    TWO(2, "飞行器3号挂载位置。M300 RTK，M350 RTK机型，对应机身上方");

    private Integer value;
    private String description;

    PayloadPositionIndexEnums(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
