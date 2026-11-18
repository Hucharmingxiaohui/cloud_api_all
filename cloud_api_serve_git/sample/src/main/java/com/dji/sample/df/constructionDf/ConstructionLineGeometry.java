package com.dji.sample.df.constructionDf;

import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.Point;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConstructionLineGeometry {
    private ConstructionLineGeometry() { }

    public static String validate(List<Point> points) {
        if (points == null || points.size() < 2) return "线路至少需要两个折点";
        if (points.size() > 200) return "线路最多支持200个折点";
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            if (p == null || !Double.isFinite(p.lng) || !Double.isFinite(p.lat)
                    || Math.abs(p.lng) > 180 || Math.abs(p.lat) > 90) return "线路坐标无效";
            if (i > 0 && p.lng == points.get(i - 1).lng && p.lat == points.get(i - 1).lat) return "线路相邻折点不能重合";
        }
        return lengthMeters(points) > 0 ? "" : "线路长度必须大于零";
    }

    public static List<Point> parseInput(Object raw) {
        if (!(raw instanceof List)) throw new RuleException(400, "请绘制线路折点");
        List<Point> points = new ArrayList<>();
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) throw new RuleException(400, "线路坐标无效");
            Map<?, ?> p = (Map<?, ?>) item;
            if (!(p.get("lng") instanceof Number) || !(p.get("lat") instanceof Number)) throw new RuleException(400, "线路坐标无效");
            points.add(new Point(((Number) p.get("lng")).doubleValue(), ((Number) p.get("lat")).doubleValue()));
        }
        String error = validate(points);
        if (!error.isEmpty()) throw new RuleException(400, error);
        return points;
    }

    public static double lengthMeters(List<Point> points) {
        double length = 0;
        for (int i = 1; i < points.size(); i++) {
            Point a = points.get(i - 1), b = points.get(i);
            double h = Math.pow(Math.sin(Math.toRadians(b.lat - a.lat) / 2), 2)
                    + Math.cos(Math.toRadians(a.lat)) * Math.cos(Math.toRadians(b.lat))
                    * Math.pow(Math.sin(Math.toRadians(b.lng - a.lng) / 2), 2);
            length += 6371008.8 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, h))));
        }
        return length;
    }

    /** Tower natural sort key: letter prefix then numeric suffix, so A2 sorts before A10. */
    public static String sortKey(String towerNo) {
        String no = towerNo == null ? "" : towerNo.trim();
        int split = no.length();
        while (split > 0 && Character.isDigit(no.charAt(split - 1))) split--;
        String digits = no.substring(split);
        return no.substring(0, split) + String.format("%010d", digits.isEmpty() ? 0 : Long.parseLong(digits));
    }

    /** "A2-A3" span label from two consecutive tower numbers. */
    public static String spanLabel(String from, String to) { return from + "-" + to; }

    private static double distanceMeters(Point a, Point b) {
        double h = Math.pow(Math.sin(Math.toRadians(b.lat - a.lat) / 2), 2)
                + Math.cos(Math.toRadians(a.lat)) * Math.cos(Math.toRadians(b.lat))
                * Math.pow(Math.sin(Math.toRadians(b.lng - a.lng) / 2), 2);
        return 6371008.8 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, h))));
    }

    private static double distancePointToSegment(Point p, Point a, Point b) {
        double scale = Math.max(1e-9, Math.cos(Math.toRadians(p.lat)));
        double px = p.lng * scale, py = p.lat, ax = a.lng * scale, ay = a.lat, bx = b.lng * scale, by = b.lat;
        double dx = bx - ax, dy = by - ay;
        double t = dx == 0 && dy == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)));
        return distanceMeters(p, new Point((ax + dx * t) / scale, ay + dy * t));
    }

    /**
     * Project a waypoint onto the line polyline and return the index of the nearest segment
     * (segment i connects point i and point i+1). Used to pre-fill the tower span "A2-A3".
     */
    public static int nearestSpanIndex(List<Point> points, Point p) {
        int best = 0; double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i + 1 < points.size(); i++) {
            double d = distancePointToSegment(p, points.get(i), points.get(i + 1));
            if (d < bestDistance) { bestDistance = d; best = i; }
        }
        return best;
    }
}
