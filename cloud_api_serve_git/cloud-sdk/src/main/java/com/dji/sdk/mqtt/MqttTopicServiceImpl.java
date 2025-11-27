package com.dji.sdk.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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

    @Resource
    private MqttPahoMessageDrivenChannelAdapter adapter;

    // 添加同步锁防止竞态条件
    private final Object subscribeLock = new Object();

    @Override
    public void subscribe(String... topics) {
        synchronized (subscribeLock) {
            String[] subscribedTopics = getSubscribedTopic();
            Set<String> topicSet = subscribedTopics != null ?
                    new HashSet<>(Arrays.asList(subscribedTopics)) : new HashSet<>();

            for (String topic : topics) {
                if (topicSet.contains(topic)) {
                    log.debug("主题已订阅，跳过: {}", topic);
                    continue;
                }
                subscribe(topic, 1);
            }
        }
    }

    @Override
    public void subscribe(String topic, int qos) {
        synchronized (subscribeLock) {
            String[] subscribedTopics = getSubscribedTopic();
            Set<String> topicSet = subscribedTopics != null ?
                    new HashSet<>(Arrays.asList(subscribedTopics)) : new HashSet<>();

            if (topicSet.contains(topic)) {
                log.debug("主题已订阅，跳过: {}", topic);
                return;
            }
            log.info("订阅主题: {}", topic);
            adapter.addTopic(topic, qos);
        }
    }

    @Override
    public void unsubscribe(String... topics) {
        synchronized (subscribeLock) {
            log.info("取消订阅主题: {}", Arrays.toString(topics));
            adapter.removeTopic(topics);
        }
    }

    public String[] getSubscribedTopic() {
        try {
            String[] topics = adapter.getTopic();
            return topics != null ? topics : new String[0];
        } catch (Exception e) {
            log.warn("获取已订阅主题失败: {}", e.getMessage());
            return new String[0];
        }
    }
}
