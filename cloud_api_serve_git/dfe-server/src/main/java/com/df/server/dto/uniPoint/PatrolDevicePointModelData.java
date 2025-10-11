package com.df.server.dto.uniPoint;

import lombok.Getter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * 设备点位模型文件内容。见规范B.2.3
 */
@XmlRootElement(name = "Device_Model")
public class PatrolDevicePointModelData implements Serializable {
    private static final long serialVersionUID = 5751458278552726359L;

    @XmlElements(value = {@XmlElement(name = "Item", type = PatrolDevicePointModelItem.class)})
    @Getter
    public List<PatrolDevicePointModelItem> item;
}

