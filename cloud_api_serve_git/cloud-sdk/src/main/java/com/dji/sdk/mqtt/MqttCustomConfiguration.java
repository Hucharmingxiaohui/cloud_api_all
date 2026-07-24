package com.dji.sdk.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.UUID;

@Configuration
@ConditionalOnProperty(value = "mqtt.CUSTOM.enabled", havingValue = "true", matchIfMissing = true)
public class MqttCustomConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MqttCustomConfiguration.class);

    @Value("${mqtt.CUSTOM.inbound-topic:}")
    private String customInboundTopic;

    @Resource
    @Qualifier("customMqttClientFactory")
    private MqttPahoClientFactory customMqttClientFactory;

    @Resource(name = ChannelName.CUSTOM_INBOUND)
    private MessageChannel customInboundChannel;

    @Bean
    public MqttPahoMessageDrivenChannelAdapter customMqttInbound() {
        String[] topics = java.util.Arrays.stream(customInboundTopic.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        log.info("[cq-dock] CUSTOM mqtt subscribe topics: {}", java.util.Arrays.toString(topics));
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                UUID.randomUUID().toString(), customMqttClientFactory, topics);
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(true);
        adapter.setConverter(converter);
        adapter.setQos(1);
        adapter.setOutputChannel(customInboundChannel);
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = ChannelName.CUSTOM_OUTBOUND)
    public MessageHandler customMqttOutbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(
                UUID.randomUUID().toString(), customMqttClientFactory);
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(true);
        messageHandler.setAsync(true);
        messageHandler.setDefaultQos(0);
        messageHandler.setConverter(converter);
        return messageHandler;
    }

    @Bean
    @ServiceActivator(inputChannel = ChannelName.CUSTOM_DEFAULT)
    public MessageHandler customDefaultInboundHandler() {
        return message -> {
            log.info("Custom channel default handler." +
                    "\nTopic: " + message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC) +
                    "\nPayload: " + message.getPayload() + "\n");
        };
    }
}
