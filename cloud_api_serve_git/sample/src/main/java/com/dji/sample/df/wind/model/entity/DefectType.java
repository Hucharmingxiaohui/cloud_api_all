package com.dji.sample.df.wind.model.entity;

/**
 * 通用缺陷类型枚举类
 */
public enum DefectType {

    // 风电设备缺陷
    LIGHTNING_DAMAGE("雷击损伤"),
    GEL_COAT_PEELING("胶衣脱落"),
    COATING_BLISTER("胶漆鼓包"),
    HUB_OIL_LEAK("轮毂漏油"),
    BLADE_ICE_ACCUMULATION("叶片覆冰"),
    BLADE_CRACKING("叶片开裂"),
    BLADE_CORROSION("叶片腐蚀"),
    TOWER_CORROSION("塔筒腐蚀"),
    NO_DEFECT("无缺陷/无结果");

    private final String description;

    DefectType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static DefectType fromDescription(String description) {
        for (DefectType defect : DefectType.values()) {
            if (defect.description.equals(description)) {
                return defect;
            }
        }
        return null;
    }
}
