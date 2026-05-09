package com.dji.sample.df.solar.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.solar.model.entity.SolarPanelComponent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SolarPanelComponentMapper extends BaseMapper<SolarPanelComponent> {

    List<SolarPanelComponent> selectListBySolarPanelId(@Param("solarPanelId") String solarPanelId);

    int selectCountByOrthophotoId(@Param("orthophotoId") String orthophotoId);

    List<SolarPanelComponent> selectListByComponentId(@Param("componentId") String componentId);

}
