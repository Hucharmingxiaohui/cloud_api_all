package com.cleaner.djuav.enums.kml;

/**
 * wpml:aircraftPathMode	 飞行器偏航角转动模式
 */
public enum AircraftPathModeEnums {

    CLOCKWISE("clockwise", "顺时针旋转"),
    COUNTER_CLOCKWISE("counterClockwise", "逆时针旋转");

    private String value;
    private String description;

    AircraftPathModeEnums(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
