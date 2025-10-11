package com.dji.sample.df.supControlDf.service;

import com.dji.sample.df.supControlDf.entity.Dock2osd;
import com.dji.sample.df.supControlDf.entity.DroneOsd;

public interface SubDeviceOsd {
    Dock2osd subDock2Osd(String dock_sn);
    DroneOsd subDroneOsd(String drone_sn);
}
