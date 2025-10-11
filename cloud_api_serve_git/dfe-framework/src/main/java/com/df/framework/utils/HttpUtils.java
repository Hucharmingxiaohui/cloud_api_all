package com.df.framework.utils;

import com.df.framework.exception.FastException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * 通用http发送方法
 *
 * @author ruoyi
 */
@Component
@Slf4j
public class HttpUtils {

    public static final String AUTHORIZATION_KEY = "Authorization";
    public static final String X_TOKEN_KEY = "x-token";
    public static final String TOKEN_KEY = "token";

    @Autowired
    @Qualifier("httpClientTemplate")
    private RestTemplate httpClientTemplate;


    /**
     * 向指定 URL 发送GET方法的请求
     */
    public String sendGet(String url, Map<String, Object> params, String token, String tokenKey) {
        /*log.info("【GET 地址】 {}", url);
        log.info("【GET 参数】 {}", params);
        log.info("【GET token】 {}： {}", tokenKey, token)*/
        ;
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                map.add(entry.getKey(), entry.getValue().toString());
            }
        }
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.isNotBlank(token) && StringUtils.isNotBlank(tokenKey)) {
            headers.set(tokenKey, token);
        }
        HttpEntity<String> request = new HttpEntity<>(headers);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        URI uri = builder.queryParams(map).build().encode().toUri();
        ResponseEntity<String> forEntity = httpClientTemplate.exchange(uri, HttpMethod.GET, request, String.class);
        log.info("【GET 结果】 {}", forEntity.getBody());
        return forEntity.getBody();
    }

    /**
     * 向指定 URL 发送POST方法的请求  请求参数JSON形式
     */
    public String sendPostJson(String url, String param, String token, String tokenKey) {
        log.info("【POST 地址】 {}", url);
        log.info("【POST 参数】 {}", param);
        log.info("【POST token】 {}： {}", tokenKey, token);
        log.info("【POST Content-Type】 {}", MediaType.APPLICATION_JSON_VALUE);
        //创建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(token) && StringUtils.isNotBlank(tokenKey)) {
            headers.set(tokenKey, token);
        }
        param = StringUtils.isBlank(param) ? "" : param;
        HttpEntity<String> entity = new HttpEntity<>(param, headers);
        ResponseEntity<String> responseEntity = httpClientTemplate.postForEntity(url, entity, String.class);
        log.info("【POST 结果】 {}", responseEntity.getBody());
        return responseEntity.getBody();
    }

    public String sendPostJson(String url, String param) {
        /*log.info("【POST 地址】 {}", url);
        log.info("【POST 参数】 {}", param);
        log.info("【POST Content-Type】 {}", MediaType.APPLICATION_JSON_VALUE);*/
        //创建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        /*if (StringUtils.isNotBlank(token) && StringUtils.isNotBlank(tokenKey)) {
            headers.set(tokenKey, token);
        }*/
        param = StringUtils.isBlank(param) ? "" : param;
        HttpEntity<String> entity = new HttpEntity<>(param, headers);
        ResponseEntity<String> responseEntity = httpClientTemplate.postForEntity(url, entity, String.class);
        log.info("【POST 结果】 {}", responseEntity.getBody());
        return responseEntity.getBody();
    }

    public String sendPost_X_WWW_FORM_URLENCODE(String url, Map<String, String> headers, Map<String, Object> params) {
        // 请求头
        String response = "{}";
        try {
            HttpPostForm httpPostForm = new HttpPostForm(url, "utf-8", headers);
            httpPostForm.addFormField(params);
            // 返回信息
            response = httpPostForm.finish();
            if (response == null) {
                throw new FastException("【POST】 返回 NULL");
            }
        } catch (Exception e) {
            log.error("【POST 地址】 {}", url);
            log.error("【POST 参数】 {}", params);
            log.error("【POST Header参数 token】 {}", headers);
            log.error("【POST Content-Type】 {}", "application/x-www-form-urlencoded");
            log.error("【POST 结果】 {}", response);
            e.printStackTrace();
        }
        return response;
    }

    public String sendPostUrl(String url, Map<String, Object> params, String token, String tokenKey) {
        log.info("【POST 地址】 {}", url);
        log.info("【POST 参数】 {}", params);
        log.info("【POST Header参数 token】 {}： {}", tokenKey, token);
        log.info("【POST Content-Type】 {}", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                map.add(entry.getKey(), entry.getValue());
            }
        }
        HttpHeaders headers = new HttpHeaders();
        //头部类型
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (StringUtils.isNotBlank(token) && StringUtils.isNotBlank(tokenKey)) {
            headers.set(tokenKey, token);
        }
        //构造实体对象
        HttpEntity<MultiValueMap<String, Object>> param = new HttpEntity<>(map, headers);
        //发起请求,服务地址，请求参数，返回消息体的数据类型
        ResponseEntity<String> response = httpClientTemplate.postForEntity(url, param, String.class);
        //body
        String body = response.getBody();
        log.info("【POST 结果】 {}", body);
        return body;
    }

    /*public static void main(String[] args) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = ((x509Certificates, authType) -> true);
        SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setSSLSocketFactory(connectionSocketFactory);
        CloseableHttpClient httpClient = httpClientBuilder.build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(factory);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.set("token", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI0MDgxZjJkZDc2M2QxNzkxMDE3NjNkMmNlMzc5MDAyOSIsInVzZXJfbmFtZSI6ImNlc2hpMDIiLCJuYW1lIjoiY2VzaGkwMiIsImlzcyI6ImNjc0BucmVjIiwiZXhwIjoxNzAzNjQwNjk2LCJpYXQiOjE3MDM2Mzg4OTZ9.n0UeLzA36liPQPxyEOYUxCpw3OXRmWvOzCUXa7PkT7oIMkpsAgvrX5CbBd0LFAx-ZVzEQv2qSHCbA11ivbqIMQ");
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(map, headers);
        String strbody = restTemplate.postForObject(
                "https://172.20.51.5:8443/v1/ccs/auth-service/oauth/check_token",
                //"https://172.20.63.9:443/test/insert",
                //HttpMethod.POST,
                httpEntity,
                String.class);
        System.out.println(strbody);
        try {
            // 请求头
            //Map<String, String> headers = new HashMap<>();
            //headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36");
            HttpPostForm httpPostForm = new HttpPostForm("https://172.20.51.5:8443/v1/ccs/auth-service/oauth/check_token",
                    "utf-8", null);
            // post参数
            httpPostForm.addFormField("token", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI0MDgxZjJkZDc2M2QxNzkxMDE3NjNkMmNlMzc5MDAyOSIsInVzZXJfbmFtZSI6ImNlc2hpMDIiLCJuYW1lIjoiY2VzaGkwMiIsImlzcyI6ImNjc0BucmVjIiwiZXhwIjoxNzAzNjQwNjk2LCJpYXQiOjE3MDM2Mzg4OTZ9.n0UeLzA36liPQPxyEOYUxCpw3OXRmWvOzCUXa7PkT7oIMkpsAgvrX5CbBd0LFAx-ZVzEQv2qSHCbA11ivbqIMQ");
            // 返回信息
            String response = httpPostForm.finish();
            System.out.println(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}
