package com.dji.sample.df.kmzDf.service;


import com.dji.sample.df.kmzDf.entity.wayline.Wayline;

import java.io.IOException;
import java.net.URL;

public interface WaylineKmzService {

    public String CreateWaypointWaylineKmz(String workspaceId, Wayline wayline,String creator) throws IOException;//航点航线规划
    public Wayline GetKmzWaypointWayline(String workspaceId, String  waylineId, URL url,String targetFolder) throws Exception;//解析航线

}
