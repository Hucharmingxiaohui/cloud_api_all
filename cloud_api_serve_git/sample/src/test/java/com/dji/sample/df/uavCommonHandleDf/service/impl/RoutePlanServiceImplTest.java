package com.dji.sample.df.uavCommonHandleDf.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.df.framework.utils.HttpUtils;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.solarDf.dao.OrthophotoEntityMapper;
import com.dji.sample.df.solarDf.dao.SolarPanelAreaMapper;
import com.dji.sample.df.solarDf.model.entity.SolarPanelArea;
import com.dji.sample.df.windDf.config.WaylineUrlConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutePlanServiceImplTest {

    @Test
    void solarRequestForwardsInspectionChoiceAndDefaultsToComponentInspection() throws Exception {
        // 对齐 SpringBeanConfiguration 的全局 SNAKE_CASE 命名策略，否则测试与线上反序列化行为不一致
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // snake_case 规范名与 camelCase 别名都应能绑定（历史前端报文为 camelCase）
        for (String jsonKey : new String[]{"inspect_support", "inspectSupport"}) {
            for (String value : new String[]{"true", "false"}) {
                assertForwardedValue(mapper, "\"" + jsonKey + "\":" + value, value, "true", "10.0", "-50.0", "1.0");
            }
        }
        // 不传该字段时按仅巡检组件处理
        assertForwardedValue(mapper, null, "false", "true", "10.0", "-50.0", "1.0");

        assertForwardedValue(mapper,
                "\"inspect_components\":false,\"inspect_support\":true,\"support_rel_alt\":8,\"support_pitch\":-45,\"support_margin\":2",
                "true", "false", "8.0", "-45.0", "2.0");
    }

    private void assertForwardedValue(ObjectMapper mapper, String injectedJson, String expectedSupport,
                                      String expectedComponents, String expectedRelAlt,
                                      String expectedPitch, String expectedMargin) throws Exception {
        RoutePlanServiceImpl service = new RoutePlanServiceImpl();
        PubWaylineJobPlanDfMapper planMapper = mock(PubWaylineJobPlanDfMapper.class);
        SolarPanelAreaMapper areaMapper = mock(SolarPanelAreaMapper.class);
        OrthophotoEntityMapper orthophotoMapper = mock(OrthophotoEntityMapper.class);
        WaylineUrlConfig config = mock(WaylineUrlConfig.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        HttpUtils http = mock(HttpUtils.class);
        com.dji.sample.df.solarDf.service.SolarAccessoryService accessoryService =
                mock(com.dji.sample.df.solarDf.service.SolarAccessoryService.class);
        ReflectionTestUtils.setField(service, "pubWaylineJobPlanDfMapper", planMapper);
        ReflectionTestUtils.setField(service, "solarPanelAreaMapper", areaMapper);
        ReflectionTestUtils.setField(service, "orthophotoEntityMapper", orthophotoMapper);
        ReflectionTestUtils.setField(service, "waylineUrlConfig", config);
        ReflectionTestUtils.setField(service, "httpUtils", http);
        ReflectionTestUtils.setField(service, "solarAccessoryService", accessoryService);
        // 未选设备时（Mockito 默认返回空列表）不应触发设备合成/守卫逻辑
        org.mockito.Mockito.when(accessoryService.listEnabledByIds(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Collections.emptyList());
        when(config.getBuildKmzUrl().getSolarPanelWayline()).thenReturn("http://localhost/solar");
        when(http.sendPostJson(anyString(), anyString())).thenReturn("{}");

        SolarPanelArea area = new SolarPanelArea();
        area.setId("area-1");
        area.setCorner1Lng(1.0);
        area.setCorner1Lat(1.0);
        area.setCorner2Lng(2.0);
        area.setCorner2Lat(2.0);
        area.setCorner3Lng(3.0);
        area.setCorner3Lat(3.0);
        area.setCorner4Lng(4.0);
        area.setCorner4Lat(4.0);
        area.setAreaHeight(0.0);
        when(areaMapper.selectById("area-1")).thenReturn(area);

        String input = "{\"plan_id\":\"plan-1\",\"solar_panel_id\":\"area-1\",\"type\":\"auto\",\"index\":0,\"area_configs\":[{\"solar_panel_id\":\"area-1\",\"flight_altitude\":30}]}";
        if (injectedJson != null) {
            input = input.substring(0, input.length() - 1) + "," + injectedJson + "}";
        }
        PubWaylineJobPlanDfEntity plan = mapper.readValue(input, PubWaylineJobPlanDfEntity.class);
        service.buildSolarPanelWayline(plan);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(http).sendPostJson(org.mockito.ArgumentMatchers.eq("http://localhost/solar"), body.capture());
        JSONObject request = JSON.parseObject(body.getValue());
        assertTrue(request.containsKey("inspectSupport"));
        assertEquals(Boolean.parseBoolean(expectedSupport), request.getBooleanValue("inspectSupport"));
        assertEquals(Boolean.parseBoolean(expectedComponents), request.getBooleanValue("inspectComponents"));
        assertEquals(Double.parseDouble(expectedRelAlt), request.getDoubleValue("supportRelAlt"));
        assertEquals(Double.parseDouble(expectedPitch), request.getDoubleValue("supportPitch"));
        assertEquals(Double.parseDouble(expectedMargin), request.getDoubleValue("supportMargin"));
    }
}
