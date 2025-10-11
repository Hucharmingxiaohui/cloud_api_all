package com.df.framework.enums;

/**
 * 告警级别
 */
public enum AlarmLevelEnum {
    YJ(1, "预警"),
    YB(2, "一般"),
    YZ(3, "严重"),
    WJ(4, "危急");

    private int type;
    private String desc;

    AlarmLevelEnum(int type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public int getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public boolean is(int type) {
        return type == this.type;
    }

    public static String getDesc(int type) {
        String ret = "未知类型";
        for (AlarmLevelEnum item : AlarmLevelEnum.values()) {
            if (item.type == type) {
                return item.desc;
            }
        }
        return ret;
    }
}
