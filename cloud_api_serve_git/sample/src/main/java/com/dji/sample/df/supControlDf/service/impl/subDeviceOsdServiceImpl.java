package com.dji.sample.df.supControlDf.service.impl;


import com.dji.sample.df.supControlDf.entity.Dock2osd;
import com.dji.sample.df.supControlDf.entity.DroneOsd;
import com.dji.sample.df.supControlDf.service.SubDeviceOsd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
//机场2
@Service
@Slf4j
public class subDeviceOsdServiceImpl implements SubDeviceOsd {
    //机场osd
    @Override
    public Dock2osd subDock2Osd(String dock_sn) {
        Dock2osd dock2osd=new Dock2osd();
        // 设置属性值
        dock2osd.setAccumulatedRunningTime(12345L);
        dock2osd.setActivationTime(67890L);
        dock2osd.setNetworkState(100.5);
        dock2osd.setMissions(10);
        dock2osd.setRemainFiles(5);
        dock2osd.setWindSpeed(15.5);
        dock2osd.setRainFull(1);
        dock2osd.setEnvironmentTemperature(25.0);
        dock2osd.setDockTemperature(22.0);
        dock2osd.setHumidity(60.0);
        dock2osd.setWorkingVoltage(11.1);
        dock2osd.setWorkingCurrent(2.5);
        dock2osd.setDroneInDock(1);
        dock2osd.setLongitude(120.0);
        dock2osd.setLatitude(30.0);
        dock2osd.setHeight(100.0);
        return dock2osd;
    }
    //无人机osd

    @Override
    public DroneOsd subDroneOsd(String drone_sn) {
        DroneOsd droneOsd=new DroneOsd();
        droneOsd.setCapacityPercent(85);
        droneOsd.setRtkNum(4);
        droneOsd.setGpsNum(2);
        droneOsd.setHorizontalSpeed(10.5);
        droneOsd.setVerticalSpeed(5.3);
        droneOsd.setWindSpeed(3.2);
        droneOsd.setLongitude(120.0);
        droneOsd.setLatitude(30.0);
        droneOsd.setAsHeight(100.0);
        droneOsd.setAlHeight(50.0);
        droneOsd.setHome_distance(15.0);
        return droneOsd;
    }



}
