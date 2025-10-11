//package com.dji.sample.df.patrolDf.config.redis;
//
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.listener.RedisMessageListenerContainer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//@Configuration
//public class RedisConfig {
//
//    @Bean
//    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory
//    ) {
//        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
//        //配置监听key过期事件
//        container.setConnectionFactory(connectionFactory);
//        // 所有的订阅消息，都需要在这里进行注册绑定,new PatternTopic(TOPIC_NAME1)表示发布的主题信息
//        //container.addMessageListener(ycListener, new PatternTopic(ycDataTopic));
//        //container.addMessageListener(yxListener, new PatternTopic(yxDataTopic));
//        //container.addMessageListener(redisKeyExpirationListener,  new PatternTopic("__keyevent@0__:expired"));
//        /**
//         * 后期发布消息功能时 订阅方收不到消息 可参考此代码解决
//         * 设置序列化对象
//         * 特别注意：1. 发布的时候需要设置序列化；订阅方也需要设置序列化
//         *         2. 设置序列化对象必须放在[加入消息监听器]这一步后面，否则会导致接收器接收不到消息
//         */
//        /*Jackson2JsonRedisSerializer seria = new Jackson2JsonRedisSerializer(Object.class);
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//        seria.setObjectMapper(objectMapper);
//        container.setTopicSerializer(seria);*/
//        return container;
//    }
//
//    @Bean(name = "redisTemplate")
//    public RedisTemplate<String, Object> getRedisTemplate(RedisConnectionFactory factory) {
//        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
//        redisTemplate.setConnectionFactory(factory);
//        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
//        // key的序列化类型
//        redisTemplate.setKeySerializer(stringRedisSerializer);
//        /*Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        // 方法过期，改为下面代码
//        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
//                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
//        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
//        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);*/
//        redisTemplate.setValueSerializer(stringRedisSerializer);
//        redisTemplate.setHashKeySerializer(stringRedisSerializer);
//        redisTemplate.setHashValueSerializer(stringRedisSerializer);
//        redisTemplate.afterPropertiesSet();
//        return redisTemplate;
//    }
//}
