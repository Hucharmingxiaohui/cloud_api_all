package com.dji.sample.df.electricInspectionDf.service;

import com.df.server.dto.robotDog.AnalyseParamsRecReq;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface ResultService {

    void handleUavResult(Map<String,String> map,String workspaceId, String jobId) throws Exception;

    void analyseFinish(AnalyseParamsRecReq analyseParamsRecReq);

    void updatePointNum(String taskPatrolledId);

}
