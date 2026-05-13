package com.dji.sample.df.solar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dji.sample.df.solar.model.entity.SolarPanel;
import com.dji.sample.df.solar.model.entity.SolarPanelComponent;

import java.util.List;

public interface SolarPanelComponentService extends IService<SolarPanelComponent> {

    List<SolarPanelComponent> selectListBySolarPanelId(String solarPanelId);
    List<SolarPanelComponent> selectListByComponentId(String componentId);
    List<SolarPanelComponent> selectListByOrthophotoId(String componentId);
}
