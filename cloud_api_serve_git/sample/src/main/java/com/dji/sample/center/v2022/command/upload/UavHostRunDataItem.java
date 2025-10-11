package com.dji.sample.center.v2022.command.upload;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 无人机系统主动向上级上报巡视设备运行数据的item。详情见规范：J.6.3
 */
@XmlRootElement(name = "Item")
public class UavHostRunDataItem extends BaseItem {

    /**
     * 巡视设备名称
     */
    private String patroldevice_name;
    /**
     * 巡视设备编码
     */
    private String patroldevice_code;
    /**
     * 时间
     */
    private String time;
    /**
     * 类型
     */
    private String type;
    /**
     * 值
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

    @XmlAttribute(name = "patroldevice_code")
    public String getPatroldevice_code() {
        return patroldevice_code;
    }

    @XmlAttribute(name = "time")
    public String getTime() {
        return this.time;
    }

    @XmlAttribute(name = "type")
    public String getType() {
        return this.type;
    }

    @XmlAttribute(name = "value")
    public String getValue() {
        return this.value;
    }

    @XmlAttribute(name = "value_unit")
    public String getValue_unit() {
        return this.value_unit;
    }

    @XmlAttribute(name = "unit")
    public String getUnit() {
        return this.unit;
    }

    public void setPatroldevice_name(String patroldevice_name) {
        this.patroldevice_name = patroldevice_name;
    }

    public void setPatroldevice_code(String patroldevice_code) {
        this.patroldevice_code = patroldevice_code;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValue_unit(String value_unit) {
        this.value_unit = value_unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}


