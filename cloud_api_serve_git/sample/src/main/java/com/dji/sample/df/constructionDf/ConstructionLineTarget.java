package com.dji.sample.df.constructionDf;

import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.Point;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** Explicit line namespace in shared task snapshots; never creates or updates construction_area. */
@Service
public class ConstructionLineTarget {
    private final ConstructionLineService lines;
    private final JdbcTemplate db;
    public ConstructionLineTarget(ConstructionLineService lines,JdbcTemplate db) { this.lines=lines; this.db=db; }
    public static boolean isLine(String id) { return id!=null && id.startsWith("line:"); }
    public static String key(String id) {
        if(id==null||!id.matches("[a-zA-Z0-9-]{1,59}")) throw new RuleException(400,"线路编号无效");
        return "line:"+id;
    }
    private static String raw(String id) {
        if(!isLine(id)) throw new RuleException(400,"缺少线路类型标识");
        String raw=id.substring(5); key(raw); return raw;
    }
    public Map<String,Object> detail(String w,String id) {
        Map<String,Object> value=new LinkedHashMap<>(lines.detail(w,raw(id)));
        value.put("id",id); value.put("target_type","line");
        // Legacy analysis protocol uses area_id as the opaque grouping key, including line:<id>.
        List<Map<String,Object>> points=new ArrayList<>();
        for(Object item:(List<?>)value.get("points")) {
            Point p=(Point)item;
            points.add(Map.of("lng",p.lng,"lat",p.lat));
        }
        value.put("boundary",points); value.put("category",value.get("type"));
        value.put("name","线路 · "+value.get("name"));
        return value;
    }
    public Map<String,Object> lock(String w,String id) {
        if(db.queryForList("SELECT id FROM construction_line WHERE workspace_id=? AND id=? FOR UPDATE",w,raw(id)).isEmpty()) throw new RuleException(404,"线路不存在");
        return detail(w,id);
    }
    public static void validateWaypoints(List<Point> points) {
        if(points==null||points.size()<2||points.size()>2000) throw new RuleException(400,"航线需包含2–2000个航点");
        for(Point p:points) if(p==null||!Double.isFinite(p.lng)||!Double.isFinite(p.lat)||Math.abs(p.lng)>180||Math.abs(p.lat)>90) throw new RuleException(400,"航点坐标无效");
    }
}
