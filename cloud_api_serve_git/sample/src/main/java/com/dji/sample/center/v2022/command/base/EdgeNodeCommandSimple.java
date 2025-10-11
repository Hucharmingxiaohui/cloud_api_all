package com.dji.sample.center.v2022.command.base;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PatrolHost")
public class EdgeNodeCommandSimple implements Serializable {
    private static final long serialVersionUID = -4770113769299694976L;

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
}

