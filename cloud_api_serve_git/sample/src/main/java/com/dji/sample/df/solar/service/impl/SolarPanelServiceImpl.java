package com.dji.sample.df.solar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dji.sample.df.solar.dao.SolarPanelMapper;
import com.dji.sample.df.solar.model.entity.SolarPanel;
import com.dji.sample.df.solar.service.SolarPanelService;
import org.springframework.stereotype.Service;

@Service
public class SolarPanelServiceImpl extends ServiceImpl<SolarPanelMapper, SolarPanel> implements SolarPanelService {
}
