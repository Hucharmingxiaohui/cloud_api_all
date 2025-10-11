package com.dji.sample.center.v2022.command.upload;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 无人机系统主动向上级上报巡视设备异常告警的item。详情见规范：J.6.8
 */
@XmlRootElement(name = "Item")
public class UavDeviceAlarmItem extends BaseItem {

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
     * 告警描述
     */
    private String content;

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

    @XmlAttribute(name = "content")
    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
