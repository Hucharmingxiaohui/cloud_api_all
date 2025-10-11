package com.dji.sample.df.thirdKmzDf.service;


import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointResult;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Wayline;

import java.net.URL;

public interface WaylineKmzThirdService {

     Wayline GetKmzWaypointWayline(String workspaceId, String  waylineId, URL url,String targetFolder) throws Exception;//解析航线
     //获取每个点位的航点照片数量
     PointResult getPointResult(String workspaceId, String  waylineId, URL url,String targetFolder) throws Exception;

}
