package com.dji.sample.center.v2022.command.control;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 检修区域配置
 */
@XmlRootElement(name = "Item")
public class CenterOverhaulAreaItem extends BaseItem {

    private String config_code;
    private Integer enable;
    private String start_time;
    private String end_time;
    private Integer device_level;
    private String device_list;
    private String coordinate_pixel;

    @XmlAttribute(name = "enable")
    public Integer getEnable() {
        return enable;
    }

    public void setEnable(Integer enable) {
        this.enable = enable;
    }

    @XmlAttribute(name = "start_time")
    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    @XmlAttribute(name = "end_time")
    public String getEnd_time() {
        return end_time;
    }

    public void setEnd_time(String end_time) {
        this.end_time = end_time;
    }

    @XmlAttribute(name = "device_level")
    public Integer getDevice_level() {
        return device_level;
    }

    public void setDevice_level(Integer device_level) {
        this.device_level = device_level;
    }

    @XmlAttribute(name = "device_list")
    public String getDevice_list() {
        return device_list;
    }

    public void setDevice_list(String device_list) {
        this.device_list = device_list;
    }

    @XmlAttribute(name = "config_code")
    public String getConfig_code() {
        return config_code;
    }

    public void setConfig_code(String config_code) {
        this.config_code = config_code;
    }

    @XmlAttribute(name = "coordinate_pixel")
    public String getCoordinate_pixel() {
        return coordinate_pixel;
    }

    public void setCoordinate_pixel(String coordinate_pixel) {
        this.coordinate_pixel = coordinate_pixel;
    }
}
