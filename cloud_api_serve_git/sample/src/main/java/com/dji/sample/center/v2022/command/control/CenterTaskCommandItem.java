package com.dji.sample.center.v2022.command.control;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Item")
public class CenterTaskCommandItem extends BaseItem {

    private String type;
    private String task_code;
    private String task_name;
    private String priority;
    private int device_level;
    private String device_list;
    private String fixed_start_time;
    private String cycle_month;
    private String cycle_week;
    private String cycle_execute_time;
    private String cycle_start_time;
    private String cycle_end_time;
    private String interval_number;
    private String interval_type;
    private String interval_execute_time;
    private String interval_start_time;
    private String interval_end_time;
    private String invalid_start_time;
    private String invalid_end_time;
    private String isenable;
    private String creator;
    private String create_time;

    @XmlAttribute(name = "type")
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlAttribute(name = "task_code")
    public String getTask_code() {
        return this.task_code;
    }

    public void setTask_code(String task_code) {
        this.task_code = task_code;
    }

    @XmlAttribute(name = "task_name")
    public String getTask_name() {
        return this.task_name;
    }

    public void setTask_name(String task_name) {
        this.task_name = task_name;
    }

    @XmlAttribute(name = "priority")
    public String getPriority() {
        return this.priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    @XmlAttribute(name = "device_list")
    public String getDevice_list() {
        return this.device_list;
    }

    public void setDevice_list(String device_list) {
        this.device_list = device_list;
    }

    @XmlAttribute(name = "creator")
    public String getCreator() {
        return this.creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @XmlAttribute(name = "create_time")
    public String getCreate_time() {
        return this.create_time;
    }

    public void setCreate_time(String create_time) {
        this.create_time = create_time;
    }

    @XmlAttribute(name = "device_level")
    public int getDevice_level() {
        return this.device_level;
    }

    public void setDevice_level(int device_level) {
        this.device_level = device_level;
    }

    @XmlAttribute(name = "fixed_start_time")
    public String getFixed_start_time() {
        return this.fixed_start_time;
    }

    public void setFixed_start_time(String fixed_start_time) {
        this.fixed_start_time = fixed_start_time;
    }

    @XmlAttribute(name = "cycle_month")
    public String getCycle_month() {
        return this.cycle_month;
    }

    public void setCycle_month(String cycle_month) {
        this.cycle_month = cycle_month;
    }

    @XmlAttribute(name = "cycle_week")
    public String getCycle_week() {
        return this.cycle_week;
    }

    public void setCycle_week(String cycle_week) {
        this.cycle_week = cycle_week;
    }

    @XmlAttribute(name = "cycle_execute_time")
    public String getCycle_execute_time() {
        return this.cycle_execute_time;
    }

    public void setCycle_execute_time(String cycle_execute_time) {
        this.cycle_execute_time = cycle_execute_time;
    }

    @XmlAttribute(name = "cycle_start_time")
    public String getCycle_start_time() {
        return this.cycle_start_time;
    }

    public void setCycle_start_time(String cycle_start_time) {
        this.cycle_start_time = cycle_start_time;
    }

    @XmlAttribute(name = "cycle_end_time")
    public String getCycle_end_time() {
        return this.cycle_end_time;
    }

    public void setCycle_end_time(String cycle_end_time) {
        this.cycle_end_time = cycle_end_time;
    }

    @XmlAttribute(name = "interval_number")
    public String getInterval_number() {
        return this.interval_number;
    }

    public void setInterval_number(String interval_number) {
        this.interval_number = interval_number;
    }

    @XmlAttribute(name = "interval_type")
    public String getInterval_type() {
        return this.interval_type;
    }

    public void setInterval_type(String interval_type) {
        this.interval_type = interval_type;
    }

    @XmlAttribute(name = "interval_execute_time")
    public String getInterval_execute_time() {
        return this.interval_execute_time;
    }

    public void setInterval_execute_time(String interval_execute_time) {
        this.interval_execute_time = interval_execute_time;
    }

    @XmlAttribute(name = "interval_start_time")
    public String getInterval_start_time() {
        return this.interval_start_time;
    }

    public void setInterval_start_time(String interval_start_time) {
        this.interval_start_time = interval_start_time;
    }

    @XmlAttribute(name = "interval_end_time")
    public String getInterval_end_time() {
        return this.interval_end_time;
    }

    public void setInterval_end_time(String interval_end_time) {
        this.interval_end_time = interval_end_time;
    }

    @XmlAttribute(name = "invalid_start_time")
    public String getInvalid_start_time() {
        return this.invalid_start_time;
    }

    public void setInvalid_start_time(String invalid_start_time) {
        this.invalid_start_time = invalid_start_time;
    }

    @XmlAttribute(name = "invalid_end_time")
    public String getInvalid_end_time() {
        return this.invalid_end_time;
    }

    public void setInvalid_end_time(String invalid_end_time) {
        this.invalid_end_time = invalid_end_time;
    }

    @XmlAttribute(name = "isenable")
    public String getIsenable() {
        return this.isenable;
    }

    public void setIsenable(String isenable) {
        this.isenable = isenable;
    }
}


