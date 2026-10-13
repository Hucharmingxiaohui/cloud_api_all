package com.dji.sample.df.cqDockDf.model.dto;

import lombok.Data;

@Data
public class CqDockTaskDispatchResult {
    private boolean success;
    private String taskCode;
    private String taskName;
    private String bayId;
    private String routeId;
    private String euaTaskId;
    private Integer responseCode;
    private String responseMsg;

    public static CqDockTaskDispatchResult failed(String taskCode, String taskName, String bayId, String routeId, String responseMsg) {
        CqDockTaskDispatchResult result = new CqDockTaskDispatchResult();
        result.setSuccess(false);
        result.setTaskCode(taskCode);
        result.setTaskName(taskName);
        result.setBayId(bayId);
        result.setRouteId(routeId);
        result.setResponseMsg(responseMsg);
        return result;
    }
}
