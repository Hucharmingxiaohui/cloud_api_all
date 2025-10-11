package com.cleaner.djuav.enums.kml;

/**
 * wpml:shootType 拍照模式（定时或定距）
 */
public enum ShootTypeEnums {

    TIME("time", "等时间拍照"),
    DISTANCE("distance", "等间隔拍照")
    ;

    private String value;
    private String description;

    ShootTypeEnums(String value, String description) {
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
