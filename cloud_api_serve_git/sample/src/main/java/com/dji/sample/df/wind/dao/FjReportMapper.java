package com.dji.sample.df.wind.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.wind.model.entity.FjReportEntity;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FjReportMapper extends BaseMapper<FjReportEntity> {
}
