package com.cleaner.djuav.enums.kml;

/**
 * wpml:meteringMode		 负载测光模式
 */
public enum MeteringModeEnums {

    AVERAGE("average", "全局测光"),
    SPOT("spot", "点测光");

    private String value;
    private String description;

    MeteringModeEnums(String value, String description) {
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
