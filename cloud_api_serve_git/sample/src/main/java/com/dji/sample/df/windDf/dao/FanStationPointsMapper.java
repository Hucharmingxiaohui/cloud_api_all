package com.dji.sample.df.windDf.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.windDf.model.entity.FanStationPoints;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface FanStationPointsMapper extends BaseMapper<FanStationPoints> {

    List<FanStationPoints> selectListById(Map map);

    int deleteByDeviceId(String id);

    int selectListCount(Map map);
}
