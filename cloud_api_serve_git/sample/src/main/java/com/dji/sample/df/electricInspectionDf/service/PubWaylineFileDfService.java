package com.dji.sample.df.electricInspectionDf.service;

import com.dji.sample.df.electricInspectionDf.model.PubWaylineFileDfEntity;

import java.util.List;

public interface PubWaylineFileDfService {
    //导入航线关联信息
    Integer addPubWayline(PubWaylineFileDfEntity pubWaylineFileDfEntity);
    //按场站专业查询航线关联信息
    List<PubWaylineFileDfEntity> getPubWaylineBySubCodeMajor(String sub_code,String major);
}
