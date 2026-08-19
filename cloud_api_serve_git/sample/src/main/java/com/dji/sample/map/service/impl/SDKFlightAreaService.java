package com.dji.sample.map.service.impl;

import com.dji.sdk.annotations.CloudSDKVersion;
import com.dji.sdk.cloudapi.flightarea.FlightAreaMethodEnum;
import com.dji.sdk.config.version.CloudSDKVersionEnum;
import com.dji.sdk.config.version.GatewayManager;
import com.dji.sdk.config.version.GatewayTypeEnum;
import com.dji.sdk.mqtt.services.ServicesPublish;
import com.dji.sdk.mqtt.services.ServicesReplyData;
import com.dji.sdk.mqtt.services.TopicServicesResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author sean
 * @version 1.9
 * @date 2023/11/22
 */
@Service
public class SDKFlightAreaService {

    @Resource
    private ServicesPublish servicesPublish;

    @CloudSDKVersion(since = CloudSDKVersionEnum.V1_0_0, exclude = GatewayTypeEnum.RC, include = {GatewayTypeEnum.DOCK, GatewayTypeEnum.DOCK2})
    public TopicServicesResponse<ServicesReplyData> flightAreasUpdate(GatewayManager gateway) {
        return servicesPublish.publish(
                gateway.getGatewaySn(),
                FlightAreaMethodEnum.FLIGHT_AREAS_UPDATE.getMethod());
    }
}
