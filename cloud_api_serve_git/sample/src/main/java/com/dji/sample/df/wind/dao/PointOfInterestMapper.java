package com.dji.sample.df.wind.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.wind.model.entity.PointOfInterest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface PointOfInterestMapper extends BaseMapper<PointOfInterest> {
    List<PointOfInterest> selectList(Map map);

    int selectListCount(Map map);
}
