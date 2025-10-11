package com.df.server.dto.robotDog;

import lombok.Data;

import java.util.List;

/**
 * 智能分析图像信息
 */
@Data
public class AnalyseImageInfo {

    private String requestId;

    private String cameraCode;

    private String presetNo;

    private String pointCode;

    private String pointName;

    private TaskInfo taskInfo;

    private Integer taskType;

    List<AnalyseParamsReq> analyseParamsReqList;

    private Integer delayTime;
}
