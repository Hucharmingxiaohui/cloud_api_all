package com.dji.sample.df.electricInspectionDf.service;

import com.df.server.dto.robotDog.AnalyseParamsRecReq;

import javax.servlet.http.HttpServletResponse;

public interface ResultService {

    void handleUavResult(String workspaceId,String jobId,HttpServletResponse response) throws Exception;

    void analyseFinish(AnalyseParamsRecReq analyseParamsRecReq);

    void updatePointNum(String taskPatrolledId);

}
