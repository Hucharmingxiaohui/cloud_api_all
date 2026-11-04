package com.dji.sample.df.cqDockDf.model.dto;

import com.dji.sample.df.cqDockDf.model.entity.CqDockUavMonitoringEntity;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import java.util.Date;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CqDockMonitoringView {
    private Long id;
    private String unitCode;
    private String topicDockSn;
    private String taskId;
    private String dockSn;
    private String droneSn;
    private String droneMessageId;
    private String dockMessageId;
    private String droneCurrTime;
    private String dockCurrTime;
    private Integer droneModeCode;
    private Integer dockModeCode;
    private Integer flighttaskStepCode;
    private Boolean droneInDock;
    private String latitude;
    private String longitude;
    private String flightHeight;
    private String horizontalSpeed;
    private String verticalSpeed;
    private String homeDistance;
    private String flightDistance;
    private String totalFlightTime;
    private String batteryLevel;
    private String nestVoltage;
    private String nestTemperature;
    private String nestHumidity;
    private String ambientTemperature;
    private String windSpeed;
    private String rainfall;
    private Date createTime;
    private Date updateTime;

    public static CqDockMonitoringView from(CqDockUavMonitoringEntity entity) {
        CqDockMonitoringView view = new CqDockMonitoringView();
        view.id = entity.getId();
        view.unitCode = entity.getUnitCode();
        view.topicDockSn = entity.getTopicDockSn();
        view.taskId = entity.getTaskId();
        view.dockSn = entity.getDockSn();
        view.droneSn = entity.getDroneSn();
        view.droneMessageId = entity.getDroneMessageId();
        view.dockMessageId = entity.getDockMessageId();
        view.droneCurrTime = entity.getDroneCurrTime();
        view.dockCurrTime = entity.getDockCurrTime();
        view.droneModeCode = entity.getDroneModeCode();
        view.dockModeCode = entity.getDockModeCode();
        view.flighttaskStepCode = entity.getFlighttaskStepCode();
        view.droneInDock = entity.getDroneInDock();
        view.latitude = entity.getLatitude();
        view.longitude = entity.getLongitude();
        view.flightHeight = entity.getFlightHeight();
        view.horizontalSpeed = entity.getHorizontalSpeed();
        view.verticalSpeed = entity.getVerticalSpeed();
        view.homeDistance = entity.getHomeDistance();
        view.flightDistance = entity.getFlightDistance();
        view.totalFlightTime = entity.getTotalFlightTime();
        view.batteryLevel = entity.getBatteryLevel();
        view.nestVoltage = entity.getNestVoltage();
        view.nestTemperature = entity.getNestTemperature();
        view.nestHumidity = entity.getNestHumidity();
        view.ambientTemperature = entity.getAmbientTemperature();
        view.windSpeed = entity.getWindSpeed();
        view.rainfall = entity.getRainfall();
        view.createTime = entity.getCreateTime();
        view.updateTime = entity.getUpdateTime();
        return view;
    }
}
