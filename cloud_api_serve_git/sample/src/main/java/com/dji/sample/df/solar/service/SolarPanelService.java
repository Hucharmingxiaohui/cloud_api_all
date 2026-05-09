package com.dji.sample.df.solar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dji.sample.df.solar.model.entity.SolarPanel;
import com.dji.sample.df.solar.model.entity.SolarPanelArea;

import java.util.Map;

public interface SolarPanelService extends IService<SolarPanel> {
    boolean parseAndSave(String jsonResponse,String orthophotoId);
    boolean updateSolarPanelById(SolarPanel solarPanel);
    boolean removeSolarPanelById(String id);
    SolarPanel getSolarPanelById(String id);
    Map<String, Object> selectList(Map<String, Object> params);
}
