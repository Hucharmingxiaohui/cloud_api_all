package com.df.framework.enums;

/**
 * 操作日志类型
 *
 * @date 2021/12/30 9:36
 */
public enum OperLogTypeEnum {

    OTHER(0, "其他"),

    ADD(2, "新增"),

    UPDATE(3, "修改"),

    DELETE(4, "删除"),

    SELECT(1, "查询"),

    CONTROL(5, "设备控制");


    private int type;
    private String des;

    OperLogTypeEnum(int type, String des) {
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
