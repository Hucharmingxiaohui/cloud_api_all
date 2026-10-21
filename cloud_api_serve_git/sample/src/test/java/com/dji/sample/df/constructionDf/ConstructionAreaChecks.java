package com.dji.sample.df.constructionDf;

import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.Point;
import com.dji.sample.df.constructionDf.dao.ConstructionAreaMapper;
import com.dji.sample.df.constructionDf.dao.ConstructionAreaVersionMapper;
import com.dji.sample.df.constructionDf.model.entity.ConstructionAreaEntity;
import com.dji.sample.df.constructionDf.model.entity.ConstructionAreaVersionEntity;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Offline executable checks; no database, Spring context or map requests. */
public class ConstructionAreaChecks {
    private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }

    private static final Map<String, Object> STORE = new LinkedHashMap<>();
    private static final List<Object> WRITES = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (p, method, args) -> {
            switch (method.getName()) {
                case "selectById": return STORE.get("area");
                case "selectOne": return STORE.get("versionRow");
                case "selectList": return STORE.get("versionRows");
                case "insert":
                case "updateById":
                case "update": WRITES.add(args[0]); return 1;
                default: return null;
            }
        });
    }

    private static Point p(double lng, double lat) { return new Point(lng, lat); }

    private static ConstructionAreaEntity area(String id, String workspace, int version) {
        ConstructionAreaEntity entity = new ConstructionAreaEntity();
        entity.setId(id);
        entity.setWorkspaceId(workspace);
        entity.setName("测试区域");
        entity.setCategory("基础施工");
        entity.setEnabled(true);
        entity.setArchived(false);
        entity.setCurrentVersion(version);
        return entity;
    }

    public static void main(String[] args) {
        // Geometry rules mirror the frontend thresholds.
        check(ConstructionAreaGeometry.validate(List.of(p(0, 0), p(1, 1), p(0, 1), p(1, 0))).contains("交叉"), "bowtie rejected");
        check(ConstructionAreaGeometry.validate(List.of(p(0, 0), p(1, 1), p(2, 2))).contains("面积过小"), "collinear rejected");
        check(ConstructionAreaGeometry.validate(List.of(p(181, 0), p(180, 1), p(179, 0))).contains("坐标无效"), "range rejected");
        check(ConstructionAreaGeometry.validate(List.of(p(0, 0), p(1, 0), p(1, 1), p(0, 0))).contains("重复顶点"), "duplicate rejected");
        check(ConstructionAreaGeometry.validate(List.of(p(0, 0), p(1, 0), p(1, 1), p(0.5, 0.5), p(0, 1))).isEmpty(), "concave accepted");
        check(ConstructionAreaGeometry.areaSquareMeters(List.of(p(121, 37), p(121.001, 37), p(121.001, 37.001), p(121, 37.001))) > 8000, "area computed");
        List<Point> parsed = ConstructionAreaGeometry.parse(ConstructionAreaGeometry.toJson(List.of(p(121.5, 37.25), p(122, 37), p(121, 36))));
        check(parsed.size() == 3 && Math.abs(parsed.get(0).lng - 121.5) < 1e-9, "boundary json roundtrip");

        // Service behavior: create starts at v1, stale expected_version conflicts as 409, cross-workspace 404, archive keeps the row.
        STORE.clear();
        WRITES.clear();
        ConstructionAreaEntity stored = area("a1", "ws1", 1);
        STORE.put("area", stored);
        ConstructionAreaVersionEntity versionRow = new ConstructionAreaVersionEntity();
        versionRow.setBoundary(ConstructionAreaGeometry.toJson(List.of(p(121, 37), p(121.01, 37), p(121.01, 37.01))));
        versionRow.setAreaSqm(ConstructionAreaGeometry.areaSquareMeters(ConstructionAreaGeometry.parse(versionRow.getBoundary())));
        STORE.put("versionRow", versionRow);
        STORE.put("versionRows", new ArrayList<>(List.of(versionRow)));
        ConstructionAreaService service = new ConstructionAreaService(proxy(ConstructionAreaMapper.class), proxy(ConstructionAreaVersionMapper.class));

        Map<String, Object> body = Map.of(
                "name", "测试区域", "category", "基础施工", "enabled", true,
                "boundary", List.of(Map.of("lng", 121.0, "lat", 37.0), Map.of("lng", 121.01, "lat", 37.0), Map.of("lng", 121.01, "lat", 37.01)));
        Map<String, Object> created = service.create("ws1", body);
        check(Integer.valueOf(1).equals(created.get("version")), "create reports stored version");
        check(!WRITES.isEmpty(), "create appends rows");

        stored.setCurrentVersion(2);
        try {
            Map<String, Object> stale = new LinkedHashMap<>(body);
            stale.put("expected_version", 1);
            service.update("ws1", "a1", stale);
            throw new AssertionError("stale version must conflict");
        } catch (ConstructionAreaGeometry.RuleException e) {
            check(e.getCode() == 409, "conflict maps to 409, got " + e.getCode());
        }
        try {
            service.update("ws1", "a1", body);
            throw new AssertionError("missing expected_version must conflict");
        } catch (ConstructionAreaGeometry.RuleException e) {
            check(e.getCode() == 409, "missing expected_version treated as conflict");
        }
        try {
            service.update("ws2", "a1", Map.of("expected_version", 2));
            throw new AssertionError("cross-workspace update must be rejected");
        } catch (ConstructionAreaGeometry.RuleException e) {
            check(e.getCode() == 404, "cross-workspace maps to 404, got " + e.getCode());
        }
        Map<String, Object> fresh = new LinkedHashMap<>(body);
        fresh.put("expected_version", 2);
        Map<String, Object> updated = service.update("ws1", "a1", fresh);
        check(Integer.valueOf(3).equals(updated.get("version")), "update bumps to next version");
        check(stored.getCurrentVersion() == 3, "master row carries new current version");

        service.archive("ws1", "a1", true);
        check(Boolean.TRUE.equals(stored.getArchived()), "archive flips flag without delete");
        service.archive("ws1", "a1", false);
        check(Boolean.FALSE.equals(stored.getArchived()), "restore clears flag");

        System.out.println("ConstructionAreaChecks passed: geometry thresholds, json roundtrip, create v1, conflict 409, cross-workspace 404, version bump, archive toggle");
    }
}
