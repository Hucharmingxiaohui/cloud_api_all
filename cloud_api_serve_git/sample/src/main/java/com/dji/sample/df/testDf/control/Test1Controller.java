package com.dji.sample.df.testDf.control;

import com.dji.sample.df.electricInspectionDf.model.PubSubstationDfEntity;
import com.dji.sample.df.testDf.service.impl.TestService;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.mqtt.services.ServicesPublish;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/test/")
public class Test1Controller {

    @Resource
    TestService testService;

    @Resource
    private ServicesPublish servicesPublish;

    @GetMapping("/query")
    public HttpResultResponse query() {
        List<PubSubstationDfEntity> query = testService.query();
//        FlighttaskExecuteRequest request = new FlighttaskExecuteRequest().setFlightId("jobId");
//        servicesPublish.publish(
//                "8UUXN3U00A046E",
//                WaylineMethodEnum.FLIGHTTASK_EXECUTE.getMethod(),
//                request,
//                request.getFlightId());
        return HttpResultResponse.success(query).setMessage("成功");
    }
}
