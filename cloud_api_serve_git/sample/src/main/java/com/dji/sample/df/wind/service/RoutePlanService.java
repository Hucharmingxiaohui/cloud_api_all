package com.dji.sample.df.wind.service;

import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;

public interface RoutePlanService {
//     void flyToFront(String name, Double yaw);
//   飞向中间点
     void flyToWayline(String name,Double value);
//   不停机巡检
     void workingWayline(String name);
//   停机巡检
     void stopWayline(String name, Double yaw);

     boolean buildFanWayline(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity);
}
