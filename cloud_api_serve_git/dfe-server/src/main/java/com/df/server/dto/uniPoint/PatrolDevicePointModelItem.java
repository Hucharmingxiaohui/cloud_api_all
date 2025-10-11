package com.df.server.dto.uniPoint;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * 设备点位模型文件内容。见规范B.2.3
 */
@XmlRootElement(name = "Item")
public class PatrolDevicePointModelItem {
    /**
     * 变电站名称
     */
    private String station_name;
    /**
     * 变电站编码（和中台对应资源 ID 保持一致）
     */
    private String station_code;
    /**
     * 区域ID（和中台对应资源 ID 保持一致）
     */
    private String area_id;
    /**
     * 区域全名
     */
    private String area_name;
    /**
     * 设备点位 ID
     */
    private String device_id;
    /**
     * 设备点位名称
     */
    private String device_name;
    /**
     * 部件 ID（和中台对应资源 ID 保持一致）
     */
    private String component_id;
    /**
     * 部件名称
     */
    private String component_name;
    /**
     * 间隔 ID（和中台对应资源 ID 保持一致）
     */
    private String bay_id;
    /**
     * 间隔名称 （间隔全名）
     */
    private String bay_name;
    /**
     * 主设备 ID （和中台对应资源 ID 保持一致）
     */
    private String main_device_id;
    /**
     * 主设备名称（主设备全名）
     */
    private String main_device_name;
    /**
     * 主设备类型
     * <1>:=油浸式变压器（电抗器）
     * <2>:=断路器
     * <3>:=组合电器
     * <4>:=隔离开关
     * <5>:=开关柜
     * <6>:=电流互感器
     * <7>:=电压互感器
     * 25
     * <8>:=避雷器
     * <9>:=并联电容器组
     * <10>:=干式电抗器
     * <11>:=串联补偿装置
     * <12>:=母线及绝缘子
     * <13>:=穿墙套管
     * <14>:=消弧线圈
     * <15>:=高频阻波器
     * <16>:=耦合电容器
     * <17>:=高压熔断器
     * <18>:=中性点隔直（限直）装置
     * <19>:=接地装置
     * <20>:=端子箱及检修电源箱
     * <21>:=站用变压器
     * <22>:=站用交流电源系统
     * <23>:=站用直流电源系统
     * <24>:=设备构架
     * <25>:=辅助设施
     * <26>:=土建设施
     * <27>:=独立避雷针
     * <28>:=电力电缆
     * <29>:=二次屏柜
     * <30>:=消防系统
     */
    private String device_type;
    /**
     * 表计类型
     * <1>:=油位表
     * <2>:=避雷器动作次数表
     * <3>:=泄漏电流表
     * <4>:=SF 6 压力表
     * <5>:=液压表
     * <6>:=开关动作次数表
     * <7>:=油温表
     * <8>:=档位表
     * <9>:=气压表
     */
    private String meter_type;
    /**
     * 辅助设施类型
     * <1>: =电子围栏
     * <2>:=红外对射
     * <3>:=泡沫喷淋
     * <4>:=消防水泵
     * <5>:=消防栓
     * <6>:=消防室
     * <7>:=设备室
     * <8>:=照明灯
     * <9>:=摄像头
     * <10>:=水位线
     * <11>:=排水泵
     * <12>:=沉降监测点
     */
    private String appearance_type;
    /**
     * 采集/保存文件类型列表（格式：多个采集文件类型，采用“,” 分隔）
     */
    private String save_type_list;
    /**
     * 识别类型列表（格式 ：多个识别类型，采用“,”分隔）
     */
    private String recognition_type_list;
    /**
     * 相位
     * <1>:=A 相
     * <2>:=B 相
     * <3>:=C 相
     * 多个相位，采用“,”分隔
     */
    private String phase;
    /**
     * 备注信息（用于描述设备点位的文字信息）
     */
    private String device_info;
    /**
     * 设备点位支持的数据来源
     * <p>
     * 按位定义数据来源，支持 32 位
     * 1 为有效， 0 为无效，如下：
     * 第 0 位：摄像机
     * 第 1 位：机器人
     * 第 2 位：无人机
     * 第 3 位：声纹
     * 第 4 位：在线监测
     * 区域巡视主机上报上级系统填写。
     */
    private int data_type;
    /**
     * 正常范围下限（区域巡视主机上报上级系统填写（选填））
     */
    private String lower_value;
    /**
     * 正常范围上限 （区域巡视主机上报上级系统填写（选填））
     */
    private String upper_value;
    /**
     * 关联视频编码及预置位
     * <p>
     * Json 格式：[{"device_code":"视频编码
     * ","device_pos":" 视 频 预 置 位 号
     * ","robot_code":" 机 器 人 编 码
     * ","robot_pos":" 机 器 人 预 置 位 号
     * ","uav_code":"无人机编码","uav_pos":"
     * 无人机预置位号"}]，若某项不存在，则内
     * 容填空。
     */
    private String video_pos;

    /**
     * 点位重要等级
     */
    private Integer level;

    /**
     * 点位重要等级
     */
    private Integer point_type;

    /**
     * 设备类型大类(1:一次设备，2：二次设备，3：辅助设备，4：部件附件）
     */
    private Integer device_main_type;
    /**
     * 点位是否已设置（0：未设置，1：已设置）（统计点位覆盖率用）
     */
    private Integer is_set;

    /**
     * 静默监视频次（每5分钟：5，每小时：60）
     */
    private Integer monitor_times;

    /**
     * 是否一键顺控点位（0：否，1：是）
     */
    private Integer is_one_key_point;

    /**
     * 是否守望点位（0：否，1：是）
     */
    private Integer is_keep_watch_point;

