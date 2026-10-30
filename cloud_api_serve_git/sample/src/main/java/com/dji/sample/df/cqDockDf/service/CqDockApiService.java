package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSONObject;
import com.dji.sample.df.cqDockDf.config.CqDockProperties;
import com.dji.sample.df.cqDockDf.model.dto.CqApiResponse;
import com.dji.sample.df.cqDockDf.model.dto.CqAssignTaskRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqTaskIdRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;

import javax.annotation.Resource;

/**
 * 重庆电力下级无人机平台 HTTP 接口封装
 */
@Slf4j
@Service
public class CqDockApiService {

    @Resource
    private CqDockProperties properties;

    @Resource
    @Qualifier("httpClientTemplate")
    private RestTemplate httpClientTemplate;

    /**
     * 机场任务下发 POST /machineNest/noauth/third/task/assign_task
     */
    public CqApiResponse assignTask(CqAssignTaskRequest request) {
        CqApiResponse response = post(properties.getAssignTaskPath(), request);
        log.info("[cq-dock] assignTask result: code={}, success={}", response.getCode(), response.getSuccess());
        return response;
    }

    /**
     * 图片列表获取 POST /machineNest/noauth/third/picture/list
     */
    public CqApiResponse pictureList(String taskId) {
        CqTaskIdRequest request = new CqTaskIdRequest();
        request.setTaskId(taskId);
        log.info("[cq-dock] pictureList request: taskId={}", taskId);
        CqApiResponse response = post(properties.getPictureListPath(), request);
        log.info("[cq-dock] pictureList result: code={}, success={}", response.getCode(), response.getSuccess());
        return response;
    }

    /**
     * 任务状态查询 POST /machineNest/noauth/third/task/status
     */
    public CqApiResponse taskStatus(String taskId) {
        CqTaskIdRequest request = new CqTaskIdRequest();
        request.setTaskId(taskId);
        log.info("[cq-dock] taskStatus request: taskId={}", taskId);
        CqApiResponse response = post(properties.getTaskStatusPath(), request);
        log.info("[cq-dock] taskStatus result: code={}, success={}", response.getCode(), response.getSuccess());
        return response;
    }

    private CqApiResponse post(String path, Object body) {
        CqApiResponse result = new CqApiResponse();
        if (StringUtils.isBlank(properties.getBaseUrl())) {
            log.error("[cq-dock] baseUrl is empty, skip request path={}", path);
            result.setSuccess(false);
            result.setMsg("cq-dock.base-url is empty");
            return result;
        }
        String url = trimSlash(properties.getBaseUrl()) + path;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("access_key", properties.getAccessKey());
            headers.set("access_secret", properties.getAccessSecret());
            CqDockTransportCodec codec = new CqDockTransportCodec(properties);
            String json = codec.request(body).toJSONString();
            log.info("[cq-dock] POST path={}", path);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            ResponseEntity<String> responseEntity = httpClientTemplate.postForEntity(url, entity, String.class);
            populate(result, codec.decodeResponse(responseEntity.getBody()));
        } catch (RestClientResponseException e) {
            result.setCode(e.getRawStatusCode());
            result.setMsg("EUA HTTP request failed");
            try {
                JSONObject error = new CqDockTransportCodec(properties).decodeResponse(e.getResponseBodyAsString());
                if (!Boolean.TRUE.equals(error.get("success"))) populate(result, error);
            } catch (Exception ignored) { /* An HTTP error body need not be JSON or encrypted. */ }
            result.setSuccess(false);
            log.warn("[cq-dock] POST failed path={}, status={}", path, e.getRawStatusCode());
        } catch (Exception e) {
            log.warn("[cq-dock] POST transport failed path={}, type={}", path, e.getClass().getSimpleName());
            result.setSuccess(false);
            result.setMsg("EUA加密传输失败");
        }
        return result;
    }

    private void populate(CqApiResponse result, JSONObject obj) {
        result.setCode(obj.getInteger("code"));
        result.setMsg(obj.getString("msg"));
        result.setSuccess(Integer.valueOf(200).equals(result.getCode()) && Boolean.TRUE.equals(obj.get("success")));
        result.setData(obj.getJSONObject("data"));
    }

    private String trimSlash(String base) {
        if (base == null) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
