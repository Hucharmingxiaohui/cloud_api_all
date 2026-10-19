package com.dji.sample.df.solarDf.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dji.sample.df.solarDf.dao.OrthophotoEntityMapper;
import com.dji.sample.df.solarDf.model.entity.OrthophotoEntity;
import com.dji.sample.df.windDf.config.WaylineUrlConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class SolarAccessoryCoordinateService {
    private final OrthophotoEntityMapper photos;
    private final WaylineUrlConfig config;
    private final RestTemplate rest;

    public SolarAccessoryCoordinateService(OrthophotoEntityMapper photos, WaylineUrlConfig config, RestTemplate rest) {
        this.photos = photos;
        this.config = config;
        this.rest = rest;
    }

    public JSONObject convert(JSONObject request) {
        OrthophotoEntity photo = photos.selectById(request.getString("orthophoto_id"));
        if (photo == null) throw new IllegalArgumentException("正射图不存在");
        JSONArray points = request.getJSONArray("points");
        if (points == null || points.isEmpty() || points.size() > 1000) throw new IllegalArgumentException("坐标数量应为1至1000个");
        String direction = request.getString("direction");
        JSONObject body = new JSONObject();
        String url;
        if ("pixel_to_geo".equals(direction)) {
            for (int i = 0; i < points.size(); i++) {
                check(points.getJSONObject(i), "row", 0, Integer.MAX_VALUE);
                check(points.getJSONObject(i), "col", 0, Integer.MAX_VALUE);
            }
            body.put("solar_area_name", photo.getName());
            Map<String, Object> area = new HashMap<>();
            area.put("area_name", "accessory");
            area.put("corners_pixels", points);
            body.put("detect_areas", Collections.singletonList(area));
            url = config.getDetectAreaGenUrl();
        } else if ("geo_to_pixel".equals(direction)) {
            for (int i = 0; i < points.size(); i++) {
                check(points.getJSONObject(i), "lon", -180, 180);
                check(points.getJSONObject(i), "lat", -90, 90);
            }
            body.put("orthophoto_name", photo.getName());
            body.put("points", points);
            url = UriComponentsBuilder.fromHttpUrl(config.getDetectAreaGenUrl()).replacePath("/geoToPixel").replaceQuery(null).build().toUriString();
        } else {
            throw new IllegalArgumentException("坐标转换方向无效");
        }
        JSONObject response = rest.postForObject(url, body, JSONObject.class);
        if (response == null || response.getIntValue("code") != 200 || response.getJSONObject("data") == null) {
            throw new IllegalStateException("正射图坐标转换失败");
        }
        JSONObject result = new JSONObject();
        JSONArray converted = "pixel_to_geo".equals(direction)
                ? response.getJSONObject("data").getJSONArray("detect_areas").getJSONObject(0).getJSONArray("corners_geo")
                : response.getJSONObject("data").getJSONArray("points");
        if (converted == null || converted.size() != points.size()) throw new IllegalStateException("坐标转换结果数量不匹配");
        result.put("points", converted);
        return result;
    }

    private static void check(JSONObject point, String key, double min, double max) {
        Double value = point == null ? null : point.getDouble(key);
        if (value == null || !Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException("坐标参数无效：" + key);
    }
}
