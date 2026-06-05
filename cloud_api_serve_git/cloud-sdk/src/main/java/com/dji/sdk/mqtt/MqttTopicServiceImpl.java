package com.dji.sdk.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *  消息订阅
 * @author sean.zhou
 * @date 2021/11/10
 * @version 0.1
 */
@Component
public class MqttTopicServiceImpl implements IMqttTopicService {

    private static final Logger log = LoggerFactory.getLogger(MqttTopicServiceImpl.class);

    @Autowired(required = false)
    @Qualifier("mqttInbound")
    private MqttPahoMessageDrivenChannelAdapter adapter;

    @Override
    public void subscribe(String... topics) {
        if (adapter == null) {
            return;
        }
        Set<String> topicSet = new HashSet<>(Arrays.asList(getSubscribedTopic()));
        for (String topic : topics) {
            if (topicSet.contains(topic)) {
                return;
            }
            subscribe(topic, 1);
        }
    }

    @Override
    public void subscribe(String topic, int qos) {
        if (adapter == null) {
            return;
        }
        Set<String> topicSet = new HashSet<>(Arrays.asList(getSubscribedTopic()));
        if (topicSet.contains(topic)) {
            return;
        }
        log.debug("subscribe topic: {}", topic);
        adapter.addTopic(topic, qos);
    }

    @Override
    public void unsubscribe(String... topics) {
        if (adapter == null) {
            return;
        }
        log.debug("unsubscribe topic: {}", Arrays.toString(topics));
        adapter.removeTopic(topics);
    }

    public String[] getSubscribedTopic() {
        if (adapter == null) {
            return new String[0];
        }
        return adapter.getTopic();
    }
}
