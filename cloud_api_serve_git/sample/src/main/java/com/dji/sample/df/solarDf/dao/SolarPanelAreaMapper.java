package com.dji.sample.df.solarDf.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.solarDf.model.entity.SolarPanelArea;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SolarPanelAreaMapper extends BaseMapper<SolarPanelArea> {

    List<SolarPanelArea> selectListByMap(Map map);

    int selectListCount(Map map);
}
