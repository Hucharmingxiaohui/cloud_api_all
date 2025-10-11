package com.dji.sample.center.v2022.command.upload;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 无人机系统主动向巡视上级上报环境数据的item。详情见规范：J.6.9
 */
@XmlRootElement(name = "Item")
public class UavHostEnvItem extends BaseItem {

    /**
     * 巡视设备名称（环境采集设备属于巡视设备时填该巡视设备名称，否则填空）
     */
    private String patroldevice_name;
    /**
     * 巡视设备编码 （环境采集设备属于巡视设备时填该巡视设备编码，否则填空）
     */
    private String patroldevice_code;
    /**
     * 时间
     */
    private String time;
    /**
     * 类型
     *
     * <1>: = 环境温度 适用机器人和无人机
     * <2>: = 环境湿度 适用机器人和无人机
     * <3>: = 风速 适用机器人和无人机
     * <4>: = 雨量 适用机器人和无人机
     * <5>: = 风向 适用机器人和无人机
     * <6>: = 气压 适用机器人和无人机
     * <7>: = 氧气 适用机器人
     * <8>: = SF6 适用机器人
     */
    private String type;
    /**
     * 值
     *
     */
    private String value;
    /**
     * 值带单位
     */
    private String value_unit;
    /**
     * 单位
     */
    private String unit;

    @XmlAttribute(name = "patroldevice_name")
    public String getPatroldevice_name() {
        return patroldevice_name;
    }

    public void setPatroldevice_name(String patroldevice_name) {
        this.patroldevice_name = patroldevice_name;
    }

    @XmlAttribute(name = "patroldevice_code")
    public String getPatroldevice_code() {
        return patroldevice_code;
    }

    public void setPatroldevice_code(String patroldevice_code) {
        this.patroldevice_code = patroldevice_code;
    }

    @XmlAttribute(name = "time")
    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @XmlAttribute(name = "type")
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlAttribute(name = "value")
    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @XmlAttribute(name = "value_unit")
    public String getValue_unit() {
        return this.value_unit;
    }

    public void setValue_unit(String value_unit) {
        this.value_unit = value_unit;
    }

    @XmlAttribute(name = "unit")
    public String getUnit() {
        return this.unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
