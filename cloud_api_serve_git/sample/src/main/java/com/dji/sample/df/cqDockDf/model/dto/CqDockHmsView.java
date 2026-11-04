package com.dji.sample.df.cqDockDf.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class CqDockHmsView {
    private Long id;
    @JsonProperty("unit_code") private String unitCode;
    @JsonProperty("topic_dock_sn") private String topicDockSn;
    @JsonProperty("message_id") private String messageId;
    @JsonProperty("alarm_code") private String alarmCode;
    @JsonProperty("dock_sn") private String dockSn;
    @JsonProperty("drone_sn") private String droneSn;
    @JsonProperty("gateway_type") private String gatewayType;
    private Boolean imminent;
    @JsonProperty("in_the_sky") private Boolean inTheSky;
    private Integer level;
    private Integer module;
    private String message;
    @JsonProperty("create_time") private Date createTime;
    @JsonProperty("update_time") private Date updateTime;
    @JsonProperty("level_label") private String levelLabel;
    @JsonProperty("module_label") private String moduleLabel;

    public static CqDockHmsView from(com.dji.sample.df.cqDockDf.model.entity.CqDockHmsEntity e) {
        CqDockHmsView v = new CqDockHmsView();
        v.id=e.getId(); v.unitCode=e.getUnitCode(); v.topicDockSn=e.getTopicDockSn(); v.messageId=e.getMessageId();
        v.alarmCode=e.getAlarmCode(); v.dockSn=e.getDockSn(); v.droneSn=e.getUavSn(); v.gatewayType=e.getGatewayType();
        v.imminent=e.getImminent(); v.inTheSky=e.getInTheSky(); v.level=e.getLevel(); v.module=e.getModule(); v.message=e.getMessage();
        v.createTime=e.getCreateTime(); v.updateTime=e.getUpdateTime();
        v.levelLabel=levelLabel(e.getLevel()); v.moduleLabel=moduleLabel(e.getModule());
        return v;
    }

    private static String levelLabel(Integer value) {
        return value == null || value < 0 || value > 2 ? null : new String[]{"通知", "提醒", "警告"}[value];
    }
    private static String moduleLabel(Integer value) {
        return value == null || value < 0 || value > 3 ? null : new String[]{"飞行任务", "设备管理", "媒体", "hms"}[value];
    }
}
