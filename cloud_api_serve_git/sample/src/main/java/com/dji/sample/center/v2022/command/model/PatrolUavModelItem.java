package com.dji.sample.center.v2022.command.model;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/*
 * 巡视上级 附录J.5.5 模型文件同步 返回
 * */
@XmlRootElement(name = "Item")
public class PatrolUavModelItem extends BaseItem {

    //巡视系统无人机模型文件路径
    private String device_file_path;

    @XmlAttribute(name = "device_file_path")
    public String getDevice_file_path() {
        return this.device_file_path;
    }

    public void setDevice_file_path(String device_file_path) {
        this.device_file_path = device_file_path;
    }
}


