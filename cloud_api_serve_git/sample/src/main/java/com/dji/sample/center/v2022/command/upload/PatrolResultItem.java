package com.dji.sample.center.v2022.command.upload;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 无人机系统主动向上级上报巡视结果数据的item。详情见规范：J.6.11
 */
@XmlRootElement(name = "Item")
public class PatrolResultItem extends BaseItem {

    private String patroldevice_code;
    private String patroldevice_name;
    private String task_name;
    private String task_code;
    private String device_name;
    private String device_id;
    private String value;
    private String value_unit;
    private String unit;
    private String time;
    private String recognition_type;
    private String file_type;
    private String file_path;
    private String rectangle;
    private String task_patrolled_id;
    private String obj_id;
    private String valid;

    @XmlAttribute(name = "patroldevice_name")
    public String getPatroldevice_name() {
        return patroldevice_name;
    }

    public void setPatroldevice_name(String patroldevice_name) {
        this.patroldevice_name = patroldevice_name;
    }

    @XmlAttribute(name = "valid")
    public String getValid() {
        return valid;
    }

    public void setValid(String valid) {
        this.valid = valid;
    }

    @XmlAttribute(name = "obj_id")
    public String getObj_id() {
        return this.obj_id;
    }


    public void setObj_id(String obj_id) {
        this.obj_id = obj_id;
    }


    @XmlAttribute(name = "patroldevice_code")
    public String getPatroldevice_code() {
        return this.patroldevice_code;
    }


    @XmlAttribute(name = "task_name")
    public String getTask_name() {
        return this.task_name;
    }


    @XmlAttribute(name = "task_code")
    public String getTask_code() {
        return this.task_code;
    }


    @XmlAttribute(name = "device_name")
    public String getDevice_name() {
        return this.device_name;
    }


    @XmlAttribute(name = "device_id")
    public String getDevice_id() {
        return this.device_id;
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


    @XmlAttribute(name = "time")
    public String getTime() {
        return this.time;
    }


    @XmlAttribute(name = "recognition_type")
    public String getRecognition_type() {
        return this.recognition_type;
    }


    @XmlAttribute(name = "file_type")
    public String getFile_type() {
        return this.file_type;
    }


    @XmlAttribute(name = "file_path")
    public String getFile_path() {
        return this.file_path;
    }


    @XmlAttribute(name = "rectangle")
    public String getRectangle() {
        return this.rectangle;
    }


    @XmlAttribute(name = "task_patrolled_id")
    public String getTask_patrolled_id() {
        return this.task_patrolled_id;
    }


    public void setPatroldevice_code(String patroldevice_code) {
        this.patroldevice_code = patroldevice_code;
    }


    public void setTask_name(String task_name) {
        this.task_name = task_name;
    }


    public void setTask_code(String task_code) {
        this.task_code = task_code;
    }


    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }


    public void setDevice_id(String device_id) {
        this.device_id = device_id;
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


    public void setTime(String time) {
        this.time = time;
    }


    public void setRecognition_type(String recognition_type) {
        this.recognition_type = recognition_type;
    }


    public void setFile_type(String file_type) {
        this.file_type = file_type;
    }


    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }


    public void setRectangle(String rectangle) {
        this.rectangle = rectangle;
    }


    public void setTask_patrolled_id(String task_patrolled_id) {
        this.task_patrolled_id = task_patrolled_id;
    }
}
