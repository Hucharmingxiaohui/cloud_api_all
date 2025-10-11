package com.dji.sdk.common;

import com.dji.sdk.config.version.GatewayManager;
import com.dji.sdk.exception.CloudSDKErrorEnum;
import com.dji.sdk.exception.CloudSDKException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * JSON  序列化工具
 *
 * @author sean
 * @version 1.7
 * @date 2023/5/23
 */
public class Common {

    private static final JsonMapper.Builder MAPPER_BUILDER = JsonMapper.builder();

    static {
        //用于支持 Java 8 的日期和时间类型（如 LocalDateTime）的序列化和反序列化。
        //LocalDateTimeDeserializer 和 LocalDateTimeSerializer：自定义 LocalDateTime 的序列化和反序列化格式为 yyyy-MM-dd HH:mm:ss。
        JavaTimeModule timeModule = new JavaTimeModule();
        timeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        MAPPER_BUILDER
                //设置属性命名策略为 SNAKE_CASE，即字段名在 JSON 中以下划线分隔的形式出
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                //设置为 NON_ABSENT，表示序列化时忽略 null 和 Optional.empty() 的值。
                .serializationInclusion(JsonInclude.Include.NON_ABSENT)
                //禁用重复模块注册的检查。
                .disable(MapperFeature.IGNORE_DUPLICATE_MODULE_REGISTRATIONS)
                .addModule(timeModule)
                //反序列化时遇到未知属性不报错
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                //序列化空 Bean 不报错
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                //允许 JSON 中使用单引号
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                //允许属性名大小写不敏感
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                //将空字符串视为 null 对象
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }

    public static void validateModel(BaseModel model) {
        if (null == model) {
            throw new CloudSDKException(CloudSDKErrorEnum.INVALID_PARAMETER, "Param must not be null.");
        }
        model.valid();
    }

    public static void validateModel(BaseModel model, GatewayManager gateway) {
        if (null == model) {
            throw new CloudSDKException(CloudSDKErrorEnum.INVALID_PARAMETER, "Param must not be null.");
        }
        model.valid(gateway);
    }

    public static ObjectMapper getObjectMapper() {
        return MAPPER_BUILDER.build();
    }

    /**
     * 将输入的字符串从蛇形命名法（snake_case）转换为驼峰命名法（camelCase）
     * @param key
     * @return
     */
    public static String convertSnake(String key) {
        StringBuilder sb = new StringBuilder();
        boolean isChange = false;
        for (char c : key.toCharArray()) {
            if (c == '_') {
                isChange = true;
                continue;
            }
            if (isChange) {
                sb.append((char) (c - 32));
                isChange = false;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
