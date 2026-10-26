package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaPointOption;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CqEuaRoutePointService {
    private static final com.fasterxml.jackson.databind.ObjectReader SNAPSHOT_READER =
            new com.fasterxml.jackson.databind.ObjectMapper().readerFor(com.fasterxml.jackson.databind.JsonNode.class)
                    .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    @Resource
    private UniPointMapper2 uniPointMapper2;

    @Data
    public static class Selection {
        private String subCode;
        private String routeId;
        private String routeName;
        private String pointCodes;
        private List<Long> pointIds;
    }

    @Transactional(rollbackFor = Exception.class)
    public void bind(List<Long> ids, String name) {
        List<Long> selectedIds = ids(ids, true);
        if (!StringUtils.hasText(name) || name.trim().codePointCount(0, name.trim().length()) > 255)
            throw new IllegalArgumentException("EUA航线名称必填且不能超过255个字符");
        name = name.trim();
        List<UniPoint> initial = uniPointMapper2.selectBatchIds(selectedIds);
        requireIds(selectedIds, initial);
        String sub = initial.get(0).getSubCode();
        if (!StringUtils.hasText(sub)) throw new IllegalArgumentException("点位未配置场站编码");
        List<UniPoint> station = uniPointMapper2.selectStationForUpdate(sub);
        List<UniPoint> selected = station.stream().filter(p -> selectedIds.contains(p.getId())).collect(Collectors.toList());
        requireIds(selectedIds, selected);
        String route = normalizedRouteId(selected.get(0));
        if (!StringUtils.hasText(route)) throw new IllegalArgumentException("点位未配置EUA航线ID");
        for (UniPoint p : selected) {
            if (!sub.equals(p.getSubCode()) || !route.equals(normalizedRouteId(p)))
                throw new IllegalArgumentException("绑定点位必须属于同一场站、同一EUA航线ID");
        }
        for (UniPoint p : station) {
            String effectiveName = selectedIds.contains(p.getId()) ? name : p.getEuaRouteName();
            if (!StringUtils.hasText(effectiveName)) continue;
            if ((name.equalsIgnoreCase(effectiveName) && !route.equals(normalizedRouteId(p)))
                    || (route.equals(normalizedRouteId(p)) && !name.equals(effectiveName)))
                throw new IllegalArgumentException("场站内EUA航线名称与ID存在冲突，点位: " + p.getId());
        }
        if (uniPointMapper2.updateEuaRouteName(selectedIds, name) != selectedIds.size())
            throw new IllegalStateException("EUA航线名称保存失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Selection resolveIds(List<Long> requested) {
        List<Long> selectedIds = ids(requested, false);
        List<UniPoint> points = uniPointMapper2.selectBatchIds(selectedIds);
        requireIds(selectedIds, points);
        Map<Long, UniPoint> byId = points.stream().collect(Collectors.toMap(UniPoint::getId, p -> p));
        List<UniPoint> ordered = selectedIds.stream().map(byId::get).collect(Collectors.toList());
        Selection selection = selection(ordered);
        List<UniPoint> station = uniPointMapper2.selectStationForUpdate(selection.getSubCode());
        Map<Long, UniPoint> current = station.stream().collect(Collectors.toMap(UniPoint::getId, p -> p));
        ordered = selectedIds.stream().map(current::get).collect(Collectors.toList());
        if (ordered.contains(null)) throw new IllegalArgumentException("所选点位已变化，请刷新后重试");
        selection = selection(ordered);
        validateStation(ordered, station);
        return selection;
    }

    public Selection resolveSnapshot(String snapshot, String sub, String route, String name) {
        List<String> codes = parseCodes(snapshot);
        if (!StringUtils.hasText(sub) || !StringUtils.hasText(route) || !StringUtils.hasText(name))
            throw new IllegalArgumentException("请重新选择点位并保存计划");
        List<UniPoint> station = uniPointMapper2.selectList(new LambdaQueryWrapper<UniPoint>().eq(UniPoint::getSubCode, sub));
        List<UniPoint> selected = new ArrayList<>();
        for (String code : codes) {
            List<UniPoint> matches = station.stream().filter(p -> code.equals(p.getPointCode())).collect(Collectors.toList());
            if (matches.size() != 1) throw new IllegalArgumentException("计划点位缺失或重码: " + code + "，请重新选择点位并保存");
            selected.add(matches.get(0));
        }
        Selection result = selection(selected);
        validateStation(selected, station);
        if (!sub.equals(result.getSubCode()) || !route.equals(result.getRouteId()) || !name.equals(result.getRouteName()))
            throw new IllegalArgumentException("计划航线绑定已变化，请重新选择点位并保存");
        return result;
    }

    public List<CqEuaPointOption> pointOptions() {
        List<UniPoint> all = uniPointMapper2.selectList(new LambdaQueryWrapper<UniPoint>().orderByAsc(UniPoint::getId));
        Map<List<String>, List<UniPoint>> groups = new LinkedHashMap<>();
        for (UniPoint p : all) {
            if (StringUtils.hasText(p.getSubCode()) && StringUtils.hasText(p.getWaylineId()) && StringUtils.hasText(p.getEuaRouteName()))
                groups.computeIfAbsent(Arrays.asList(p.getSubCode(), normalizedRouteId(p)), k -> new ArrayList<>()).add(p);
        }
        List<CqEuaPointOption> options = new ArrayList<>();
        for (List<UniPoint> group : groups.values()) {
            try {
                Selection selected = selection(group);
                validateStation(group, all.stream().filter(p -> selected.getSubCode().equals(p.getSubCode())).collect(Collectors.toList()));
                CqEuaPointOption option = new CqEuaPointOption();
                option.setSubCode(selected.getSubCode()); option.setRouteId(selected.getRouteId()); option.setRouteName(selected.getRouteName());
                for (UniPoint p : group) {
                    CqEuaPointOption.Point child = new CqEuaPointOption.Point();
                    child.setId(p.getId()); child.setPointCode(p.getPointCode()); child.setPointName(p.getPointName());
                    option.getPoints().add(child);
                }
                options.add(option);
            } catch (IllegalArgumentException e) {
                log.warn("EUA可选点位组被排除: subCode={}, routeId={}, reason={}", group.get(0).getSubCode(), group.get(0).getWaylineId(), e.getMessage());
            }
        }
        return options;
    }

    public List<String> parseCodes(String snapshot) {
        try {
            com.fasterxml.jackson.databind.JsonNode parsed = SNAPSHOT_READER.readValue(snapshot);
            if (parsed == null || !parsed.isArray() || parsed.size() == 0) throw new IllegalArgumentException();
            List<String> result = new ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode node : parsed) {
                String value = node.isTextual() ? node.textValue() : null;
                if (!StringUtils.hasText(value) || !value.equals(value.trim()) || value.contains(",") || result.contains(value))
                    throw new IllegalArgumentException();
                result.add(value);
            }
            return result;
        } catch (java.io.IOException | RuntimeException e) {
            throw new IllegalArgumentException("计划点位快照无效，请重新选择点位并保存");
        }
    }

    private Selection selection(List<UniPoint> points) {
        UniPoint first = points.get(0);
        Selection result = new Selection();
        result.setSubCode(first.getSubCode()); result.setRouteId(normalizedRouteId(first)); result.setRouteName(first.getEuaRouteName());
        Set<String> codes = new LinkedHashSet<>();
        for (UniPoint p : points) {
            if (p.getId() == null || !StringUtils.hasText(p.getSubCode()) || !StringUtils.hasText(p.getWaylineId())
                    || !StringUtils.hasText(p.getEuaRouteName()) || !Objects.equals(result.getSubCode(), p.getSubCode())
                    || !Objects.equals(result.getRouteId(), normalizedRouteId(p)) || !Objects.equals(result.getRouteName(), p.getEuaRouteName()))
                throw new IllegalArgumentException("点位必须绑定同一场站、同一EUA航线ID及名称，点位: " + p.getId());
            if (!StringUtils.hasText(p.getPointCode()) || !p.getPointCode().equals(p.getPointCode().trim())
                    || p.getPointCode().contains(",") || !codes.add(p.getPointCode()))
                throw new IllegalArgumentException("点位业务编码为空、重复或无效，点位: " + p.getId());
        }
        result.setPointCodes(JSON.toJSONString(codes));
        if (result.getPointCodes().getBytes(StandardCharsets.UTF_8).length > 65535)
            throw new IllegalArgumentException("所选点位超出计划存储容量");
        result.setPointIds(points.stream().map(UniPoint::getId).collect(Collectors.toList()));
        return result;
    }

    private void validateStation(List<UniPoint> selected, List<UniPoint> station) {
        UniPoint first = selected.get(0);
        for (UniPoint p : station) {
            if (StringUtils.hasText(p.getEuaRouteName()) &&
                    ((first.getEuaRouteName().equalsIgnoreCase(p.getEuaRouteName()) && !normalizedRouteId(first).equals(normalizedRouteId(p)))
                    || (normalizedRouteId(first).equals(normalizedRouteId(p)) && !first.getEuaRouteName().equals(p.getEuaRouteName()))))
                throw new IllegalArgumentException("EUA航线名称与ID冲突，点位: " + p.getId());
        }
        for (UniPoint p : selected) {
            if (station.stream().filter(s -> p.getPointCode().equals(s.getPointCode())).count() != 1)
                throw new IllegalArgumentException("点位业务编码不唯一: " + p.getPointCode());
        }
    }

    private String normalizedRouteId(UniPoint point) {
        return point.getWaylineId() == null ? null : point.getWaylineId().trim();
    }

    private List<Long> ids(List<Long> requested, boolean rejectDuplicates) {
        if (requested == null || requested.isEmpty() || requested.stream().anyMatch(id -> id == null || id <= 0))
            throw new IllegalArgumentException("请选择有效点位");
        List<Long> result = new ArrayList<>(new LinkedHashSet<>(requested));
        if (rejectDuplicates && result.size() != requested.size()) throw new IllegalArgumentException("点位ID不能重复");
        return result;
    }

    private void requireIds(List<Long> ids, List<UniPoint> points) {
        Set<Long> actual = points.stream().map(UniPoint::getId).collect(Collectors.toSet());
        if (points.size() != ids.size() || !actual.equals(new HashSet<>(ids)))
            throw new IllegalArgumentException("所选点位不存在或跨场站，请刷新后重试");
    }
}
