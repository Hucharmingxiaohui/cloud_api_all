package com.df.server.vo.UniRobot;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 机器人接口
 * <p>
 * Created by lianyc on 2025-05-20
 */
@Data
public class UniRobotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private Integer id;
    /**
     * 机器人编码，和df_uni_patroldevice_book表的patroldevice_code一致
     */
    private String robotCode;
    /**
     * 机器人（设备）名称 ，对应标准规范中的patroldevcie_name
     */
    private String robotName;
    /**
     * <1>: = 室外机器人
     * <2>: = 室内机器人
     * <3>: = 挂轨机器人
     */
    private Integer robotType;
    /**
     * 机器人系统编码
     */
    private String sysCode;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 状态上报时间，格式为 yyyy-MM-dd
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date statusUpdateTime;
    /**
     * 电池电量状态 0正常 1低 值带单位
     */
    private String batteryState;
    /**
     * 通信状态异常 0正常 1异常 值带单位
     */
    private String commState;
    /**
     * 超声停障  0正常 1停障 值带单位
     */
    private String avoidingState;
    /**
     * 驱动状态 0正常 1异常 值带单位
     */
    private String driverState;
    /**
     * 故障报警 0正常 1报警 值带单位
     */
    private String alarmState;
    /**
     * 运行状态：1空闲状态 2巡视状态 3充电状态 4检修状态 值带单位
     */
    private String runState;
    /**
     * 控制模式：1任务模式  2紧急定位模式  3后台遥控模式  4手持遥控模式 值带单位
     */
    private String ctrlMod;
    /**
     * 控制权状态 0空闲 1获得 值带单位
     */
    private String authState;
    /**
     * 轮转值班状态 0空闲 1值班 值带单位
     */
    private String dutyState;
    /**
     * 电池电量状态 0正常 1低，值
     */
    private String batteryStateValue;
    /**
     * 通信状态异常 0正常 1异常，值
     */
    private String commStateValue;
    /**
     * 超声停障  0正常 1停障，值
     */
    private String avoidingStateValue;
    /**
     * 驱动状态 0正常 1异常，值
     */
    private String driverStateValue;
    /**
     * 故障报警 0正常 1报警，值
     */
    private String alarmStateValue;
    /**
     * 运行状态：1空闲状态 2巡视状态 3充电状态 4检修状态，值
     */
    private String runStateValue;
    /**
     * 控制模式：1任务模式  2紧急定位模式  3后台遥控模式  4手持遥控模式，值
     */
    private String ctrlModValue;
    /**
     * 控制权状态 0空闲 1获得，值
     */
    private String authStateValue;
    /**
     * 轮转值班状态 0空闲 1值班，值
     */
    private String dutyStateValue;
    /**
     * 电池电量状态 0正常 1低 单位
     */
    private String batteryStateUnit;
    /**
     * 通信状态异常 0正常 1异常 单位
     */
    private String commStateUnit;
    /**
     * 超声停障  0正常 1停障 单位
     */
    private String avoidingStateUnit;
    /**
     * 驱动状态 0正常 1异常 单位
     */
    private String driverStateUnit;
    /**
     * 故障报警 0正常 1报警 单位
     */
    private String alarmStateUnit;
    /**
     * 运行状态：1空闲状态 2巡视状态 3充电状态 4检修状态 单位
     */
    private String runStateUnit;
    /**
     * 控制模式：1任务模式  2紧急定位模式  3后台遥控模式  4手持遥控模式 单位
     */
    private String ctrlModUnit;
    /**
     * 控制权状态 0空闲 1获得 单位
     */
    private String authStateUnit;
    /**
     * 轮转值班状态 0空闲 1值班 单位
     */
    private String dutyStateUnit;
    /**
     * 运行数据更新时间，格式为 yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date runUpdateTime;
    /**
     * 水平速度，值带单位
     */
    private String runSpeed;
    /**
     * 行驶里程，值带单位
     */
    private String runMileage;
    /**
     * 电池电量，值带单位
     */
    private String batteryLeft;
    /**
     * 云台俯仰角，值带单位
     */
    private String ptzAngleFy;
    /**
     * 云台横滚角，值带单位
     */
    private String ptzAngleHg;
    /**
     * 云台偏航角，值带单位
     */
    private String ptzAnglePh;
    /**
     * 充电电流 值带单位
     */
    private String chargeCurrent;
    /**
     * 水平速度，值
     */
    private String runSpeedValue;
    /**
     * 行驶里程，值
     */
    private String runMileageValue;
    /**
     * 电池电量，值
     */
    private String batteryLeftValue;
    /**
     * 云台俯仰角，值
     */
    private String ptzAngleFyValue;
    /**
     * 云台横滚角，值
     */
    private String ptzAngleHgValue;
    /**
     * 云台偏航角，值
     */
    private String ptzAnglePhValue;
    /**
     * 充电电流 值
     */
    private String chargeCurrentValue;
    /**
     * 水平速度，单位
     */
    private String runSpeedUnit;
    /**
     * 行驶里程，单位
     */
    private String runMileageUnit;
    /**
     * 电池电量，单位
     */
    private String batteryLeftUnit;
    /**
     * 云台俯仰角，单位
     */
    private String ptzAngleFyUnit;
    /**
     * 云台横滚角，单位
     */
    private String ptzAngleHgUnit;
    /**
     * 云台偏航角，单位
     */
    private String ptzAnglePhUnit;
    /**
     * 充电电流，单位
     */
    private String chargeCurrentUnit;
    /**
     * 机器人坐标更新时间,格式为 yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date coordinateUpdateTime;
    /**
     * 坐标所属的地图文件
     */
    private String filePath;
    /**
     * 坐标框(像素点),格式:x,y,z,a，其中x、y、z为地图文件的坐标，a为机器人的旋转角度
     */
    private String coordinatePixel;
    /**
     * 坐标框(经纬度),x、y 为经纬度
     */
    private String coordinateGeography;
    /**
     * 微气象数据更新时间，格式为 yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date evnUpdateTime;
    /**
     * 环境温度，值带单位
     */
    private String envTemp;
    /**
     * 环境湿度，值带单位
     */
    private String envHumi;
    /**
     * 风速，值带单位
     */
    private String envWindSpeed;
    /**
     * 雨量，值带单位
     */
    private String envRain;
    /**
     * 风向，值带单位
     */
    private String envWindDirection;
    /**
     * 气压，值带单位
     */
    private String envPressure;
    /**
     * 氧气，值带单位
     */
    private String envO2;
    /**
     * SF6，值带单位
     */
    private String envSf6;
    /**
     * 环境温度，值
     */
    private String envTempValue;
    /**
     * 环境湿度，值
     */
    private String envHumiValue;
    /**
     * 风速，值
     */
    private String envWindSpeedValue;
    /**
     * 雨量，值
     */
    private String envRainValue;
    /**
     * 风向，值
     */
    private String envWindDirectionValue;
    /**
     * 气压，值
     */
    private String envPressureValue;
    /**
     * 氧气，值
     */
    private String envO2Value;
    /**
     * SF6，值
     */
    private String envSf6Value;
    /**
     * 环境温度，单位
     */
    private String envTempUnit;
    /**
     * 环境湿度，单位
     */
    private String envHumiUnit;
    /**
     * 风速，单位
     */
    private String envWindSpeedUnit;
    /**
     * 雨量，单位
     */
    private String envRainUnit;
    /**
     * 风向，单位
     */
    private String envWindDirectionUnit;
    /**
     * 气压，单位
     */
    private String envPressureUnit;
    /**
     * 氧气，单位
     */
    private String envO2Unit;
    /**
     * SF6，单位
     */
    private String envSf6Unit;
    /**
     * 在线状态 0离线，1在线
     */
    private Integer onlineState;
    /**
     * 电机温度
     */
    private String motorTemp;

    /**
     * 地图文件访问Url
     */
    private String filePathUrl;


    public String getEnvWindDirection() {
        return StringUtils.isBlank(envWindDirection) ? "-" : envWindDirection;
    }

    public String getEnvRain() {
        return StringUtils.isBlank(envRain) ? "-" : envRain;
    }

    public String getEnvWindSpeed() {
        return StringUtils.isBlank(envWindSpeed) ? "-" : envWindSpeed;
    }

    public String getEnvHumi() {
        return StringUtils.isBlank(envHumi) ? "-" : envHumi;
    }

    public String getEnvTemp() {
        return StringUtils.isBlank(envTemp) ? "-" : envTemp;
    }

    public String getEnvPressure() {
        return StringUtils.isBlank(envPressure) ? "-" : envPressure;
    }
}
