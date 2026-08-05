package com.dji.sample.df.substationDf.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.center.entity.UniPoint;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface UniPointMapper2 extends BaseMapper<UniPoint> {

    List<UniPoint> selectListByMap(Map map);

    int selectListCount(Map map);
}
