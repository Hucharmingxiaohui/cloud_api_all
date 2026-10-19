package com.dji.sample.df.solarDf.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dji.sample.df.solarDf.dao.OrthophotoEntityMapper;
import com.dji.sample.df.solarDf.model.entity.OrthophotoEntity;
import com.dji.sample.df.windDf.config.WaylineUrlConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SolarAccessoryCoordinateServiceTest {
    private final OrthophotoEntityMapper photos = mock(OrthophotoEntityMapper.class);
    private final RestTemplate rest = mock(RestTemplate.class);
    private final WaylineUrlConfig config = new WaylineUrlConfig();

    private SolarAccessoryCoordinateService service() {
        OrthophotoEntity photo = new OrthophotoEntity();
        photo.setName("测试正射图");
        when(photos.selectById("p")).thenReturn(photo);
        config.setDetectAreaGenUrl("http://localhost:5005/detectArea");
        return new SolarAccessoryCoordinateService(photos, config, rest);
    }

    @Test
    void convertsSinglePixelWithoutRequiringFourCorners() {
        SolarAccessoryCoordinateService service = service();
        when(rest.postForObject(eq(config.getDetectAreaGenUrl()), any(), eq(JSONObject.class)))
                .thenReturn(JSON.parseObject("{\"code\":200,\"data\":{\"detect_areas\":[{\"corners_geo\":[{\"lon\":120,\"lat\":36}]}]}}"));
        JSONObject result = service.convert(JSON.parseObject("{\"orthophoto_id\":\"p\",\"direction\":\"pixel_to_geo\",\"points\":[{\"row\":10,\"col\":20}]}"));
        assertEquals(120.0, result.getJSONArray("points").getJSONObject(0).getDouble("lon"));
    }

    @Test
    void usesConfiguredHostForReverseProjection() {
        SolarAccessoryCoordinateService service = service();
        when(rest.postForObject(eq("http://localhost:5005/geoToPixel"), any(), eq(JSONObject.class)))
                .thenReturn(JSON.parseObject("{\"code\":200,\"data\":{\"points\":[{\"row\":10,\"col\":20}]}}"));
        JSONObject result = service.convert(JSON.parseObject("{\"orthophoto_id\":\"p\",\"direction\":\"geo_to_pixel\",\"points\":[{\"lon\":120,\"lat\":36}]}"));
        assertEquals(20, result.getJSONArray("points").getJSONObject(0).getIntValue("col"));
    }

    @Test
    void rejectsMissingCoordinatesBeforeRemoteRequest() {
        SolarAccessoryCoordinateService service = service();
        assertThrows(IllegalArgumentException.class, () -> service.convert(JSON.parseObject("{\"orthophoto_id\":\"p\",\"direction\":\"pixel_to_geo\",\"points\":[{\"row\":10}]}")));
        verifyNoInteractions(rest);
    }
}
