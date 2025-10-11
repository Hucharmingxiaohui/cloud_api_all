package com.dji.sample.df.electricInspectionDf.service;

import com.dji.sample.df.electricInspectionDf.model.PubSubstationDfEntity;

import java.util.List;
import java.util.Map;

public interface PubSubstationDfService {
    //新增一个场站
    Integer addPubStation(PubSubstationDfEntity pubSubstationDfEntity);
    //查询所有的场站
    List<PubSubstationDfEntity> getPubStationList();
    //根据运维班查询场站,工作空间id
    List<PubSubstationDfEntity> getPubStationByWorkspaceId(String workspace_id);
    //根据id删除场站
    boolean deleteSubStationById(Integer id);
    //根据id更新场站
    boolean updateSubSationById(PubSubstationDfEntity pubSubstationDfEntity);

    Map getSubstationsByParam(Map <String,Object> map);

}
