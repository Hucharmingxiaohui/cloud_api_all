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
import com.df.framework.config.HttpClientPoolConfig;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.ContentDisposition;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

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

    @Resource
    private HttpClientPoolConfig httpClientPoolConfig;

    static final long MAX_DOWNLOAD_BYTES = 64L * 1024 * 1024;

    /** Binary transport only: caller owns the target stream and any image/file validation. */
    public String downloadFile(String fileId, OutputStream target) throws IOException {
        if (StringUtils.isBlank(fileId) || StringUtils.isBlank(properties.getBaseUrl())
                || StringUtils.isBlank(properties.getAccessKey()) || StringUtils.isBlank(properties.getAccessSecret()))
            throw new IOException("EUA download configuration or fileId missing");
        try {
            URI base = URI.create(properties.getBaseUrl());
            if (base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                    || !("http".equalsIgnoreCase(base.getScheme()) || "https".equalsIgnoreCase(base.getScheme())))
                throw new IOException("Invalid EUA download origin");
            URI uri = UriComponentsBuilder.fromUriString(trimSlash(properties.getBaseUrl()))
                    .path("/machineNest/noauth/third/file/download").queryParam("fileId", "{fileId}")
                    .encode(StandardCharsets.UTF_8).buildAndExpand(fileId).toUri();
            return streamDownload(uri.toURL(), target, true);
        } catch (Exception e) {
            // Transport exceptions may include URLs or upstream text; do not expose their cause.
            throw new IOException("EUA file download failed");
        }
    }

    String downloadLegacyPicture(String url, OutputStream target) throws IOException {
        try {
            URL source = new URL(url);
            if (!("http".equalsIgnoreCase(source.getProtocol()) || "https".equalsIgnoreCase(source.getProtocol()))
                    || source.getUserInfo() != null) throw new IOException("Invalid legacy protocol");
            return streamDownload(source, target, false);
        } catch (Exception e) { throw new IOException("EUA legacy image download failed"); }
    }

    private String streamDownload(URL url, OutputStream target, boolean authenticated) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(httpClientPoolConfig.getConnectTimeout() > 0 ? httpClientPoolConfig.getConnectTimeout() : 10000);
        connection.setReadTimeout(httpClientPoolConfig.getReadTimeout() > 0 ? httpClientPoolConfig.getReadTimeout() : 30000);
        connection.setRequestMethod("GET");
        if (authenticated) {
            authenticationHeaders().forEach((name, values) -> connection.setRequestProperty(name, values.get(0)));
        }
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IOException("EUA HTTP download rejected");
            String type = connection.getContentType();
            if (type != null && type.toLowerCase(Locale.ROOT).contains("json")) throw new IOException("EUA JSON download rejected");
            long length = connection.getContentLengthLong();
            if (length > MAX_DOWNLOAD_BYTES || length == 0) throw new IOException("EUA download size invalid");
            String name = null;
            String disposition = connection.getHeaderField("Content-Disposition");
            if (disposition != null) {
                try { name = ContentDisposition.parse(disposition).getFilename(); } catch (IllegalArgumentException ignored) { }
            }
            try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream())) {
                input.mark(1024);
                byte[] prefix = input.readNBytes(1024);
                input.reset();
                String start = new String(prefix, StandardCharsets.UTF_8).replace("\uFEFF", "").trim();
                if (start.startsWith("{") || start.startsWith("[")) throw new IOException("EUA JSON download rejected");
                long count = 0;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    count += read;
                    if (count > MAX_DOWNLOAD_BYTES) throw new IOException("EUA download limit exceeded");
                    target.write(buffer, 0, read);
                }
                if (count == 0 || (length >= 0 && count != length)) throw new IOException("EUA incomplete download");
            }
            return name;
        } finally { connection.disconnect(); }
    }

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
            headers.putAll(authenticationHeaders());
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

    private HttpHeaders authenticationHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("access_key", properties.getAccessKey());
        headers.set("access_secret", properties.getAccessSecret());
        return headers;
    }

    private String trimSlash(String base) {
        if (base == null) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
