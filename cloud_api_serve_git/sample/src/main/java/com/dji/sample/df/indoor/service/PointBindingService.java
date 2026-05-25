package com.dji.sample.df.indoor.service;


import com.dji.sample.df.indoor.model.dto.PointBindingRequest;
import com.dji.sample.df.indoor.model.dto.PointBindingResponse;

import java.util.List;

public interface PointBindingService {
    PointBindingResponse addPoint(PointBindingRequest request);
    PointBindingResponse updatePoint(String id, PointBindingRequest request);
    boolean deletePoint(String id);
    PointBindingResponse getPointById(String id);
    List<PointBindingResponse> getAllPoints();
}
