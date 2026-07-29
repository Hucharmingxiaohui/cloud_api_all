package com.dji.sdk.mqtt;

import com.dji.sdk.common.Common;
import com.dji.sdk.mqtt.requests.TopicRequestsRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Client configuration for inbound messages.
 * @author sean.zhou
 * @date 2021/11/10
 * @version 0.1
 */
@Configuration
@ConditionalOnProperty(value = "cloud-sdk.mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MqttConfiguration.class);

    @Value("${cloud-sdk.mqtt.inbound-topic: }")
    private String inboundTopic;

    @Value("${cloud-sdk.mqtt.default-log-ignore-methods:}")
    private String defaultLogIgnoreMethods;

    @Resource
    private MqttPahoClientFactory mqttClientFactory;

    @Resource(name = ChannelName.INBOUND)
    private MessageChannel inboundChannel;

    /**
     * Clients of inbound message channels.
     * @return
     */
    @Bean
    public MqttPahoMessageDrivenChannelAdapter mqttInbound() {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                UUID.randomUUID().toString(), mqttClientFactory, inboundTopic.split(","));
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        // use byte types uniformly
        converter.setPayloadAsBytes(true);
        adapter.setConverter(converter);
        adapter.setQos(1);
        adapter.setOutputChannel(inboundChannel);
        return adapter;
    }

    /**
     * Clients of outbound message channels.
     * @return
     */
    @Bean
    @ServiceActivator(inputChannel = ChannelName.OUTBOUND)
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(
                UUID.randomUUID().toString(), mqttClientFactory);
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        // use byte types uniformly
        converter.setPayloadAsBytes(true);

        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(0);
        messageHandler.setConverter(converter);
        return messageHandler;
    }



    /**
     * Define a default channel to handle messages that have no effect.
     * @return
     */
    @Bean
    @ServiceActivator(inputChannel = ChannelName.DEFAULT)
    public MessageHandler defaultInboundHandler() {
        return message -> {
            if (shouldIgnoreDefaultLog(message.getPayload())) {
                return;
            }
            log.info("The default channel does not handle messages." +
                    "\nTopic: " + message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC) +
                    "\nPayload: " + message.getPayload() + "\n");
        };
    }

    private boolean shouldIgnoreDefaultLog(Object payload) {
        Set<String> ignoredMethods = parseDefaultLogIgnoreMethods();
        if (ignoredMethods.isEmpty() || payload == null) {
            return false;
        }
        if (payload instanceof TopicRequestsRequest) {
            return ignoredMethods.contains(((TopicRequestsRequest<?>) payload).getMethod());
        }
        try {
            if (!(payload instanceof byte[])) {
                JsonNode methodNode = Common.getObjectMapper().convertValue(payload, JsonNode.class).findValue("method");
                return methodNode != null && ignoredMethods.contains(methodNode.asText());
            }
            JsonNode methodNode = Common.getObjectMapper().readTree((byte[]) payload).findValue("method");
            return methodNode != null && ignoredMethods.contains(methodNode.asText());
        } catch (Exception e) {
            return false;
        }
    }

    private Set<String> parseDefaultLogIgnoreMethods() {
        if (defaultLogIgnoreMethods == null || defaultLogIgnoreMethods.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(defaultLogIgnoreMethods.split(","))
                .map(String::trim)
                .filter(method -> !method.isEmpty())
                .collect(Collectors.toSet());
    }
}
