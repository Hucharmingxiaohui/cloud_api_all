package com.dji.sample.df.testDf.dao;

import com.dji.sample.df.electricInspectionDf.model.PubSubstationDfEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TestMapper {
    List<PubSubstationDfEntity> query();
}
