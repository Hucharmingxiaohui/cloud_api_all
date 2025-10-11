package com.cleaner.djuav.enums.kml;

/**
 * wpml:flyToWaylineMode 飞向首航点模式
 */
public enum FlyToWaylineModeEnums {

    SAFELY("safely", "安全模式"),
    POINT_TO_POINT("pointToPoint", "倾斜飞行模式");

    private String value;
    private String description;

    FlyToWaylineModeEnums(String value, String description) {
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
