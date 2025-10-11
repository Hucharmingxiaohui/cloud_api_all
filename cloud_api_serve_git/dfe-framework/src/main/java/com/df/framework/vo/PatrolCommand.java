package com.df.framework.vo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PatrolHost")
public class PatrolCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    //发送方唯一标识
    @XmlElement(name = "SendCode")
    private String sendCode;
    //接收方唯一标识
    @XmlElement(name = "ReceiveCode")
    private String receiveCode;
    //消息类型
    @XmlElement(name = "Type")
    private String type;
    //目标对象唯一标识
    @XmlElement(name = "Code")
    private String code;
    //命令
    @XmlElement(name = "Command")
    private String command;
    //时间，格式：yyyy-MM-dd hh:mm:ss
    @XmlElement(name = "Time")
    private String time;
    //消息内容
    @XmlElement(name = "Items")
    private PatrolCommandItem items;

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

    public PatrolCommandItem getItems() {
        return this.items;
    }

    public void setItems(PatrolCommandItem items) {
        this.items = items;
    }

    /**
     * 添加消息内容Item
     *
     * @param item
     */
    public void addItem(BaseItem item) {
        if (item == null) {
            return;
        }

        if (items == null) {
            items = new PatrolCommandItem();
        }

        List<BaseItem> baseItems = items.getItem();
        if (baseItems == null) {
            baseItems = new ArrayList<>();
            items.setItem(baseItems);
        }

        baseItems.add(item);
    }
}
