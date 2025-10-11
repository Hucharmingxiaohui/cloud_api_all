package com.dji.sample.center.v2022.command.system;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 巡视上级注册消息内容
 */
@XmlRootElement(name = "Item")
public class CenterRegisterItem extends BaseItem {

    private String heart_beat_interval;
    private String patroldevice_run_interval;
    private String env_interval;
    private String nest_run_interval;

    @XmlAttribute(name = "heart_beat_interval")
    public String getHeart_beat_interval() {
        return this.heart_beat_interval;
    }

    public void setHeart_beat_interval(String heart_beat_interval) {
        this.heart_beat_interval = heart_beat_interval;
    }

    @XmlAttribute(name = "patroldevice_run_interval")
    public String getPatroldevice_run_interval() {
        return patroldevice_run_interval;
    }

    public void setPatroldevice_run_interval(String patroldevice_run_interval) {
        this.patroldevice_run_interval = patroldevice_run_interval;
    }

    @XmlAttribute(name = "env_interval")
    public String getEnv_interval() {
        return this.env_interval;
    }

    public void setEnv_interval(String env_interval) {
        this.env_interval = env_interval;
    }

    @XmlAttribute(name = "nest_run_interval")
    public String getNest_run_interval() {
        return nest_run_interval;
    }

    public void setNest_run_interval(String nest_run_interval) {
        this.nest_run_interval = nest_run_interval;
    }
}


