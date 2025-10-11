package com.dji.sample.center.v2022.enums;

/**
 * 国网规范点位设备类型对应表
 */
public enum DeviceTypeEnum {
    Youjin(1, "油浸式变压器(电抗器)")
    , Duanluqi(2, "断路器")
    , Zuhedianqi(3, "组合电器")
    , Gelikaiguan(4, "隔离开关")
    , Kaiguangui(5, "开关柜")
    , Dianliuhuganqi(6, "电流互感器")
    , Dianyahuganqi(7, "电压互感器")
    , Bileiqi(8, "避雷器")
    , Bingliandianrongqizu(9, "并联电容器组")
    , Ganshidiankangqi(10, "干式电抗器")
    , Chuanlianbuchangzhuangzhi(11, "串联补偿装置")
    , Muxianjijueyuanzi(12, "母线及绝缘子")
    , Chuanqiangtaoguan(13, "穿墙套管")
    , Xiaohuxianquan(14, "消弧线圈")
    , Gaopinzuboqi(15, "高频阻波器")
    , Ouhedianrongqi(16, "耦合电容器")
    , Gaoyarongduanqi(17, "高压熔断器")
    , Zhongxingdian(18, "中性点隔直(限直)装置")
    , Jiedi(19, "接地装置")
    , Duanzixiang(20, "端子箱及检修电源箱")
    , Zhanyongbianyaqi(21, "站用变压器")
    , Jiaoliu(22, "站用交流电源系统")
    , Zhiliu(23, "站用直流电源系统")
    , Shebeigoujia(24, "设备构架")
    , Fuzhusheshi(25, "辅助设施")
    , Tujian(26, "土建设施")
    , Bileizhen(27, "独立避雷针")
    , BileiqiDongzuo(28, "避雷器动作次数表")
    , Pingui(29, "二次屏柜")
    , Xiaofang(30, "消防系统")
    ;

    private Integer typeNum;
    private String des;

    DeviceTypeEnum(Integer typeNum, String des) {
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
