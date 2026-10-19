package com.dji.sample.df.solarDf.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.solarDf.dao.*;
import com.dji.sample.df.solarDf.model.entity.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SolarAccessoryService {
    private final SolarAccessoryMapper devices;
    private final SolarAccessoryWaypointMapper waypoints;
    private final OrthophotoEntityMapper photos;
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList("inverter", "combiner", "transformer", "cabinet", "tower", "weather_station", "other"));

    public SolarAccessoryService(SolarAccessoryMapper devices, SolarAccessoryWaypointMapper waypoints, OrthophotoEntityMapper photos) {
        this.devices = devices;
        this.waypoints = waypoints;
        this.photos = photos;
    }

    public List<SolarAccessory> list(String orthophotoId) {
        requirePhoto(orthophotoId);
        List<SolarAccessory> result = devices.selectList(new LambdaQueryWrapper<SolarAccessory>()
                .eq(SolarAccessory::getOrthophotoId, orthophotoId).orderByAsc(SolarAccessory::getName).orderByAsc(SolarAccessory::getId));
        if (result.isEmpty()) return result;
        List<String> ids = result.stream().map(SolarAccessory::getId).collect(Collectors.toList());
        Map<String, List<SolarAccessoryWaypoint>> children = waypoints.selectList(new LambdaQueryWrapper<SolarAccessoryWaypoint>()
                .in(SolarAccessoryWaypoint::getDeviceId, ids).orderByAsc(SolarAccessoryWaypoint::getSortOrder))
                .stream().collect(Collectors.groupingBy(SolarAccessoryWaypoint::getDeviceId));
        result.forEach(device -> device.setWaypoints(children.getOrDefault(device.getId(), Collections.emptyList())));
        return result;
    }

    /**
     * 按计划选中的设备ID取可执行的附属设备：仅保留启用设备，且只带启用航点（按sort_order升序）。
     * 保持请求中的ID顺序；无启用航点的设备直接剔除。
     */
    public List<SolarAccessory> listEnabledByIds(List<String> ids, String orthophotoId) {
        if (ids == null || ids.isEmpty() || orthophotoId == null || orthophotoId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> distinct = ids.stream().filter(id -> id != null && !id.trim().isEmpty()).distinct().collect(Collectors.toList());
        if (distinct.isEmpty()) return Collections.emptyList();
        Map<String, SolarAccessory> byId = devices.selectBatchIds(distinct).stream()
                .filter(device -> Boolean.TRUE.equals(device.getEnabled()))
                .filter(device -> orthophotoId.equals(device.getOrthophotoId()))
                .collect(Collectors.toMap(SolarAccessory::getId, device -> device));
        if (byId.isEmpty()) return Collections.emptyList();
        Map<String, List<SolarAccessoryWaypoint>> children = waypoints.selectList(new LambdaQueryWrapper<SolarAccessoryWaypoint>()
                .in(SolarAccessoryWaypoint::getDeviceId, byId.keySet()).orderByAsc(SolarAccessoryWaypoint::getSortOrder))
                .stream()
                .filter(point -> Boolean.TRUE.equals(point.getEnabled()))
                .collect(Collectors.groupingBy(SolarAccessoryWaypoint::getDeviceId));
        List<SolarAccessory> result = new ArrayList<>();
        for (String id : distinct) {
            SolarAccessory device = byId.get(id);
            if (device == null) continue;
            List<SolarAccessoryWaypoint> enabled = children.getOrDefault(id, Collections.emptyList());
            if (enabled.isEmpty()) continue;
            device.setWaypoints(enabled);
            result.add(device);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public SolarAccessory save(SolarAccessory device) {
        validate(device);
        requirePhoto(device.getOrthophotoId());
        if (device.getId() == null || device.getId().isEmpty()) {
            device.setId(UUID.randomUUID().toString());
            if (devices.insert(device) != 1) throw new IllegalStateException("设备保存失败");
        } else {
            requireDevice(device.getId(), device.getOrthophotoId());
            if (devices.updateById(device) != 1) throw new IllegalStateException("设备更新失败");
            waypoints.delete(new LambdaQueryWrapper<SolarAccessoryWaypoint>().eq(SolarAccessoryWaypoint::getDeviceId, device.getId()));
        }
        int index = 1;
        for (SolarAccessoryWaypoint point : device.getWaypoints()) {
            point.setId(UUID.randomUUID().toString());
            point.setDeviceId(device.getId());
            point.setSortOrder(index++);
            if (waypoints.insert(point) != 1) throw new IllegalStateException("航点保存失败");
        }
        return device;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id, String orthophotoId) {
        requireDevice(id, orthophotoId);
        waypoints.delete(new LambdaQueryWrapper<SolarAccessoryWaypoint>().eq(SolarAccessoryWaypoint::getDeviceId, id));
        if (devices.deleteById(id) != 1) throw new IllegalStateException("设备删除失败");
    }

    /**
     * 正射图级联清理：删除该图下全部附属设备及其预置航点。
     * 供正射图删除链路调用；航点表不冗余正射图ID，故先查设备ID再删子表。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByOrthophoto(String orthophotoId) {
        text(orthophotoId, "正射图ID", 64);
        List<SolarAccessory> list = devices.selectList(new LambdaQueryWrapper<SolarAccessory>()
                .eq(SolarAccessory::getOrthophotoId, orthophotoId));
        if (list != null && !list.isEmpty()) {
            List<String> ids = list.stream().map(SolarAccessory::getId).collect(Collectors.toList());
            waypoints.delete(new LambdaQueryWrapper<SolarAccessoryWaypoint>()
                    .in(SolarAccessoryWaypoint::getDeviceId, ids));
        }
        devices.delete(new LambdaQueryWrapper<SolarAccessory>().eq(SolarAccessory::getOrthophotoId, orthophotoId));
    }

    private void requirePhoto(String id) {
        text(id, "正射图ID", 64);
        if (photos.selectById(id) == null) throw new IllegalArgumentException("正射图不存在");
    }

    private void requireDevice(String id, String photoId) {
        text(id, "设备ID", 64);
        SolarAccessory existing = devices.selectById(id);
        if (existing == null || !Objects.equals(existing.getOrthophotoId(), photoId)) {
            throw new IllegalArgumentException("设备不存在或不属于当前正射图");
        }
    }

    private static void validate(SolarAccessory device) {
        if (device == null) throw new IllegalArgumentException("设备不能为空");
        text(device.getOrthophotoId(), "正射图ID", 64);
        text(device.getName(), "设备名称", 100);
        if (!TYPES.contains(device.getDeviceType())) throw new IllegalArgumentException("设备类型无效");
        position(device.getLongitude(), device.getLatitude(), device.getHeight());
        if (device.getEnabled() == null) throw new IllegalArgumentException("设备启用状态不能为空");
        if (device.getWaypoints() == null || device.getWaypoints().size() > 200) throw new IllegalArgumentException("航点数量应为0至200个");
        for (SolarAccessoryWaypoint point : device.getWaypoints()) {
            if (point == null) throw new IllegalArgumentException("航点不能为空");
            text(point.getName(), "航点名称", 100);
            position(point.getLongitude(), point.getLatitude(), point.getHeight());
            range(point.getHeading(), -180, 360, "机头偏航角");
            range(point.getPitch(), -90, 30, "云台俯仰角");
            range(point.getFocalLength(), 0.1, 2000, "焦距");
            if (!Arrays.asList("take_photo", "none").contains(point.getAction())) throw new IllegalArgumentException("拍摄动作无效");
            if (!Arrays.asList("visible", "ir", "both").contains(point.getImageFormat())) throw new IllegalArgumentException("拍摄模式无效");
            if (point.getEnabled() == null) throw new IllegalArgumentException("航点启用状态不能为空");
        }
    }

    private static void position(Double lon, Double lat, Double height) {
        range(lon, -180, 180, "经度");
        range(lat, -90, 90, "纬度");
        range(height, -1000, 10000, "椭球高");
    }

    private static void range(Double value, double min, double max, String name) {
        if (value == null || !Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(name + "应在" + min + "至" + max + "之间");
        }
    }

    private static void text(String value, String name, int max) {
        if (value == null || value.trim().isEmpty() || value.length() > max) throw new IllegalArgumentException(name + "不能为空且长度不能超过" + max);
    }
}
