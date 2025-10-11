package com.dji.sample.center.v2022.command.model;

import lombok.Getter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * 中心端附录 B.3.8 设备点位模型文件
 */
@XmlRootElement(name = "Device_Model")
public class PatrolUavPointModelData implements Serializable {

    @XmlElements(value = {@XmlElement(name = "Item", type = PatrolUavPointModelItem.class)})
    @Getter
    public List<PatrolUavPointModelItem> item;
}

