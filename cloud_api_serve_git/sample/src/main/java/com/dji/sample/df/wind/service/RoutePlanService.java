package com.dji.sample.df.wind.service;

import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;

import java.util.Map;

public interface RoutePlanService {
//     void flyToFront(String name, Double yaw);
//   飞向中间点
     void flyToWayline(String name,Double value);
//   飞向中间点
     void nextTopWayline(String waylineId);
//   不停机巡检
     void workingWayline(String name,String value);
//   停机巡检
     void stopWayline(String name, Double yaw,String value);
//   风机飞向顶端航线
     Map<String,Object> buildFanWayline(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity);
//   风机兴趣点环绕航线
     Map<String,Object> buildInterestPointWayline(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity);
//   光伏板航线
     Map<String,Object> buildSolarPanelWayline(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity);
}
