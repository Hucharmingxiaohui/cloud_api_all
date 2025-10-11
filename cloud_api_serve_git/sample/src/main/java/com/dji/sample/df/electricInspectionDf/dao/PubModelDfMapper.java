package com.dji.sample.df.electricInspectionDf.dao;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.electricInspectionDf.model.PubModelDfEntity;

public interface PubModelDfMapper extends BaseMapper<PubModelDfEntity> {

    PubModelDfEntity selectByModelName(String fileName);
}
