package com.dji.sample.center.v2022.command.model;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * 巡视上级附录 J.5.5 设备点位模型文件
 */
@XmlRootElement(name = "Item")
public class PatrolUavPointModelItem extends BaseItem {
    private String station_name;
    private String station_code;
    private String area_id;
    private String area_name;
    private String data_type;
    private String lower_value;
    private String upper_value;
    private String video_pos;

    private String device_id;
    private String device_name;
    private String component_id;
    private String component_name;
    private String bay_id;
    private String bay_name;
    private String main_device_id;
    private String main_device_name;
    private String device_type;
    private String meter_type;
    private String appearance_type;
    private String save_type_list;
    private String recognition_type_list;
    private String phase;
    private String device_info;

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

    @XmlAttribute(name = "station_name")
    public String getStation_name() {
        return station_name;
    }

    public void setStation_name(String station_name) {
        this.station_name = station_name;
    }

    @XmlAttribute(name = "station_code")
    public String getStation_code() {
        return station_code;
    }

    public void setStation_code(String station_code) {
        this.station_code = station_code;
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

    @XmlAttribute(name = "data_type")
    public String getData_type() {
        return data_type;
    }

    public void setData_type(String data_type) {
        this.data_type = data_type;
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

    @XmlAttribute(name = "video_pos")
    public String getVideo_pos() {
        return video_pos;
    }

    public void setVideo_pos(String video_pos) {
        this.video_pos = video_pos;
    }
}
