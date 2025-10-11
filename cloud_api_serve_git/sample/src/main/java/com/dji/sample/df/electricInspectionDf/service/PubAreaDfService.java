package com.dji.sample.df.electricInspectionDf.service;

import com.dji.sample.df.electricInspectionDf.model.PubAreaDfEntity;

import java.util.List;

public interface PubAreaDfService {
    //添加区域id
    boolean addArea(PubAreaDfEntity pubAreaDfEntity);
    //根据场站id查询区域
    List<PubAreaDfEntity> getAreaListBySubCode(String sub_code);
    //更新区域信息
    boolean updateAreaById(PubAreaDfEntity pubAreaDfEntity);
    //删除区域
    boolean deleteAreaById(int id);
}
