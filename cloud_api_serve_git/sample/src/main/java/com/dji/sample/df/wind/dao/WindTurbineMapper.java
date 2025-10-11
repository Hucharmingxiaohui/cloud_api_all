package com.dji.sample.df.wind.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface WindTurbineMapper extends BaseMapper<WindTurbine> {

    List<WindTurbine> selectList(Map map);
}
