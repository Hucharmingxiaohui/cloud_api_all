package com.dji.sample.df.wind.service;

import com.df.server.dto.uniPoint.UniPointImportExcel;
import com.dji.sample.df.wind.model.entity.UniPointImportExcel2;

public interface ImportPointService {

    void importPoint(UniPointImportExcel2 point);
}
