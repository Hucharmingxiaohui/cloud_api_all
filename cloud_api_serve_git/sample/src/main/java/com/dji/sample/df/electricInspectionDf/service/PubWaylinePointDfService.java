package com.dji.sample.df.electricInspectionDf.service;

import com.dji.sample.df.electricInspectionDf.model.PubWaylinePointDfEntity;
import com.dji.sdk.common.PaginationData;

import java.util.List;
import java.util.Map;

public interface PubWaylinePointDfService {
    //导入点位台帐
    List<PubWaylinePointDfEntity> savePubWaylinePointDfEntity( String sub_code,List<PubWaylinePointDfEntity> pointsList);//存入点未到数据库
    // 根据ID获取点未信息
    PubWaylinePointDfEntity getPubWaylinePointDfEntityById(Integer id);

    // 更新点未信息
    Integer updatePubWaylinePointDfEntity(PubWaylinePointDfEntity pubWaylinePointDfEntity);

    // 删除点未信息
    Integer deletePubWaylinePointDfEntityById(Integer id);

    // 查询所有点未信息
    PaginationData<PubWaylinePointDfEntity> getAllPubWaylinePointDfEntities(long page, long pageSize);
    //按变电站id查询 sub_code
    List<PubWaylinePointDfEntity> getPointBySubCode(String sub_code);
    //根据航线id,航点序列查询点位信息
    PubWaylinePointDfEntity getPointByWaylineIdAndSequence(String wayline_id,String waypoint_sequence);
    //根据部件id查询点位
    List<PubWaylinePointDfEntity> getPointsByComponentId(String component_id);
    //根据条件查询点位
    Map getPointsByParam(Map map);
}
