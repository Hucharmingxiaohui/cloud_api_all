package com.dji.sample.center.v2022.command.upload;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 无人机系统主动向上级上报无人机机巢状态数据的item。详情见规范：J.6.4
 */
@XmlRootElement(name = "Item")
public class UavNestStatusDataItem extends BaseItem {
    /**
     * 机巢名称
     */
    private String nest_name;
    /**
     * 机巢编码
     */
    private String nest_code;
    /**
     * 模块序号（非必须，用于区分多个模块）
     */
    private String module_no;
    /**
     * 类型
     * <1>: = 电池电量
     * <2>: = 电池使用状态
     * <1>: = 充电中
     * <2>: = 使用中
     * <3>: = 电池状态
     * <1>: = 正常
     * <2>: = 异常
     * <4>: = 电池电压
     * <5>: = 舱内温度
     * <6>: = 舱内湿度
     */
    private String type;
    /**
     * 值
     */
    private String value;
    /**
     * 值带单位
     */
    private String value_unit;
    /**
     * 单位
     */
    private String unit;

    @XmlAttribute(name = "nest_name")
    public String getNest_name() {
        return nest_name;
    }

    public void setNest_name(String nest_name) {
        this.nest_name = nest_name;
    }

    @XmlAttribute(name = "nest_code")
    public String getNest_code() {
        return nest_code;
    }

    public void setNest_code(String nest_code) {
        this.nest_code = nest_code;
    }

    @XmlAttribute(name = "module_no")
    public String getModule_no() {
        return module_no;
    }

    public void setModule_no(String module_no) {
        this.module_no = module_no;
    }

    @XmlAttribute(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlAttribute(name = "value")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @XmlAttribute(name = "value_unit")
    public String getValue_unit() {
        return value_unit;
    }

    public void setValue_unit(String value_unit) {
        this.value_unit = value_unit;
    }

    @XmlAttribute(name = "unit")
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
