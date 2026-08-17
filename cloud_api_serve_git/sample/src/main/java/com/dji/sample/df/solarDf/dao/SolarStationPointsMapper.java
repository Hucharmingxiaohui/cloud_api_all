package com.dji.sample.df.solarDf.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.solarDf.model.entity.SolarStationPoints;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SolarStationPointsMapper extends BaseMapper<SolarStationPoints> {

    List<SolarStationPoints> selectListById(Map map);

    int selectListCount(Map map);
}
