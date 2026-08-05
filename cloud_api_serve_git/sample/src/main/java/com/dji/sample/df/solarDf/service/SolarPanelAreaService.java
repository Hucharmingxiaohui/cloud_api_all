package com.dji.sample.df.solarDf.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.df.framework.vo.Result;
import com.dji.sample.df.solarDf.model.dto.SolarDetectRequestDTO;
import com.dji.sample.df.solarDf.model.entity.SolarPanelArea;

import java.util.Map;

public interface SolarPanelAreaService extends IService<SolarPanelArea> {

    boolean saveSolarPanelArea(SolarPanelArea solarPanelArea);
    boolean updateSolarPanelAreaById(SolarPanelArea solarPanelArea);
    boolean removeSolarPanelAreaById(String id);
    SolarPanelArea getSolarPanelAreaById(Long id);
    Map<String, Object> selectList(Map<String, Object> params);
    Result detectAreaGenSolar(SolarDetectRequestDTO solarDetectRequestDTO);
}
