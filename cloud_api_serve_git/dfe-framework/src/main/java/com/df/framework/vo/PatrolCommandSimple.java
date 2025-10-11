package com.df.framework.vo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * 不含<items></items>的通用bean
 * 参考 E.4.3 示例
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PatrolHost")
public class PatrolCommandSimple implements Serializable {

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


