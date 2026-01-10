package com.dji.sample.center.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface UniPointMapper2 extends BaseMapper<UniPoint> {

    List<UniPoint> selectList(Map map);

    int selectListCount(Map map);
}
