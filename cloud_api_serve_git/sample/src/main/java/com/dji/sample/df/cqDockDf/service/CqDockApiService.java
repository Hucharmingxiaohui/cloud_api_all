package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSON;
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
        log.info("[cq-dock] assignTask request: {}", JSON.toJSONString(request));
        CqApiResponse response = post(properties.getAssignTaskPath(), request);
        log.info("[cq-dock] assignTask response: code={}, success={}, msg={}, data={}",
                response.getCode(), response.getSuccess(), response.getMsg(), response.getData());
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
        log.info("[cq-dock] pictureList response: code={}, success={}, msg={}, data={}",
                response.getCode(), response.getSuccess(), response.getMsg(), response.getData());
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
        log.info("[cq-dock] taskStatus response: code={}, success={}, msg={}, data={}",
                response.getCode(), response.getSuccess(), response.getMsg(), response.getData());
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
            String json = body == null ? "{}" : JSON.toJSONString(body);
            log.info("[cq-dock] POST url={}, body={}", url, json);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            ResponseEntity<String> responseEntity = httpClientTemplate.postForEntity(url, entity, String.class);
            String raw = responseEntity.getBody();
            result.setRawBody(raw);
            log.info("[cq-dock] POST result url={}, body={}", url, raw);
            if (StringUtils.isNotBlank(raw)) {
                JSONObject obj = JSON.parseObject(raw);
                result.setCode(obj.getInteger("code"));
                result.setMsg(obj.getString("msg"));
                result.setSuccess(obj.getBoolean("success"));
                result.setData(obj.getJSONObject("data"));
            }
        } catch (Exception e) {
            log.error("[cq-dock] POST failed url={}, error={}", url, e.getMessage(), e);
            result.setSuccess(false);
            result.setMsg(e.getMessage());
        }
        return result;
    }

    private String trimSlash(String base) {
        if (base == null) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
