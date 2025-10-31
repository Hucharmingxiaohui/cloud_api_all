package com.dji.sample.df.wind.service;

public interface RoutePlanService {
//     void flyToFront(String name, Double yaw);
     void flyToWayline(String name,Double value);
     void workingWayline(String name);
     void stopWayline(String name, Double yaw);
}
