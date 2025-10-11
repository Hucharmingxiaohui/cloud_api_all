package com.dji.sample.center.v2022.enums;

/**
 * 国网规范点位设备类型对应表
 */
public enum RecognitionTypeEnum {
    Biaoji(1, "表计读取")
    , Weizhi(2, "位置状态识别")
    , Shebei(3, "设备外观查看")
    , Hongwai(4, "红外测温")
    , Sound(5, "声音检测")
    , Spark(6, "闪烁检测")
    , Chaoshengbo(7, "局放超声波检测")
    , Didianya(8, "局放地电压检测")
    , Tegaopin(9, "局放特高频检测")
    ;

    private Integer typeNum;
    private String des;

    RecognitionTypeEnum(Integer typeNum, String des) {
        this.typeNum = typeNum;
        this.des = des;
    }

    public Integer getTypeNum() {
        return typeNum;
    }

    public String getDes() {
        return des;
    }
}
