package com.df.server.service.his;


import com.df.server.dto.robotDog.AnalyseImageInfo;
import com.df.server.dto.robotDog.AnalyseParamsRecReq;
import com.df.server.entity.his.HisExePointAnalyse;

public interface HisExePointAnalyseService {

    /**
     * 发送智能分析请求
     *
     * @param analyseImageInfo
     */
    void sendAnalyse(AnalyseImageInfo analyseImageInfo);

    /**
     * 点位分析完成处理
     *
     * @param analyseParamsRecReq
     */
    void analyseFinish(AnalyseParamsRecReq analyseParamsRecReq);
}
