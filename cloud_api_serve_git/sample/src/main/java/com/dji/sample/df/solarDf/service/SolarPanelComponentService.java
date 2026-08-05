package com.dji.sample.df.solarDf.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dji.sample.df.solarDf.model.entity.SolarPanelComponent;

import java.util.List;

public interface SolarPanelComponentService extends IService<SolarPanelComponent> {

    List<SolarPanelComponent> selectListBySolarPanelId(String solarPanelId);
    List<SolarPanelComponent> selectListByComponentId(String componentId);
    List<SolarPanelComponent> selectListByOrthophotoId(String componentId);
}
