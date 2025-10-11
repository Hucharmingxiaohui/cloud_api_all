package com.dji.sample.center.v2022.command.upload;

import com.dji.sample.center.v2022.tool.BaseItem;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 无人机系统主动向上级上报任务状态数据的item。详情见规范：J.6.10
 */
@XmlRootElement(name = "Item")
public class PatrolStatusItem extends BaseItem {

    private String task_patrolled_id;
    private String task_name;
    private String task_code;
    private String task_state;
    private String plan_start_time;
    private String start_time;
    private String task_progress;
    private String task_estimated_time;
    private String description;

    @XmlAttribute(name = "task_patrolled_id")
    public String getTask_patrolled_id() {
        return task_patrolled_id;
    }

    public void setTask_patrolled_id(String task_patrolled_id) {
        this.task_patrolled_id = task_patrolled_id;
    }

    @XmlAttribute(name = "task_name")
    public String getTask_name() {
        return task_name;
    }

    public void setTask_name(String task_name) {
        this.task_name = task_name;
    }

    @XmlAttribute(name = "task_code")
    public String getTask_code() {
        return task_code;
    }

    public void setTask_code(String task_code) {
        this.task_code = task_code;
    }

    @XmlAttribute(name = "task_state")
    public String getTask_state() {
        return task_state;
    }

    public void setTask_state(String task_state) {
        this.task_state = task_state;
    }

    @XmlAttribute(name = "plan_start_time")
    public String getPlan_start_time() {
        return plan_start_time;
    }

    public void setPlan_start_time(String plan_start_time) {
        this.plan_start_time = plan_start_time;
    }

    @XmlAttribute(name = "start_time")
    public String getStart_time() {
        return start_time;
    }

    public void setStart_time(String start_time) {
        this.start_time = start_time;
    }

    @XmlAttribute(name = "task_progress")
    public String getTask_progress() {
        return task_progress;
    }

    public void setTask_progress(String task_progress) {
        this.task_progress = task_progress;
    }

    @XmlAttribute(name = "task_estimated_time")
    public String getTask_estimated_time() {
        return task_estimated_time;
    }

    public void setTask_estimated_time(String task_estimated_time) {
        this.task_estimated_time = task_estimated_time;
    }

    @XmlAttribute(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
