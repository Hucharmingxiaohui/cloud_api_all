package com.cleaner.djuav.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 子项目专用的 JSON 配置：
 * 1) 命名策略恢复为驼峰（LOWER_CAMEL_CASE）  sample里FlightTaskServicelmpl.java中设置了蛇形命名配置
 * 2) 不做“null -> 空字符串”的奇怪序列化
 * 3) 注册 JavaTimeModule，常用 LocalDateTime 支持
 *
 * 关键点：@Primary 确保 MVC 首选用这个 ObjectMapper
 */
@Configuration
public class WaylineJacksonConfig {

//    @Bean
//      // ★ 让它成为首选，覆盖父工程的蛇形命名配置

    @Bean("waylineObjectMapper")   // 不要 @Primary
    public ObjectMapper waylineObjectMapper() {
        ObjectMapper om = new ObjectMapper();

        // 驼峰命名（与子dfe-wayline”单独运行时的行为一致）
        om.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);

        // 常用、稳妥的 JSON 行为
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // 如需日期反序列化/序列化支持
        om.registerModule(new JavaTimeModule());
        // 如需固定格式，可加自定义(可选)：
        // JavaTimeModule time = new JavaTimeModule();
        // time.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // time.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // om.registerModule(time);

        // 不做空字符串/大小写不敏感等“过度宽松”的全局改造，避免把空值“吃掉”
        // （父工程里那些 ACCEPT_EMPTY_STRING_AS_NULL_OBJECT、nullValueSerializer 等，这里都不启用）

        return om;
    }
}
