package com.dji.sample.center.v2022.command.base;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PatrolHost")
public class PatrolHostCommand implements Serializable {
    private static final long serialVersionUID = -7241252023624476312L;

    @XmlElement(name = "SendCode")
    private String sendCode;

    @XmlElement(name = "ReceiveCode")
    private String receiveCode;

    @XmlElement(name = "Type")
    private String type;

    @XmlElement(name = "Code")
    private String code;

    @XmlElement(name = "Command")
    private String command;

    @XmlElement(name = "Time")
    private String time;

    @XmlElement(name = "Items")
    private PatrolHostCommandItem items;

    public String getSendCode() {
        return this.sendCode;
    }

    public void setSendCode(String sendCode) {
        this.sendCode = sendCode;
    }

    public String getReceiveCode() {
        return this.receiveCode;
    }

    public void setReceiveCode(String receiveCode) {
        this.receiveCode = receiveCode;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCommand() {
        return this.command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public PatrolHostCommandItem getItems() {
        return this.items;
    }

    public void setItems(PatrolHostCommandItem items) {
        this.items = items;
    }

    /**
     * 添加item，原来的太蛮烦了
     * Demo：
     * PatrolHostCommand response = patrolHostClient.getBaseCommandData("251", "4", "200");
     * PatrolHostTaskControlItem item = (PatrolHostTaskControlItem)rtn.getObject();
     * response.addItem(item);
     * patrolHostClient.responseCommand(response, PatrolHostTaskControlItem.class, requestSerialNum);
     * 姜学云 2022/3/27 9:24
     *
     * @param item
     */
    public void addItem(BaseItem item) {
        if (item == null) {
            return;
        }

        if (items == null) {
            items = new PatrolHostCommandItem();
        }

        List<BaseItem> baseItems = items.getItem();
        if (baseItems == null) {
            baseItems = new ArrayList<>();
            items.setItem(baseItems);
        }

        baseItems.add(item);
    }

    public void addItems(List<? extends BaseItem> l) {
        if (l == null) {
            return;
        }

        if (items == null) {
            items = new PatrolHostCommandItem();
        }

        List<BaseItem> baseItems = items.getItem();
        if (baseItems == null) {
            baseItems = new ArrayList<>();
            items.setItem(baseItems);
        }

        baseItems.addAll(l);
    }
}
