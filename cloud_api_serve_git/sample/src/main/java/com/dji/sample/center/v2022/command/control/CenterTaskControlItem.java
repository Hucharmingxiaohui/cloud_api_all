package com.dji.sample.center.v2022.command.control;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 巡视上级任务控制指令item
 */
@XmlRootElement(name = "Item")
public class CenterTaskControlItem extends BaseItem {

    private String task_patrolled_id;

    @XmlAttribute(name = "task_patrolled_id")
    public String getTask_patrolled_id() {
        return task_patrolled_id;
    }

    public void setTask_patrolled_id(String task_patrolled_id) {
        this.task_patrolled_id = task_patrolled_id;
    }
}
