package com.df.framework.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Component
@Configuration
public class WebSocketConfig {
    /**
     * 用途：扫描并注册所有携带@ServerEndpoint注解的实例。 @ServerEndpoint("/loggings")
     * 如果使用外部容器 则无需提供ServerEndpointExporter。
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter serverEndpointExporter = new ServerEndpointExporter();
        //serverEndpointExporter.setAnnotatedEndpointClasses(WebSocketEndpoint.class);
        //logger.info("【初始化websocket】serverEndpointExporter......");
        return serverEndpointExporter;
    }
}