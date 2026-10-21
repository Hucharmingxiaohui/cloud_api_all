package com.dji.sample.df.constructionDf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-side mirror of frontend constructionModel.validateBoundary.
 * Coordinates are WGS84 degrees; thresholds must stay identical to the TS version.
 */
public final class ConstructionAreaGeometry {
    private ConstructionAreaGeometry() {
    }

    /** Business rule violation; controller maps it to an error response body. */
    public static class RuleException extends RuntimeException {
        private final int code;

        public RuleException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() { return code; }
    }

    public static final class Point {
        public final double lng;
        public final double lat;

        public Point(double lng, double lat) {
            this.lng = lng;
            this.lat = lat;
        }
    }

    public static String validate(List<Point> points) {
        if (points == null || points.size() < 3) return "请绘制至少包含三个顶点的区域";
        if (points.size() > 100) return "区域最多支持100个顶点";
        for (Point p : points) {
            if (p == null || !Double.isFinite(p.lng) || !Double.isFinite(p.lat)
                    || Math.abs(p.lng) > 180 || Math.abs(p.lat) > 85) return "区域坐标无效，请检查经纬度";
        }
        Set<String> unique = new HashSet<>();
        for (Point p : points) unique.add(p.lng + "," + p.lat);
        if (unique.size() != points.size()) return "区域包含重复顶点";
        int n = points.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (j == i + 1 || (i == 0 && j == n - 1)) continue;
                if (intersects(points.get(i), points.get((i + 1) % n), points.get(j), points.get((j + 1) % n))) {
                    return "区域边界不能交叉或重叠";
                }
            }
        }
        if (areaSquareMeters(points) < 10) return "区域面积过小或顶点共线，请重新绘制";
        return "";
    }

    public static List<Point> validated(List<Point> points) {
        String error = validate(points);
        if (!error.isEmpty()) throw new RuleException(400, error);
        return points;
    }

    public static double areaSquareMeters(List<Point> points) {
        if (points == null || points.size() < 3) return 0;
        Point origin = points.get(0);
        double xScale = 111320 * Math.cos(origin.lat * Math.PI / 180);
        double sum = 0;
        for (int i = 0; i < points.size(); i++) {
            Point a = points.get(i);
            Point b = points.get((i + 1) % points.size());
            double ax = (a.lng - origin.lng) * xScale;
            double ay = (a.lat - origin.lat) * 110540;
            double bx = (b.lng - origin.lng) * xScale;
            double by = (b.lat - origin.lat) * 110540;
            sum += ax * by - bx * ay;
        }
        return Math.abs(sum) / 2;
    }

    private static double orientation(Point a, Point b, Point c) {
        return (b.lng - a.lng) * (c.lat - a.lat) - (b.lat - a.lat) * (c.lng - a.lng);
    }

    private static boolean onSegment(Point p, Point q, Point r) {
        return Math.abs(orientation(p, q, r)) < 1e-12
                && r.lng >= Math.min(p.lng, q.lng) && r.lng <= Math.max(p.lng, q.lng)
                && r.lat >= Math.min(p.lat, q.lat) && r.lat <= Math.max(p.lat, q.lat);
    }

    private static boolean intersects(Point a, Point b, Point c, Point d) {
        return (orientation(a, b, c) * orientation(a, b, d) < 0 && orientation(c, d, a) * orientation(c, d, b) < 0)
                || onSegment(a, b, c) || onSegment(a, b, d) || onSegment(c, d, a) || onSegment(c, d, b);
    }

    private static final ObjectMapper JSON = new ObjectMapper();

    public static List<Point> parse(String boundaryJson) {
        if (boundaryJson == null || boundaryJson.isEmpty()) throw new RuleException(404, "区域边界缺失");
        try {
            List<Map<String, Object>> raw = JSON.readValue(boundaryJson, new TypeReference<List<Map<String, Object>>>() { });
            List<Point> points = new ArrayList<>(raw.size());
            for (Map<String, Object> item : raw) {
                if (item == null || !(item.get("lng") instanceof Number) || !(item.get("lat") instanceof Number)) {
                    throw new RuleException(404, "区域边界数据无效");
                }
                points.add(new Point(((Number) item.get("lng")).doubleValue(), ((Number) item.get("lat")).doubleValue()));
            }
            return points;
        } catch (RuleException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleException(404, "区域边界数据无效");
        }
    }

    public static String toJson(List<Point> points) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) builder.append(',');
            builder.append("{\"lng\":").append(points.get(i).lng).append(",\"lat\":").append(points.get(i).lat).append('}');
        }
        return builder.append(']').toString();
    }
}
