package com.cleaner.djuav.enums.kml;

/**
 * wpml:positioningType	 经纬度与高度数据源
 */
public enum PositioningTypeEnums {

    GPS("GPS", "位置数据采集来源为GPS/BDS/GLONASS/GALILEO等"),
    RTK_BASE_STATION("RTKBaseStation", "采集位置数据时，使用RTK基站进行差分定位"),
    QIAN_XUN("QianXun", "采集位置数据时，使用千寻网络RTK进行差分定位"),
    CUSTOM("Custom", "采集位置数据时，使用自定义网络RTK进行差分定位");

    private String value;
    private String description;

    PositioningTypeEnums(String value, String description) {
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
