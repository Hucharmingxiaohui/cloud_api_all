package com.cleaner.djuav.enums.kml;

/**
 * wpml:executeHeightMode 执行高度模式
 * 该元素仅在waylines.wpml中使用。
 */
public enum ExecuteHeightModeEnums {

    WGS84("WGS84", "椭球高模式"),
    RELATIVE_TO_START_POINT("relativeToStartPoint", "相对起飞点高度模式"),
    REALTIME_FOLLOW_SURFACE("realTimeFollowSurface", "使用实时仿地模式，仅支持M3E/M3T/M3M");

    private String value;
    private String description;

    ExecuteHeightModeEnums(String value, String description) {
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
