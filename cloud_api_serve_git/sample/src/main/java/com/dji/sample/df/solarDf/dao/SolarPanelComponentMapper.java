package com.dji.sample.df.solarDf.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.solarDf.model.entity.SolarPanelComponent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SolarPanelComponentMapper extends BaseMapper<SolarPanelComponent> {

    List<SolarPanelComponent> selectListBySolarPanelId(@Param("solarPanelId") String solarPanelId);

    int selectCountByOrthophotoId(@Param("orthophotoId") String orthophotoId);

    List<SolarPanelComponent> selectListByComponentId(@Param("componentId") String componentId);

    List<SolarPanelComponent> selectListByOrthophotoId(@Param("orthophotoId") String orthophotoId);

}
