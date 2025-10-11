package com.cleaner.djuav.enums.kml;

/**
 * wpml:imageFormat		 图片格式列表
 */
public enum ImageFormatEnums {

    WIDE("wide", "存储广角镜头照片"),
    ZOOM("zoom", "存储变焦镜头照片"),
    IR("ir", "存储红外镜头照片"),
    NARROW_BAND("narrow_band", "存储窄带镜头拍摄照片"),
    VISABLE("visable", "可见光照片");

    private String value;
    private String description;

    ImageFormatEnums(String value, String description) {
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
