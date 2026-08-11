package com.dji.sample.df.cqDockDf.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("df_cq_dock_uav_monitoring")
public class CqDockUavMonitoringEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("unit_code")
    private String unitCode;
    @TableField("topic_dock_sn")
    private String topicDockSn;
    @TableField("task_id")
    private String taskId;
    @TableField("dock_sn")
    private String dockSn;
    @TableField("drone_sn")
    private String droneSn;
    @TableField("drone_message_id")
    private String droneMessageId;
    @TableField("dock_message_id")
    private String dockMessageId;
    @TableField("drone_curr_time")
    private String droneCurrTime;
    @TableField("dock_curr_time")
    private String dockCurrTime;
    @TableField("drone_mode_code")
    private Integer droneModeCode;
    @TableField("dock_mode_code")
    private Integer dockModeCode;
    @TableField("flighttask_step_code")
    private Integer flighttaskStepCode;
    @TableField("drone_in_dock")
    private Boolean droneInDock;
    @TableField("latitude")
    private String latitude;
    @TableField("longitude")
    private String longitude;
    @TableField("flight_height")
    private String flightHeight;
    @TableField("horizontal_speed")
    private String horizontalSpeed;
    @TableField("vertical_speed")
    private String verticalSpeed;
    @TableField("home_distance")
    private String homeDistance;
    @TableField("flight_distance")
    private String flightDistance;
    @TableField("total_flight_time")
    private String totalFlightTime;
    @TableField("battery_level")
    private String batteryLevel;
    @TableField("communication_status")
    private String communicationStatus;
    @TableField("fault_alarm")
    private String faultAlarm;
    @TableField("operation_status")
    private String operationStatus;
    @TableField("nest_door_status")
    private String nestDoorStatus;
    @TableField("nest_platform_status")
    private String nestPlatformStatus;
    @TableField("nest_charge_status")
    private String nestChargeStatus;
    @TableField("nest_voltage")
    private String nestVoltage;
    @TableField("nest_temperature")
    private String nestTemperature;
    @TableField("nest_humidity")
    private String nestHumidity;
    @TableField("ambient_temperature")
    private String ambientTemperature;
    @TableField("wind_speed")
    private String windSpeed;
    @TableField("rainfall")
    private String rainfall;
    @TableField("drone_raw_data")
    private String droneRawData;
    @TableField("dock_raw_data")
    private String dockRawData;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
}
