package com.cleaner.djuav.enums.kml;

/**
 * wpml:overlap	 采集类型
 */
public enum LensTypeEnums {

    LIDAR("lidar", "激光"),
    CAMERA("camera", "可见光")
    ;

    private String value;
    private String description;

    LensTypeEnums(String value, String description) {
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
