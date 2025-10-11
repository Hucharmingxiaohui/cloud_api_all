package com.dji.sample.center.v2022.command.upload;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 无人机系统主动向上级上报巡视路线的item。详情见规范：J.6.7
 */
@XmlRootElement(name = "Item")
public class UavPatrolLineItem extends BaseItem {

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
     * 坐标，格式：”x,y,z,a”，x、y、z为地图文件的坐标，a为巡视设备航向角
     */
    private String coordinate_pixel;
    /**
     * 经纬度，格式：”x，y”
     */
    private String coordinate_geography;

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

    @XmlAttribute(name = "coordinate_pixel")
    public String getCoordinate_pixel() {
        return this.coordinate_pixel;
    }

    public void setCoordinate_pixel(String coordinate_pixel) {
        this.coordinate_pixel = coordinate_pixel;
    }

    @XmlAttribute(name = "coordinate_geography")
    public String getCoordinate_geography() {
        return this.coordinate_geography;
    }

    public void setCoordinate_geography(String coordinate_geography) {
        this.coordinate_geography = coordinate_geography;
    }
}
