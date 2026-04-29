package com.dji.sample.df.solar.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.solar.model.entity.SolarPanel;
import com.dji.sample.df.solar.model.entity.SolarPanelArea;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SolarPanelMapper extends BaseMapper<SolarPanel> {
}
