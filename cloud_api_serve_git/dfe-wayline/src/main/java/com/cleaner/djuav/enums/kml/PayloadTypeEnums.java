package com.cleaner.djuav.enums.kml;

/**
 * wpml:payloadEnumValue	 负载机型主类型
 */
public enum PayloadTypeEnums {

    H20(42, "H20"),
    H20T(43, "H20T"),
    M30(52, "M30双光相机"),
    M30T(53, "M30T三光相机"),
    H20N(61, "H20N"),
    MAVIC_3E(66, "Mavic 3E 相机"),
    MAVIC_3T(67, "Mavic 3T 相机"),
    MAVIC_3M(68, "Mavic 3M 相机"),
    MATRICE_3D(80, "Matrice 3D 相机"),
    MATRICE_3TD(81, "Matrice 3TD 相机"),
    PSDK(65534, "PSDK 负载"),
    H30(82, "H30"),
    H30T(83, "H30T");

    private Integer value;
    private String description;

    PayloadTypeEnums(Integer value, String description) {
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
