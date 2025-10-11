package com.dji.sample.df.electricInspectionDf.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.electricInspectionDf.model.PubSubstationDfEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface PubSubstationDfMapper extends BaseMapper<PubSubstationDfEntity> {

    List<PubSubstationDfEntity> getSubstationsByParam(Map<String,Object> map);

    int getSubstationsByParamCount(Map <String,Object> map);
}
