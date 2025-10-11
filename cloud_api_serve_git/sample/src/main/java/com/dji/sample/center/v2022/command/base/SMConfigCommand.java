package com.dji.sample.center.v2022.command.base;


import com.dji.sample.center.v2022.command.model.EdgeNodeMSConfigModelItem;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Effect_Config")
public class SMConfigCommand implements Serializable {
    @XmlElement(name = "Item")
    public List<EdgeNodeMSConfigModelItem> Item;


    public List<EdgeNodeMSConfigModelItem> getItem() {
        return this.Item;
    }

    public void setItem(List<EdgeNodeMSConfigModelItem> Item) {
        this.Item = Item;
    }
}
