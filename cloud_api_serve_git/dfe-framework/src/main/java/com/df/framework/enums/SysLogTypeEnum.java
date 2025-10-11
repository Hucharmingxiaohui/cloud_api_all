package com.df.framework.enums;

/**
 * 操作日志类型
 *
 * @date 2021/12/30 9:36
 */
public enum SysLogTypeEnum {

    USER(1, "用户管理"),

    AUTH(2, "权限管理"),

    SYS(3, "系统管理"),

    LOG(4, "日志审计");


    private int type;
    private String des;

    SysLogTypeEnum(int type, String des) {
        this.type = type;
        this.des = des;
    }

    public int getType() {
        return type;
    }

    public String getDes() {
        return des;
    }
}
