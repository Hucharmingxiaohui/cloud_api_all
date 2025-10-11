package com.dji.sdk.mqtt;

import com.dji.sdk.common.SpringBeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author sean.zhou
 * @date 2021/11/10
 * @version 0.1
 */
@Component
public class InboundMessageRouter extends AbstractMessageRouter {

    private static final Logger log = LoggerFactory.getLogger(InboundMessageRouter.class);

    /**
     * All mqtt broker messages will arrive here before distributing them to different channels.
     * @param message message from mqtt broker
     * @return channel
     * All mqtt broker messages will arrive here before distributing them to different channels.
     * @param message message from mqtt broker
     * @return channel
     *
     * 全部节点的信息都会先从这里过，然后再查询TopicEnum中的方法，寻找到相应的通道（也就是代码中已经注册的Channel）
     * 举个例子：就比如我现在使用MQTTX向 mysys/envents_test （broker）发送一个消息
     * 首先会经过这里，然后我们根据 mysys/envents_test 在 TopicEnum.find(topic) 寻找，
     * 找到相应的通道为：STATE_ENVENTS(Pattern.compile("^"+MY_BASIC_PRE+ENVENTS_TEST+"$"), ChannelName.ENVENTS_INBOUND_TEST),
     * 即找到一个 ChannelName 为 ENVENTS_INBOUND_TEST 通道（这个通道我们已经注册在Spring 中啦）
     *
     * 找到这个通道后，我们会将消息投递到这个通道去
     * 接下来就是看是谁订阅了这个通道的消息，那么就会接着处理这个消息
     * 比如我们的案例中是由 EnventsTestRouter 这个二级路由订阅了消息通道，来进行消息的再次分发，
     * 在这里的时候，EnventsTestRouter 可以不再是根据节点的名称来进行处理，而是具体的消息来进行二次处理，比如指定要判断消息中的某一个字段是什么
     * 从而再交由谁处理（即再次投递到哪个 ChannelName 中去）
     * 补充：所有的入站信息，都会率先经过这里。determineTargetChannels的实际作用并非是分发，而是找到需要接收的Channel（信道），
     * 具体的调用是在上层的抽象类 AbstractMessageRouter.handleMessageInternal 方法内，具体的分发也是在这个方法的下半部分。
     */
    @Override
    @Router(inputChannel = ChannelName.INBOUND)
    protected Collection<MessageChannel> determineTargetChannels(Message<?> message) {
        MessageHeaders headers = message.getHeaders();
        String topic = headers.get(MqttHeaders.RECEIVED_TOPIC).toString();
        byte[] payload = (byte[])message.getPayload();

        log.debug("received topic: {} \t payload =>{}", topic, new String(payload));

        CloudApiTopicEnum topicEnum = CloudApiTopicEnum.find(topic);
        MessageChannel bean = (MessageChannel) SpringBeanUtils.getBean(topicEnum.getBeanName());

        return Collections.singleton(bean);
    }
}
