package com.df.framework.vo;


import javax.xml.bind.annotation.XmlElementRef;
import java.util.List;

public class PatrolCommandItem {
    private List<BaseItem> item;

    @XmlElementRef
    public List<BaseItem> getItem() {
        return this.item;
    }

    public void setItem(List<BaseItem> item) {
        this.item = item;
    }
}
