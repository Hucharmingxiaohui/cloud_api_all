package com.dji.sample.df.solarDf.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.df.framework.vo.Result;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 光伏三维航线服务代理接口。
 * 前端只访问主后端，由主后端转发到 172.20.63.157 的航线生成服务。
 */
@Slf4j
@RestController
@RequestMapping("/api/solar3d")
public class Solar3DRouteController {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();

    @Value("${solar.route.edited-url:http://172.20.63.157:5001/solar/edited}")
    private String solarRouteEditedUrl;

    @PostMapping("/edited")
    public Result edited(@RequestBody Map<String, Object> payload) {
        String requestId = "solar-edited-" + System.currentTimeMillis();
        String routeDraftId = String.valueOf(payload.get("route_draft_id"));
        Object waypoints = payload.get("waypoints");
        int waypointCount = waypoints instanceof java.util.Collection ? ((java.util.Collection<?>) waypoints).size() : -1;
        String requestJson = JSONObject.toJSONString(payload);
        long startedAt = System.currentTimeMillis();

        log.info("[Solar3DEdited][{}] forward start, route_draft_id={}, waypointCount={}, bytes={}, url={}",
                requestId, routeDraftId, waypointCount, requestJson.length(), solarRouteEditedUrl);

        Request request = new Request.Builder()
                .url(solarRouteEditedUrl)
                .post(okhttp3.RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")
                .header("Connection", "close")
                .header("X-Solar-Edited-Request-Id", requestId)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseText = body == null ? "" : body.string();
            long elapsed = System.currentTimeMillis() - startedAt;
            log.info("[Solar3DEdited][{}] forward done, status={}, elapsed={}ms, bytes={}",
                    requestId, response.code(), elapsed, responseText.length());

            Object data = parseResponse(responseText);
            if (!response.isSuccessful()) {
                log.warn("[Solar3DEdited][{}] upstream failed, body={}", requestId, preview(responseText));
                return Result.error("航线服务返回异常: HTTP " + response.code());
            }
            return Result.success(data, "光伏三维航线编辑结果已回传");
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startedAt;
            log.error("[Solar3DEdited][{}] forward failed, elapsed={}ms", requestId, elapsed, e);
            return Result.error("航线服务回传失败: " + e.getMessage());
        }
    }

    private Object parseResponse(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parse(responseText);
        } catch (Exception e) {
            return responseText;
        }
    }

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 500 ? text : text.substring(0, 500);
    }
}
