package com.dji.sample.df.solar.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.solar.model.entity.SolarPanel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SolarPanelMapper extends BaseMapper<SolarPanel> {

    List<SolarPanel> selectListByMap(Map map);

    int selectListCount(Map map);
}
