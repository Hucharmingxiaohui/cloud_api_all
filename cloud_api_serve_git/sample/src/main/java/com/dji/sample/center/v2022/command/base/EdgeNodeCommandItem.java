package com.dji.sample.center.v2022.command.base;


import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "Items")
public class EdgeNodeCommandItem {
    private List<BaseItem> item;

    @XmlElementRef
    public List<com.dji.sample.center.v2022.tool.BaseItem> getItem() {
        return this.item;
    }

    public void setItem(List<BaseItem> item) {
        this.item = item;
    }
}
