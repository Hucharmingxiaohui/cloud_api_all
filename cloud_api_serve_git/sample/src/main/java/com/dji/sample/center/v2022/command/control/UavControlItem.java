package com.dji.sample.center.v2022.command.control;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Item")
public class UavControlItem extends BaseItem {
    private String value;

    @XmlAttribute(name = "value")
    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
