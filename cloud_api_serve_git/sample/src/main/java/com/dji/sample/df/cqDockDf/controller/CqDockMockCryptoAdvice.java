package com.dji.sample.df.cqDockDf.controller;

import com.dji.sample.df.cqDockDf.service.CqDockTransportCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/** Deliberately excludes production callbacks and all non-mock controllers. */
@RestControllerAdvice(assignableTypes = CqDockMockController.class)
@ConditionalOnProperty(name = "cq-dock.mock-enabled", havingValue = "true")
public class CqDockMockCryptoAdvice extends RequestBodyAdviceAdapter implements ResponseBodyAdvice<Object> {
    private final CqDockTransportCodec codec;
    public CqDockMockCryptoAdvice(CqDockTransportCodec codec) { this.codec = codec; }

    @Override public boolean supports(MethodParameter parameter, Type type, Class<? extends HttpMessageConverter<?>> converter) {
        return parameter.getContainingClass() == CqDockMockController.class;
    }

    @Override public HttpInputMessage beforeBodyRead(HttpInputMessage input, MethodParameter parameter, Type type,
            Class<? extends HttpMessageConverter<?>> converter) throws IOException {
        try {
            byte[] plain = codec.decodeRequest(new String(input.getBody().readAllBytes(), StandardCharsets.UTF_8))
                    .toJSONString().getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders(); headers.putAll(input.getHeaders());
            headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
            headers.setContentLength(plain.length);
            return new HttpInputMessage() {
                public InputStream getBody() { return new ByteArrayInputStream(plain); }
                public HttpHeaders getHeaders() { return headers; }
            };
        } catch (Exception ignored) {
            throw new HttpMessageNotReadableException("请求数据解密失败", input);
        }
    }

    @Override public boolean supports(MethodParameter parameter, Class<? extends HttpMessageConverter<?>> converter) {
        return parameter.getContainingClass() == CqDockMockController.class;
    }

    @Override public Object beforeBodyWrite(Object body, MethodParameter parameter, MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        return request.getMethod() == HttpMethod.POST ? codec.response(body) : body;
    }
}
