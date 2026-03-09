package com.dji.sample.df.solar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dji.sample.df.solar.model.entity.SolarPanel;

import java.util.Map;

public interface SolarPanelService extends IService<SolarPanel> {

    boolean saveSolarPanel(SolarPanel solarPanel);
    boolean updateSolarPanelById(SolarPanel solarPanel);
    boolean removeSolarPanelById(Long id);
    SolarPanel getSolarPanelById(Long id);
    Map<String, Object> selectList(Map<String, Object> params);
}
