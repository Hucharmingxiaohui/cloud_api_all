package com.dji.sample.df.wind.model.entity;

/**
 * 缺陷类型枚举类(待定)
 */
public enum DefectType {
    /** 漏油缺陷 */
    OIL_LEAK("漏油"),

    /** 脏污缺陷 */
    DIRT("脏污"),

    /** 划痕缺陷 */
    SCRATCH("划痕"),

    /** 裂纹缺陷 */
    CRACK("裂纹"),

    /** 变形缺陷 */
    DEFORMATION("变形"),

    /** 气泡缺陷 */
    BUBBLE("气泡"),

    /** 缺失部件 */
    MISSING_PARTS("缺失部件"),

    /** 装配不良 */
    POOR_ASSEMBLY("装配不良"),

    /** 锈蚀缺陷 */
    RUST("锈蚀"),

    /** 尺寸不符 */
    DIMENSION_MISMATCH("尺寸不符"),

    /** 颜色异常 */
    COLOR_ABNORMALITY("颜色异常"),

    /** 表面不平整 */
    UNEVEN_SURFACE("表面不平整"),

    /** 毛刺缺陷 */
    BURR("毛刺"),

    /** 焊接缺陷 */
    WELDING_DEFECT("焊接缺陷"),

    /** 涂层剥落 */
    COATING_PEELING("涂层剥落"),

    /** 异物混入 */
    FOREIGN_MATTER("异物混入"),

    /** 标签错误 */
    LABEL_ERROR("标签错误"),

    /** 功能失效 */
    FUNCTIONAL_FAILURE("功能失效"),

    /** 电气短路 */
    ELECTRICAL_SHORT("电气短路"),

    /** 密封不良 */
    POOR_SEALING("密封不良");

    private final String description;

    /**
     * 构造函数
     * @param description 缺陷的中文描述
     */
    DefectType(String description) {
        this.description = description;
    }

    /**
     * 获取缺陷的中文描述
     * @return 中文描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据描述获取枚举值
     * @param description 中文描述
     * @return 对应的枚举值，找不到时返回null
     */
    public static DefectType fromDescription(String description) {
        for (DefectType defect : DefectType.values()) {
            if (defect.description.equals(description)) {
                return defect;
            }
        }
        return null;
    }

    /**
     * 判断是否为表面缺陷
     * @return 如果是表面缺陷返回true
     */
    public boolean isSurfaceDefect() {
        return this == DIRT || this == SCRATCH || this == RUST ||
                this == BUBBLE || this == COATING_PEELING ||
                this == UNEVEN_SURFACE || this == BURR;
    }

    /**
     * 判断是否为功能缺陷
     * @return 如果是功能缺陷返回true
     */
    public boolean isFunctionalDefect() {
        return this == OIL_LEAK || this == FUNCTIONAL_FAILURE ||
                this == ELECTRICAL_SHORT || this == POOR_SEALING;
    }

    /**
     * 判断是否为结构缺陷
     * @return 如果是结构缺陷返回true
     */
    public boolean isStructuralDefect() {
        return this == CRACK || this == DEFORMATION ||
                this == MISSING_PARTS || this == WELDING_DEFECT;
    }

    /**
     * 获取所有缺陷类型的中文描述数组
     * @return 中文描述数组
     */
    public static String[] getAllDescriptions() {
        DefectType[] values = values();
        String[] descriptions = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            descriptions[i] = values[i].getDescription();
        }
        return descriptions;
    }

    @Override
    public String toString() {
        return name() + "(" + description + ")";
    }
}
