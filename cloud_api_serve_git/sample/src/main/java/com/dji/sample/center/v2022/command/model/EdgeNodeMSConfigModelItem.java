package com.dji.sample.center.v2022.command.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *  检修区域配置文件
 * <p>
 *
 */
@XmlRootElement(name = "Item")
public class EdgeNodeMSConfigModelItem {

    private String source_code;
    private String source_name;
    private String source_type;
    private String device_id;

    @XmlAttribute(name = "source_code")
    public String getSource_code() {
        return source_code;
    }

    public void setSource_code(String source_code) {
        this.source_code = source_code;
    }

    @XmlAttribute(name = "source_name")
    public String getSource_name() {
        return source_name;
    }

    public void setSource_name(String source_name) {
        this.source_name = source_name;
    }

    @XmlAttribute(name = "source_type")
    public String getSource_type() {
        return source_type;
    }

    public void setSource_type(String source_type) {
        this.source_type = source_type;
    }

    @XmlAttribute(name = "device_id")
    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }
}
