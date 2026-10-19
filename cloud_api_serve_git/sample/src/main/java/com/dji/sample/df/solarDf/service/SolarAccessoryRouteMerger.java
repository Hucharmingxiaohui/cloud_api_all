package com.dji.sample.df.solarDf.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dji.sample.df.solarDf.model.entity.SolarAccessory;
import com.dji.sample.df.solarDf.model.entity.SolarAccessoryWaypoint;

import java.util.ArrayList;
import java.util.List;

/**
 * 附属设备航点与 C++ /solar 主航线的合成逻辑（纯函数，便于单测）。
 *
 * 合成策略（已确认）：设备航点追加在组件+支架主航线之后；每台设备内按 sort_order 升序。
 * 预览（index=0）：追加到 three_d_payload.waypoints 与 route_2d.area.points（带像素坐标）。
 * 保存（index=1）：主航线 + 设备航点映射为 /solar/edited 输入格式，由服务器合成 edited.kmz。
 *
 * 口径说明：采集页录入的毫米焦距按 35mm 等效焦距传递（与 Pilot 变焦标注口径一致）。
 */
public final class SolarAccessoryRouteMerger {

    private static final double SENSOR_WIDTH_MM = 36.0;
    private static final double SENSOR_HEIGHT_4X3_MM = 27.0;
    private static final double DEFAULT_PITCH = -90.0;

    private SolarAccessoryRouteMerger() {
    }

    /**
     * 预览响应追加设备航点。
     * waypointPixels/devicePixels 为与扁平化设备航点、设备位置顺序对齐的像素坐标（可为 null，表示跳过 2D 叠加）。
     * route_2d 点附带 device_id/device_name/waypoint_name，前端据此分段着色并标注；
     * route_2d.accessory_devices 为设备位置标记（名称+像素坐标）。
     * 返回追加的航点数。
     */
    public static int appendPreviewWaypoints(JSONObject response, List<SolarAccessory> devices,
                                             List<JSONObject> waypointPixels, List<JSONObject> devicePixels) {
        if (response == null || devices == null || devices.isEmpty()) return 0;
        JSONObject threeD = response.getJSONObject("three_d_payload");
        if (threeD == null) return 0;
        JSONArray waypoints = threeD.getJSONArray("waypoints");
        if (waypoints == null) {
            waypoints = new JSONArray();
            threeD.put("waypoints", waypoints);
        }
        JSONObject area = route2dArea(response);
        JSONArray points = null;
        JSONArray deviceMarkers = null;
        if (area != null) {
            points = area.getJSONArray("points");
            if (points == null) {
                points = new JSONArray();
                area.put("points", points);
            }
            deviceMarkers = new JSONArray();
            area.put("accessory_devices", deviceMarkers);
        }
        int appended = 0;
        int pixelIndex = 0;
        for (SolarAccessory device : devices) {
            for (SolarAccessoryWaypoint point : device.getWaypoints()) {
                waypoints.add(previewWaypoint(point, waypoints.size()));
                if (points != null && waypointPixels != null && pixelIndex < waypointPixels.size() && waypointPixels.get(pixelIndex) != null) {
                    points.add(route2dPoint(point, waypointPixels.get(pixelIndex), device));
                }
                pixelIndex++;
                appended++;
            }
            if (deviceMarkers != null && devicePixels != null) {
                int deviceIndex = devices.indexOf(device);
                JSONObject pixel = deviceIndex < devicePixels.size() ? devicePixels.get(deviceIndex) : null;
                deviceMarkers.add(deviceMarker(device, pixel));
            }
        }
        return appended;
    }

    /**
     * 保存路径合成 /solar/edited 的完整航点列表：主航线（three_d_payload.waypoints）在前，设备航点在后。
     */
    public static JSONArray buildEditedWaypoints(JSONObject response, List<SolarAccessory> devices) {
        JSONArray result = new JSONArray();
        JSONObject threeD = response == null ? null : response.getJSONObject("three_d_payload");
        JSONArray main = threeD == null ? null : threeD.getJSONArray("waypoints");
        if (main != null) {
            for (int i = 0; i < main.size(); i++) {
                result.add(mainToEdited(main.getJSONObject(i)));
            }
        }
        if (devices != null) {
            for (SolarAccessory device : devices) {
                for (SolarAccessoryWaypoint point : device.getWaypoints()) {
                    result.add(accessoryToEdited(point));
                }
            }
        }
        return result;
    }

    /** 设备航点 → /solar/edited 输入格式 */
    public static JSONObject accessoryToEdited(SolarAccessoryWaypoint point) {
        JSONObject edited = new JSONObject();
        edited.put("lon", point.getLongitude());
        edited.put("lat", point.getLatitude());
        edited.put("height", point.getHeight());
        edited.put("aircraft_heading", point.getHeading());
        edited.put("pitch", point.getPitch());
        edited.put("visible_focal_length_35mm", point.getFocalLength());
        edited.put("ir_focal_length_35mm", point.getFocalLength());
        return edited;
    }

