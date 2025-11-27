package com.dji.sample.map.service.impl;

import com.dji.sdk.cloudapi.map.api.AbstractOfflineMapService;
import com.dji.sdk.cloudapi.property.DockDroneOfflineMapEnable;
import com.dji.sdk.mqtt.MqttReply;
import com.dji.sdk.mqtt.events.TopicEventsResponse;
import com.dji.sdk.mqtt.state.TopicStateRequest;
import com.dji.sdk.mqtt.state.TopicStateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SDKOfflineMapService extends AbstractOfflineMapService {

    public TopicStateResponse<MqttReply> dockDroneOfflineMapEnable(TopicStateRequest<DockDroneOfflineMapEnable> request, MessageHeaders headers) {
        // 空实现，只记录日志，不抛出异常
        log.debug("收到离线地图信息");

        // 返回空的成功响应
        return new TopicStateResponse<MqttReply>()
                .setData(MqttReply.success());
    }
}

