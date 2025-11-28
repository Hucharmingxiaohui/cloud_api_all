package com.dji.sample.airsense.service.impl;

import com.dji.sdk.cloudapi.airsense.AirsenseWarning;
import com.dji.sdk.cloudapi.airsense.api.AbstractAirsenseService;
import com.dji.sdk.mqtt.MqttReply;
import com.dji.sdk.mqtt.events.TopicEventsRequest;
import com.dji.sdk.mqtt.events.TopicEventsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SDKAirsenseService extends AbstractAirsenseService {

    @Override
    public TopicEventsResponse<MqttReply> airsenseWarning(TopicEventsRequest<List<AirsenseWarning>> request, MessageHeaders headers) {
        // 空实现，只记录日志，不抛出异常
        log.debug("收到航空感知警告，数量: {}", request.getData().size());

        // 返回空的成功响应
        return new TopicEventsResponse<MqttReply>()
                .setData(MqttReply.success());
    }
}
