package com.dji.sample.control.service;

import com.dji.sample.control.model.param.InFlightWaylineDeliverParam;
import com.dji.sample.wayline.model.param.CreateJobParam;
import com.dji.sdk.common.HttpResultResponse;

public interface IControlService2 {
    //  空中下发航线
    HttpResultResponse inFlightWaylineDeliver(String sn, InFlightWaylineDeliverParam param, CreateJobParam createJobParam);

}