    /** 主航线预览航点 → /solar/edited 输入格式（从 cameras 提取俯仰与焦距） */
    public static JSONObject mainToEdited(JSONObject waypoint) {
        JSONObject edited = new JSONObject();
        edited.put("lon", waypoint.getDouble("lon"));
        edited.put("lat", waypoint.getDouble("lat"));
        edited.put("height", waypoint.getDouble("height"));
        edited.put("aircraft_heading", waypoint.getDouble("aircraft_heading"));
        edited.put("pitch", mainPitch(waypoint));
        Double visible = mainFocal(waypoint, "visable");
        Double ir = mainFocal(waypoint, "ir");
        // 未找到时省略字段，由 C++ 端按配置默认值补齐
        if (visible != null) edited.put("visible_focal_length_35mm", visible);
        if (ir != null) edited.put("ir_focal_length_35mm", ir);
        return edited;
    }

    /** 设备航点 → 预览载荷航点（对齐 C++ buildThreeDRoutePayload 字段） */
    public static JSONObject previewWaypoint(SolarAccessoryWaypoint point, int index) {
        JSONObject wp = new JSONObject();
        wp.put("waypoint_id", String.format("acc_wp_%04d", index + 1));
        wp.put("index", index);
        wp.put("lon", point.getLongitude());
        wp.put("lat", point.getLatitude());
        wp.put("height", point.getHeight());
        wp.put("aircraft_heading", point.getHeading());
        JSONArray cameras = new JSONArray();
        String format = point.getImageFormat() == null ? "visible" : point.getImageFormat();
        if ("visible".equals(format) || "both".equals(format)) {
            cameras.add(camera(point, "visable"));
        }
        if ("ir".equals(format) || "both".equals(format)) {
            cameras.add(camera(point, "ir"));
        }
        wp.put("cameras", cameras);
        if (!cameras.isEmpty()) {
            wp.put("camera", cameras.get(0));
        }
        return wp;
    }

    /** 设备航点 → route_2d.area.points 条目（含像素坐标与设备标识，供前端分段着色标注） */
    public static JSONObject route2dPoint(SolarAccessoryWaypoint point, JSONObject pixel, SolarAccessory device) {
        JSONObject p = new JSONObject();
        p.put("lon", point.getLongitude());
        p.put("lat", point.getLatitude());
        p.put("height", point.getHeight());
        p.put("heading", point.getHeading());
        p.put("pitch", point.getPitch());
        p.put("device_id", device.getId());
        p.put("device_name", device.getName());
        p.put("waypoint_name", point.getName());
        if (pixel != null) {
            p.put("row", pixel.getInteger("row"));
            p.put("col", pixel.getInteger("col"));
        }
        return p;
    }

    /** 设备位置标记 → route_2d.accessory_devices 条目（像素坐标供 2D 标注） */
    public static JSONObject deviceMarker(SolarAccessory device, JSONObject pixel) {
        JSONObject marker = new JSONObject();
        marker.put("id", device.getId());
        marker.put("name", device.getName());
        marker.put("device_type", device.getDeviceType());
        marker.put("lon", device.getLongitude());
        marker.put("lat", device.getLatitude());
        if (pixel != null) {
            marker.put("row", pixel.getInteger("row"));
            marker.put("col", pixel.getInteger("col"));
        }
        return marker;
    }

    private static JSONObject camera(SolarAccessoryWaypoint point, String imageFormat) {
        JSONObject camera = new JSONObject();
        Double focal = point.getFocalLength();
        camera.put("yaw", point.getHeading());
        camera.put("pitch", point.getPitch());
        camera.put("roll", 0.0);
        camera.put("focal_length_35mm", focal);
        camera.put("hfov", focal != null ? focal35ToHfov(focal) : null);
        camera.put("vfov", focal != null ? focal35ToVfov(focal) : null);
        camera.put("aspect_ratio", 4032.0 / 3024.0);
        camera.put("image_format", imageFormat);
        camera.put("payload_lens_index", imageFormat);
        camera.put("file_suffix", "");
        return camera;
    }

    private static Double mainPitch(JSONObject waypoint) {
        JSONObject camera = firstCamera(waypoint);
        return camera != null && camera.getDouble("pitch") != null ? camera.getDouble("pitch") : DEFAULT_PITCH;
    }

    private static Double mainFocal(JSONObject waypoint, String type) {
        JSONArray cameras = waypoint.getJSONArray("cameras");
        if (cameras != null) {
            for (int i = 0; i < cameras.size(); i++) {
                JSONObject camera = cameras.getJSONObject(i);
                String format = camera.getString("image_format");
                if (format != null && format.contains(type)) {
                    return camera.getDouble("focal_length_35mm");
                }
            }
        }
        return null;
    }

    private static JSONObject firstCamera(JSONObject waypoint) {
        JSONObject camera = waypoint.getJSONObject("camera");
        if (camera != null) return camera;
        JSONArray cameras = waypoint.getJSONArray("cameras");
        return cameras != null && !cameras.isEmpty() ? cameras.getJSONObject(0) : null;
    }

    private static JSONObject route2dArea(JSONObject response) {
        JSONObject route2d = response.getJSONObject("route_2d");
        return route2d != null ? route2d.getJSONObject("area") : null;
    }

    private static double focal35ToHfov(double focal35mm) {
        return 2.0 * Math.toDegrees(Math.atan(SENSOR_WIDTH_MM / (2.0 * focal35mm)));
    }

    private static double focal35ToVfov(double focal35mm) {
        return 2.0 * Math.toDegrees(Math.atan(SENSOR_HEIGHT_4X3_MM / (2.0 * focal35mm)));
    }
}
