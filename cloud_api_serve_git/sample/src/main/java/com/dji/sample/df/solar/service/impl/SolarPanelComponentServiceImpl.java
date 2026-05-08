package com.dji.sample.df.solar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dji.sample.df.solar.dao.SolarPanelComponentMapper;
import com.dji.sample.df.solar.model.entity.SolarPanelComponent;
import com.dji.sample.df.solar.service.SolarPanelComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SolarPanelComponentServiceImpl extends ServiceImpl<SolarPanelComponentMapper, SolarPanelComponent> implements SolarPanelComponentService {

    @Autowired
    private SolarPanelComponentMapper solarPanelComponentMapper;

    @Override
    public List<SolarPanelComponent> selectListBySolarPanelId(String solarPanelId) {

        List<SolarPanelComponent> solarPanelComponents = solarPanelComponentMapper.selectListBySolarPanelId(solarPanelId);
        return solarPanelComponents;

    }

    @Override
    public List<SolarPanelComponent> selectListByComponentId(String componentId) {
        List<SolarPanelComponent> solarPanelComponents = solarPanelComponentMapper.selectListByComponentId(componentId);
        return solarPanelComponents;
    }
}
