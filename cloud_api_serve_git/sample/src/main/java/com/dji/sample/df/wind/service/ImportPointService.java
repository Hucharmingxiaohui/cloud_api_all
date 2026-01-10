package com.dji.sample.df.wind.service;

import com.df.server.dto.uniPoint.UniPointImportExcel;
import com.dji.sample.df.wind.model.entity.UniPointImportExcel2;

import java.util.Map;

public interface ImportPointService {

    void importPoint(UniPointImportExcel2 point);

    Map<String,Object> selectList(Map map);
}
