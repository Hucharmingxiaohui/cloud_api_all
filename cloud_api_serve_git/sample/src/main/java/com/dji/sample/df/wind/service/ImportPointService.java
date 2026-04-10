package com.dji.sample.df.wind.service;

import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.wind.model.entity.UniPointImportExcel2;

import java.util.List;
import java.util.Map;

public interface ImportPointService {

    void importPoint(UniPointImportExcel2 point);

    Map<String,Object> selectList(Map map);

    public int batchDelete(List<Integer> ids);

    public UniPoint getPointByCode(String subCode, String pointCode);
}
