package com.cleaner.djuav.enums.kml;

/**
 * wpml:overlap	 采集类型
 */
public enum CollectionMethodEnums {

    ORTHO("ortho", "正射采集"),
    INCLINED("inclined", "倾斜采集")
    ;

    private String value;
    private String description;

    CollectionMethodEnums(String value, String description) {
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
