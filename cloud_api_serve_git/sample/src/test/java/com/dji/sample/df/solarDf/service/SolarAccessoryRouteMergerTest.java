package com.dji.sample.df.solarDf.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dji.sample.df.solarDf.model.entity.SolarAccessory;
import com.dji.sample.df.solarDf.model.entity.SolarAccessoryWaypoint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SolarAccessoryRouteMergerTest {

    private SolarAccessoryWaypoint waypoint(String name, int order, String format) {
        SolarAccessoryWaypoint point = new SolarAccessoryWaypoint();
        point.setName(name);
        point.setSortOrder(order);
        point.setLongitude(120.1 + order * 0.001);
        point.setLatitude(36.1);
        point.setHeight(55.0 + order);
        point.setHeading(180.0 + order);
        point.setPitch(-35.0);
        point.setFocalLength(24.0);
        point.setAction("take_photo");
        point.setImageFormat(format);
        point.setEnabled(true);
        return point;
    }

    private SolarAccessory device(String id, String name, SolarAccessoryWaypoint... points) {
        SolarAccessory device = new SolarAccessory();
        device.setId(id);
        device.setName(name);
        device.setDeviceType("inverter");
        device.setOrthophotoId("photo-1");
        device.setEnabled(true);
        device.setWaypoints(Arrays.asList(points));
        return device;
    }

    private JSONObject previewResponse() {
        return JSON.parseObject("{\"three_d_payload\":{\"waypoints\":[" +
                "{\"waypoint_id\":\"wp_0001\",\"index\":0,\"lon\":120.0,\"lat\":36.0,\"height\":30.0,\"aircraft_heading\":10.0," +
                "\"cameras\":[{\"image_format\":\"visable\",\"pitch\":-70.0,\"focal_length_35mm\":75.0},{\"image_format\":\"ir\",\"pitch\":-70.0,\"focal_length_35mm\":52.0}]}," +
                "{\"waypoint_id\":\"wp_0002\",\"index\":1,\"lon\":120.01,\"lat\":36.01,\"height\":31.0,\"aircraft_heading\":12.0," +
                "\"cameras\":[{\"image_format\":\"visable\",\"pitch\":-71.0,\"focal_length_35mm\":76.0}]}]}," +
                "\"route_2d\":{\"area\":{\"points\":[{\"lon\":120.0,\"lat\":36.0,\"row\":1,\"col\":1}]}}}");
    }

    @Test
    void appendsPreviewWaypointsAfterMainInDeviceOrder() {
        JSONObject response = previewResponse();
        SolarAccessory first = device("d1", "逆变器-01", waypoint("正面", 1, "visible"), waypoint("背面", 2, "both"));
        SolarAccessory second = device("d2", "汇流箱-02", waypoint("全景", 1, "ir"));

        JSONObject wpPixel1 = new JSONObject();
        wpPixel1.put("row", 9);
        wpPixel1.put("col", 8);
        JSONObject devicePixel = new JSONObject();
        devicePixel.put("row", 5);
        devicePixel.put("col", 4);
        int appended = SolarAccessoryRouteMerger.appendPreviewWaypoints(response,
                Arrays.asList(first, second),
                Collections.singletonList(wpPixel1),
                Collections.singletonList(devicePixel));

        assertEquals(3, appended);
        JSONArray waypoints = response.getJSONObject("three_d_payload").getJSONArray("waypoints");
        assertEquals(5, waypoints.size());
        JSONObject firstAccessory = waypoints.getJSONObject(2);
        assertEquals("acc_wp_0003", firstAccessory.getString("waypoint_id"));
        assertEquals(2, firstAccessory.getIntValue("index"));
        assertEquals(180.0 + 1, firstAccessory.getDoubleValue("aircraft_heading"));
        // visible 单光：一台相机
        assertEquals(1, firstAccessory.getJSONArray("cameras").size());
        assertEquals("visable", firstAccessory.getJSONArray("cameras").getJSONObject(0).getString("image_format"));
        // both 双光
        assertEquals(2, waypoints.getJSONObject(3).getJSONArray("cameras").size());
        // 2D 叠加：仅第一个设备航点有像素坐标（waypointPixels 只给了一个）
        JSONArray points = response.getJSONObject("route_2d").getJSONObject("area").getJSONArray("points");
        assertEquals(2, points.size());
        assertEquals(9, points.getJSONObject(1).getIntValue("row"));
        assertEquals(-35.0, points.getJSONObject(1).getDoubleValue("pitch"));
        // 2D 点带设备标识，供前端分段着色与标注
        assertEquals("d1", points.getJSONObject(1).getString("device_id"));
        assertEquals("逆变器-01", points.getJSONObject(1).getString("device_name"));
        assertEquals("正面", points.getJSONObject(1).getString("waypoint_name"));
        // 设备位置标记：仅第一台提供了像素
        JSONArray markers = response.getJSONObject("route_2d").getJSONObject("area").getJSONArray("accessory_devices");
        assertEquals(2, markers.size());
        assertEquals("逆变器-01", markers.getJSONObject(0).getString("name"));
        assertEquals("inverter", markers.getJSONObject(0).getString("device_type"));
        assertEquals(5, markers.getJSONObject(0).getIntValue("row"));
        assertEquals(4, markers.getJSONObject(0).getIntValue("col"));
        // 未提供像素的设备标记仍输出（仅无 row/col）
        assertEquals("汇流箱-02", markers.getJSONObject(1).getString("name"));
        assertFalse(markers.getJSONObject(1).containsKey("row"));
        assertFalse(markers.getJSONObject(1).containsKey("col"));
    }

    @Test
    void buildsEditedWaypointsMainFirstThenAccessories() {
        JSONObject response = previewResponse();
        SolarAccessory accessory = device("d1", "逆变器-01", waypoint("正面", 1, "visible"));

        JSONArray edited = SolarAccessoryRouteMerger.buildEditedWaypoints(response, Collections.singletonList(accessory));

        assertEquals(3, edited.size());
        JSONObject first = edited.getJSONObject(0);
        assertEquals(10.0, first.getDoubleValue("aircraft_heading"));
        assertEquals(-70.0, first.getDoubleValue("pitch"));
        assertEquals(75.0, first.getDoubleValue("visible_focal_length_35mm"));
        assertEquals(52.0, first.getDoubleValue("ir_focal_length_35mm"));
        // 只有可见光相机时省略 ir 焦距，由 C++ 按默认补齐
        JSONObject second = edited.getJSONObject(1);
        assertEquals(-71.0, second.getDoubleValue("pitch"));
        assertEquals(76.0, second.getDoubleValue("visible_focal_length_35mm"));
        assertNull(second.get("ir_focal_length_35mm"));
        // 设备航点：毫米焦距按 35mm 等效口径传递
        JSONObject tail = edited.getJSONObject(2);
        assertEquals(181.0, tail.getDoubleValue("aircraft_heading"));
        assertEquals(-35.0, tail.getDoubleValue("pitch"));
        assertEquals(24.0, tail.getDoubleValue("visible_focal_length_35mm"));
        assertEquals(24.0, tail.getDoubleValue("ir_focal_length_35mm"));
    }

    @Test
    void missingThreeDPayloadAppendsNothingAndEditedKeepsAccessories() {
        JSONObject response = JSON.parseObject("{\"route_2d\":{\"area\":{\"points\":[]}}}");
        SolarAccessory accessory = device("d1", "逆变器-01", waypoint("正面", 1, "visible"));

        assertEquals(0, SolarAccessoryRouteMerger.appendPreviewWaypoints(response,
                Collections.singletonList(accessory), null, null));
        JSONArray edited = SolarAccessoryRouteMerger.buildEditedWaypoints(response,
                Collections.singletonList(accessory));
        assertEquals(1, edited.size());
        assertEquals("acc", "acc".equals("acc") ? edited.getJSONObject(0).getDoubleValue("height") == 56.0 ? "acc" : "bad" : "bad");
    }

    @Test
    void cameraFovMatchesCppFormula() {
        SolarAccessoryWaypoint point = waypoint("正面", 1, "visible");
        JSONObject preview = SolarAccessoryRouteMerger.previewWaypoint(point, 0);
        JSONObject camera = preview.getJSONArray("cameras").getJSONObject(0);
        double focal = 24.0;
        double expectedHfov = 2.0 * Math.toDegrees(Math.atan(36.0 / (2.0 * focal)));
        double expectedVfov = 2.0 * Math.toDegrees(Math.atan(27.0 / (2.0 * focal)));
        assertEquals(expectedHfov, camera.getDoubleValue("hfov"), 1e-9);
        assertEquals(expectedVfov, camera.getDoubleValue("vfov"), 1e-9);
        assertEquals(0.0, camera.getDoubleValue("roll"));
    }
}
