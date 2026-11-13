package com.dji.sample.df.centerOrdinary;

import com.alibaba.fastjson.JSON;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.mediaDf.OrdinaryPhotoMatcher;
import org.springframework.util.StringUtils;
import java.util.*;
import java.util.stream.Collectors;

public final class CenterOrdinarySelection {
    private CenterOrdinarySelection() { }

    public static List<String> codes(String text) {
        if (!StringUtils.hasText(text)) throw new IllegalArgumentException("未选择点位编码");
        List<String> result = Arrays.stream(text.split(",", -1)).map(String::trim).sorted().collect(Collectors.toList());
        if (result.stream().anyMatch(String::isEmpty) || new HashSet<>(result).size() != result.size())
            throw new IllegalArgumentException("点位编码为空或重复");
        return result;
    }

    public static String label(String name) {
        if (!StringUtils.hasText(name)) throw new IllegalArgumentException("点位名称不能为空");
        return name;
    }

    public static String freeze(String requested, String subCode, List<UniPoint> rows) {
        List<String> codes = codes(requested);
        if (!StringUtils.hasText(subCode) || rows == null || rows.size() != codes.size())
            throw new IllegalArgumentException("所选点位未全部解析或点位编码不唯一");
        Set<String> found = new HashSet<>(), routes = new HashSet<>();
        for (UniPoint p : rows) {
            if (!subCode.equals(p.getSubCode()) || !codes.contains(p.getPointCode()) || !found.add(p.getPointCode()))
                throw new IllegalArgumentException("所选点位不属于本站或编码不唯一");
            if (!StringUtils.hasText(p.getWaylineId())) throw new IllegalArgumentException("所选点位未关联航线");
            routes.add(p.getWaylineId());
            if (!Objects.equals(p.getPicType(), 0) && !Objects.equals(p.getPicType(), 1))
                throw new IllegalArgumentException("点位照片类型必须为可见光0或红外1");
            label(p.getPointName());
        }
        if (routes.size() != 1) throw new IllegalArgumentException("所选点位必须且只能关联一条航线");
        OrdinaryPhotoMatcher.validatePoints(rows.get(0).getWaylineId(), rows);
        return JSON.toJSONString(rows);
    }

    public static String freezeRoute(String requested, String subCode, List<UniPoint> selected, List<UniPoint> routePoints) {
        freeze(requested, subCode, selected);
        String route=selected.get(0).getWaylineId();
        if (routePoints==null || routePoints.isEmpty()) throw new IllegalArgumentException("缺少全航线点位快照");
        Set<String> found=new HashSet<>();
        for (UniPoint p:routePoints) {
            if (!Objects.equals(route,p.getWaylineId()) || !Objects.equals(subCode,p.getSubCode()) || !found.add(p.getPointCode()))
                throw new IllegalArgumentException("全航线点位厂站、航线或编码不唯一");
        }
        for (UniPoint p:selected) if (routePoints.stream().noneMatch(q -> Objects.equals(p.getPointCode(),q.getPointCode())
                && Objects.equals(p.getId(),q.getId()) && Objects.equals(p.getPointName(),q.getPointName())
                && Objects.equals(p.getPicType(),q.getPicType()) && Objects.equals(p.getWaylinePointPos(),q.getWaylinePointPos())))
            throw new IllegalArgumentException("选中点位与全航线快照不一致");
        OrdinaryPhotoMatcher.validatePoints(route,routePoints);
        Map<String,Object> snapshot=new LinkedHashMap<>();
        snapshot.put("version",2); snapshot.put("selectedCodes",codes(requested)); snapshot.put("routePoints",routePoints);
        return JSON.toJSONString(snapshot);
    }

    public static List<UniPoint> routePoints(String snapshot, String subCode, String route) {
        if (snapshot==null || !snapshot.startsWith("{")) throw new IllegalStateException("旧记录缺少全航线冻结快照，禁止使用实时点表或仅选中点位匹配");
        com.alibaba.fastjson.JSONObject data=JSON.parseObject(snapshot);
        if (!Integer.valueOf(2).equals(data.getInteger("version")) || data.getJSONArray("routePoints")==null)
            throw new IllegalStateException("缺少版本2全航线冻结快照");
        List<UniPoint> rows=data.getJSONArray("routePoints").toJavaList(UniPoint.class);
        if (rows.isEmpty() || rows.stream().anyMatch(p -> !Objects.equals(subCode,p.getSubCode()) || !Objects.equals(route,p.getWaylineId())))
            throw new IllegalStateException("全航线冻结快照与任务厂站或航线不一致");
        return rows;
    }

}
