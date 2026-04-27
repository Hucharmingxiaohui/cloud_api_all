package com.dji.sample.df.solar.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dji.sample.df.solar.model.entity.SolarPanelArea;

import java.util.Map;

public interface SolarPanelAreaService extends IService<SolarPanelArea> {

    boolean saveSolarPanelArea(SolarPanelArea solarPanelArea);
    boolean updateSolarPanelAreaById(SolarPanelArea solarPanelArea);
    boolean removeSolarPanelAreaById(Long id);
    SolarPanelArea getSolarPanelAreaById(Long id);
    Map<String, Object> selectList(Map<String, Object> params);
}
