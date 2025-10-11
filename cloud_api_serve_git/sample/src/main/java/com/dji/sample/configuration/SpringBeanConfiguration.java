package com.dji.sample.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Administrator
 */
@Configuration
public class SpringBeanConfiguration {

    @Bean
    @Primary
//    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        //确保创建的 ObjectMapper 仅用于 JSON 处理，而不是 XML。
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        //设置属性命名策略为 SNAKE_CASE，这意味着 Java 对象中的驼峰命名属性在序列化为 JSON 时会转换为下划线命名
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        //设置序列化时仅包含非 null 的属性，这意味着如果某个属性的值为 null，它不会被包含在 JSON 输出中
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //创建一个 JavaTimeModule 用于处理 Java 8 的日期和时间类型。
        JavaTimeModule timeModule = new JavaTimeModule();
        //添加自定义的序列化器和反序列化器，以便 LocalDateTime 类型可以按照指定的格式 "yyyy-MM-dd HH:mm:ss" 进行序列化和反序列化
        timeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        objectMapper.disable(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS);
        objectMapper.registerModules(timeModule);
        //设置为 false，使得在反序列化时，如果 JSON 中有未知属性，不会抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //使得在序列化空 Bean（没有属性的对象）时，不会抛出异常。
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        //允许 JSON 字符串中使用单引号
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        //允许属性名在反序列化时大小写不敏感
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        //允许将空字符串反序列化为 null 对象
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        //自定义 null 值的序列化行为，当属性值为 null 时，序列化为空字符串 "" 而不是默认的 null
        objectMapper.getSerializerProvider().setNullValueSerializer(new JsonSerializer<Object>() {
            @Override
            public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString("");
            }
        });
        return objectMapper;
    }
}
