package com.dji.sample.df.uavCommonHandleDf.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * drone_monitoring 表 无人机及机巢监控数据实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("drone_monitoring")
public class DroneMonitoringEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("drone_id")
    private String droneId;

    @TableField("nest_id")
    private String nestId;

    @TableField("battery_level")
    private String batteryLevel;          // 无人机电池电量(%)

    @TableField("communication_status")
    private String communicationStatus;        // 无人机通信状态

    @TableField("fault_alarm")
    private String faultAlarm;                // 无人机故障报警

    @TableField("operation_status")
    private String operationStatus;            // 无人机运行状态

    @TableField("horizontal_speed")
    private String horizontalSpeed;        // 无人机水平速度(m/s)

    @TableField("vertical_speed")
    private String verticalSpeed;          // 无人机垂直速度(m/s)

    @TableField("flight_distance")
    private String flightDistance;         // 无人机飞行距离(m)

    @TableField("flight_height")
    private String flightHeight;           // 无人机飞行高度(m)

    @TableField("nest_door_status")
    private String nestDoorStatus;             // 机巢舱门状态

    @TableField("nest_platform_status")
    private String nestPlatformStatus;         // 机巢平台状态

    @TableField("nest_charge_status")
    private String nestChargeStatus;           // 机巢充电状态

    @TableField("nest_voltage")
    private String nestVoltage;             // 机巢工作电压(mV)

    @TableField("nest_temperature")
    private String nestTemperature;         // 机巢舱内温度(℃)

    @TableField("nest_humidity")
    private String nestHumidity;            // 机巢舱内湿度(%)

    @TableField("ambient_temperature")
    private String ambientTemperature;      // 环境温度(℃)

    @TableField("wind_speed")
    private String windSpeed;               // 风速(m/s)

    @TableField("rainfall")
    private String rainfall;                 // 雨量
}
