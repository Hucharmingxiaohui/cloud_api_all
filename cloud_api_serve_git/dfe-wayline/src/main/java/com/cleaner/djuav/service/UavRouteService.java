package com.cleaner.djuav.service;

import com.cleaner.djuav.domain.UavRouteReq;
import com.cleaner.djuav.domain.kml.KmlInfo;

import java.io.IOException;

public interface UavRouteService {

    /**
     * 编辑kmz文件
     */
    void updateKmz(UavRouteReq uavRouteReq);

    /**
     * 生成kmz文件(带航点)
     */
    void buildKmz(UavRouteReq uavRouteReq);

    String buildKmzAndReturnPath(UavRouteReq uavRouteReq); // ← 新增 xtj

    /**
     * 解析kmz文件
     *
     * @param file
     */
    KmlInfo parseKmz(String file) throws IOException;
}