    /**
     * 标签属性（1：人工关注）
     */
    private String label_attri;

    @XmlAttribute(name = "point_type")
    public Integer getPoint_type() {
        return point_type;
    }

    public void setPoint_type(Integer point_type) {
        this.point_type = point_type;
    }

    @XmlAttribute(name = "label_attri")
    public String getLabel_attri() {
        return label_attri;
    }

    public void setLabel_attri(String label_attri) {
        this.label_attri = label_attri;
    }

    @XmlAttribute(name = "device_main_type")
    public Integer getDevice_main_type() {
        return device_main_type;
    }

    public void setDevice_main_type(Integer device_main_type) {
        this.device_main_type = device_main_type;
    }

    @XmlAttribute(name = "is_set")
    public Integer getIs_set() {
        return is_set;
    }

    public void setIs_set(Integer is_set) {
        this.is_set = is_set;
    }

    @XmlAttribute(name = "monitor_times")
    public Integer getMonitor_times() {
        return monitor_times;
    }

    public void setMonitor_times(Integer monitor_times) {
        this.monitor_times = monitor_times;
    }

    @XmlAttribute(name = "is_one_key_point")
    public Integer getIs_one_key_point() {
        return is_one_key_point;
    }

    public void setIs_one_key_point(Integer is_one_key_point) {
        this.is_one_key_point = is_one_key_point;
    }

    @XmlAttribute(name = "is_keep_watch_point")
    public Integer getIs_keep_watch_point() {
        return is_keep_watch_point;
    }

    public void setIs_keep_watch_point(Integer is_keep_watch_point) {
        this.is_keep_watch_point = is_keep_watch_point;
    }

    @XmlAttribute(name = "level")
    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    @XmlAttribute(name = "station_code")
    public String getStation_code() {
        return station_code;
    }

    public void setStation_code(String station_code) {
        this.station_code = station_code;
    }

    @XmlAttribute(name = "station_name")
    public String getStation_name() {
        return station_name;
    }

    public void setStation_name(String station_name) {
        this.station_name = station_name;
    }

    @XmlAttribute(name = "area_id")
    public String getArea_id() {
        return area_id;
    }

    public void setArea_id(String area_id) {
        this.area_id = area_id;
    }

    @XmlAttribute(name = "area_name")
    public String getArea_name() {
        return area_name;
    }

    public void setArea_name(String area_name) {
        this.area_name = area_name;
    }

    @XmlAttribute(name = "device_id")
    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    @XmlAttribute(name = "device_name")
    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    @XmlAttribute(name = "component_id")
    public String getComponent_id() {
        return component_id;
    }

    public void setComponent_id(String component_id) {
        this.component_id = component_id;
    }

    @XmlAttribute(name = "component_name")
    public String getComponent_name() {
        return component_name;
    }

    public void setComponent_name(String component_name) {
        this.component_name = component_name;
    }

    @XmlAttribute(name = "bay_id")
    public String getBay_id() {
        return bay_id;
    }

    public void setBay_id(String bay_id) {
        this.bay_id = bay_id;
    }

    @XmlAttribute(name = "bay_name")
    public String getBay_name() {
        return bay_name;
    }

    public void setBay_name(String bay_name) {
        this.bay_name = bay_name;
    }

    @XmlAttribute(name = "main_device_id")
    public String getMain_device_id() {
        return main_device_id;
    }

    public void setMain_device_id(String main_device_id) {
        this.main_device_id = main_device_id;
    }

    @XmlAttribute(name = "main_device_name")
    public String getMain_device_name() {
        return main_device_name;
    }

    public void setMain_device_name(String main_device_name) {
        this.main_device_name = main_device_name;
    }

    @XmlAttribute(name = "device_type")
    public String getDevice_type() {
        return device_type;
    }

    public void setDevice_type(String device_type) {
        this.device_type = device_type;
    }

    @XmlAttribute(name = "meter_type")
    public String getMeter_type() {
        return meter_type;
    }

    public void setMeter_type(String meter_type) {
        this.meter_type = meter_type;
    }

    @XmlAttribute(name = "appearance_type")
    public String getAppearance_type() {
        return appearance_type;
    }

    public void setAppearance_type(String appearance_type) {
        this.appearance_type = appearance_type;
    }

    @XmlAttribute(name = "save_type_list")
    public String getSave_type_list() {
        return save_type_list;
    }

    public void setSave_type_list(String save_type_list) {
        this.save_type_list = save_type_list;
    }

    @XmlAttribute(name = "recognition_type_list")
    public String getRecognition_type_list() {
        return recognition_type_list;
    }

    public void setRecognition_type_list(String recognition_type_list) {
        this.recognition_type_list = recognition_type_list;
    }

    @XmlAttribute(name = "phase")
    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    @XmlAttribute(name = "device_info")
    public String getDevice_info() {
        return device_info;
    }

    public void setDevice_info(String device_info) {
        this.device_info = device_info;
    }

    @XmlAttribute(name = "video_pos")
    public String getVideo_pos() {
        return video_pos;
    }

    public void setVideo_pos(String video_pos) {
        this.video_pos = video_pos;
    }

    @XmlAttribute(name = "lower_value")
    public String getLower_value() {
        return lower_value;
    }

    public void setLower_value(String lower_value) {
        this.lower_value = lower_value;
    }

    @XmlAttribute(name = "upper_value")
    public String getUpper_value() {
        return upper_value;
    }

    public void setUpper_value(String upper_value) {
        this.upper_value = upper_value;
    }

    @XmlAttribute(name = "data_type")
    public int getData_type() {
        return data_type;
    }

    public void setData_type(int data_type) {
        this.data_type = data_type;
    }
}
