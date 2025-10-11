package com.dji.sample.df.electricInspectionDf.service;

import com.dji.sample.df.electricInspectionDf.model.PubBayDfEntity;

import java.util.List;

public interface PubBayDfService {

    //新增间隔
    boolean addBay(PubBayDfEntity pubBayDfEntity);
    List<PubBayDfEntity> getBayListByAreaId(String area_id);
    boolean updateBayById(PubBayDfEntity pubBayDfEntity);
    boolean deleteBayById(int id);
}
