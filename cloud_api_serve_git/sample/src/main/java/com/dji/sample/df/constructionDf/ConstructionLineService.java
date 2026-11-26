package com.dji.sample.df.constructionDf;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.Point;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.df.constructionDf.dao.ConstructionLineMapper;
import com.dji.sample.df.constructionDf.dao.ConstructionLineVersionMapper;
import com.dji.sample.df.constructionDf.model.entity.ConstructionLineEntity;
import com.dji.sample.df.constructionDf.model.entity.ConstructionLineVersionEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class ConstructionLineService {
    private final ConstructionLineMapper lines;
    private final ConstructionLineVersionMapper versions;
    private final org.springframework.jdbc.core.JdbcTemplate db;
    public ConstructionLineService(ConstructionLineMapper lines, ConstructionLineVersionMapper versions) { this(lines, versions, null); }
    @org.springframework.beans.factory.annotation.Autowired
    public ConstructionLineService(ConstructionLineMapper lines, ConstructionLineVersionMapper versions, org.springframework.jdbc.core.JdbcTemplate db) {
        this.lines = lines; this.versions = versions; this.db = db;
    }
    private void workspace(String workspace) {
        if (workspace == null || workspace.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录工作空间");
    }
    private ConstructionLineEntity require(String workspace, String id) {
        workspace(workspace);
        ConstructionLineEntity line = lines.selectById(id);
        if (line == null || !workspace.equals(line.getWorkspaceId())) throw new RuleException(404, "线路不存在");
        return line;
    }
    public List<Map<String, Object>> list(String workspace, String keyword, Boolean enabled, boolean archived) {
        workspace(workspace);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ConstructionLineEntity line : lines.selectList(new LambdaQueryWrapper<ConstructionLineEntity>()
                .eq(ConstructionLineEntity::getWorkspaceId, workspace)
                .eq(ConstructionLineEntity::getArchived, archived)
                .eq(enabled != null, ConstructionLineEntity::getEnabled, enabled)
                .like(keyword != null && !keyword.isBlank(), ConstructionLineEntity::getName, keyword)
                .orderByDesc(ConstructionLineEntity::getGmtModified))) result.add(current(line));
        return result;
    }
    private ConstructionLineVersionEntity version(String id, int version) {
        ConstructionLineVersionEntity row = versions.selectOne(new LambdaQueryWrapper<ConstructionLineVersionEntity>()
                .eq(ConstructionLineVersionEntity::getLineId, id).eq(ConstructionLineVersionEntity::getVersion, version));
        if (row == null) throw new RuleException(404, "线路版本不存在");
        return row;
    }
    private Map<String, Object> current(ConstructionLineEntity line) {
        Map<String, Object> data = snapshot(version(line.getId(), line.getCurrentVersion()));
        data.put("gmt_create", line.getGmtCreate()); data.put("gmt_modified", line.getGmtModified());
        data.put("tower_ids", line.getTowerIds() == null || line.getTowerIds().isBlank() ? null : ConstructionAreaGeometry.parseTowerNos(line.getTowerIds()));
        return data;
    }
    public Map<String, Object> detail(String workspace, String id) { return current(require(workspace, id)); }
    public List<Map<String, Object>> history(String workspace, String id) {
        require(workspace, id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ConstructionLineVersionEntity row : versions.selectList(new LambdaQueryWrapper<ConstructionLineVersionEntity>()
                .eq(ConstructionLineVersionEntity::getLineId, id).orderByDesc(ConstructionLineVersionEntity::getVersion))) result.add(snapshot(row));
        return result;
    }
    private String text(Object value, String label, int max) {
        if (!(value instanceof String)) throw new RuleException(400, label + "无效");
        String text = ((String) value).trim();
        if (text.length() > max) throw new RuleException(400, label + "不能超过" + max + "字");
        return text;
    }
    private void fields(ConstructionLineEntity line, Map<String, Object> body) {
        String name = text(body.get("name"), "线路名称", 100);
        String type = text(body.get("type"), "线路类型", 32);
        String color = text(body.get("color"), "线路颜色", 7);
        if (name.isEmpty() || type.isEmpty()) throw new RuleException(400, "请填写线路名称和类型");
        if (!color.matches("#[0-9a-fA-F]{6}")) throw new RuleException(400, "线路颜色必须为#RRGGBB");
        if (!(body.get("enabled") instanceof Boolean)) throw new RuleException(400, "线路启用状态无效");
        line.setName(name); line.setType(type); line.setColor(color.toUpperCase(Locale.ROOT));
        line.setEnabled((Boolean) body.get("enabled"));
        line.setNotes(body.get("notes") == null ? "" : text(body.get("notes"), "备注", 500));
    }
    private void expected(ConstructionLineEntity line, Object expected) {
        if (!(expected instanceof Number) || !Double.isFinite(((Number) expected).doubleValue())
                || ((Number) expected).doubleValue() != line.getCurrentVersion()) throw new RuleException(409, "线路已更新（当前 v" + line.getCurrentVersion() + "），请载入最新线路");
    }
    @Transactional
    public Map<String, Object> create(String workspace, Map<String, Object> body) {
        workspace(workspace);
        ConstructionLineEntity line = new ConstructionLineEntity(); fields(line, body);
        List<Point> points; List<String> towerIds = towerIds(body); List<String> towerNos = null;
        if (towerIds != null) {
            TowerChain chain = towerChain(workspace, towerIds);
            points = chain.points; towerNos = chain.nos;
        } else points = ConstructionLineGeometry.parseInput(body.get("points"));
        line.setId(UUID.randomUUID().toString()); line.setWorkspaceId(workspace); line.setCurrentVersion(1); line.setArchived(false);
        line.setTowerIds(towerIds == null ? null : ConstructionAreaGeometry.toJsonStrings(towerIds));
        line.setGmtCreate(new Date()); line.setGmtModified(line.getGmtCreate()); lines.insert(line); append(line, points, towerNos);
        return current(line);
    }
    @Transactional
    public Map<String, Object> update(String workspace, String id, Map<String, Object> body) {
        ConstructionLineEntity line = require(workspace, id); expected(line, body.get("expected_version"));
        fields(line, body);
        List<String> towerIds = towerIds(body); List<String> towerNos = null; List<Point> points;
        if (towerIds == null && line.getTowerIds() != null && !line.getTowerIds().isBlank()) {
            // 杆塔链线路未重传杆塔时，按既有杆塔刷新几何（杆塔坐标可能已调整）
            towerIds = ConstructionAreaGeometry.parseTowerNos(line.getTowerIds());
        }
        if (towerIds != null) { TowerChain chain = towerChain(workspace, towerIds); points = chain.points; towerNos = chain.nos; }
        else points = ConstructionLineGeometry.parseInput(body.get("points"));
        line.setTowerIds(towerIds == null ? null : ConstructionAreaGeometry.toJsonStrings(towerIds));
        advance(line); append(line, points, towerNos); return current(line);
    }
    @Transactional
    public Map<String, Object> archive(String workspace, String id, Map<String, Object> body) {
        ConstructionLineEntity line = require(workspace, id); expected(line, body.get("expected_version"));
        if (!(body.get("archived") instanceof Boolean)) throw new RuleException(400, "归档状态无效");
        ConstructionLineVersionEntity current = version(id, line.getCurrentVersion());
        List<Point> points = ConstructionAreaGeometry.parse(current.getPoints());
        List<String> towerNos = current.getTowerNos() == null ? null : ConstructionAreaGeometry.parseTowerNos(current.getTowerNos());
        line.setArchived((Boolean) body.get("archived")); advance(line); append(line, points, towerNos); return current(line);
    }
    private void advance(ConstructionLineEntity line) {
        int previous = line.getCurrentVersion(); line.setCurrentVersion(previous + 1); line.setGmtModified(new Date());
        if (lines.update(line, new LambdaUpdateWrapper<ConstructionLineEntity>()
                .eq(ConstructionLineEntity::getId, line.getId()).eq(ConstructionLineEntity::getWorkspaceId, line.getWorkspaceId())
                .eq(ConstructionLineEntity::getCurrentVersion, previous)) != 1) throw new RuleException(409, "线路正在被其他端更新，请刷新重试");
    }
    private void append(ConstructionLineEntity line, List<Point> points, List<String> towerNos) {
        ConstructionLineVersionEntity row = new ConstructionLineVersionEntity();
        row.setId(UUID.randomUUID().toString()); row.setLineId(line.getId()); row.setVersion(line.getCurrentVersion());
        row.setName(line.getName()); row.setType(line.getType()); row.setColor(line.getColor()); row.setEnabled(line.getEnabled());
        row.setNotes(line.getNotes()); row.setArchived(line.getArchived()); row.setPoints(ConstructionAreaGeometry.toJson(points));
        row.setTowerNos(towerNos == null ? null : ConstructionAreaGeometry.toJsonStrings(towerNos));
        row.setLengthM(ConstructionLineGeometry.lengthMeters(points)); row.setGmtCreate(new Date()); versions.insert(row);
    }
    private Map<String, Object> snapshot(ConstructionLineVersionEntity row) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("line_id", row.getLineId()); data.put("version", row.getVersion()); data.put("name", row.getName());
        data.put("type", row.getType()); data.put("color", row.getColor()); data.put("enabled", row.getEnabled());
        data.put("notes", row.getNotes()); data.put("archived", row.getArchived()); data.put("length_m", row.getLengthM());
        data.put("tower_nos", row.getTowerNos() == null ? null : ConstructionAreaGeometry.parseTowerNos(row.getTowerNos()));
        data.put("points", ConstructionAreaGeometry.parse(row.getPoints())); data.put("gmt_create", row.getGmtCreate());
        return data;
    }

    /** tower_ids 为空视为手绘线路；否则解析为去重字符串列表。 */
    private List<String> towerIds(Map<String, Object> body) {
        Object raw = body.get("tower_ids");
        if (raw == null) return null;
        if (!(raw instanceof List) || ((List<?>) raw).isEmpty() || ((List<?>) raw).size() > 200) throw new RuleException(400, "杆塔链需包含2至200个杆塔");
        List<String> ids = new ArrayList<>();
        for (Object item : (List<?>) raw) {
            if (!(item instanceof String)) throw new RuleException(400, "杆塔链包含无效杆塔");
            String id = ((String) item).trim();
            if (id.isEmpty()) throw new RuleException(400, "杆塔链包含无效杆塔");
            if (ids.contains(id)) throw new RuleException(400, "杆塔链中杆塔重复");
            ids.add(id);
        }
        return ids;
    }

    /** 杆塔链解析结果：折点与杆塔编号（快照用）。 */
    private static final class TowerChain {
        final List<Point> points; final List<String> nos;
        TowerChain(List<Point> points, List<String> nos) { this.points = points; this.nos = nos; }
    }

    /** 按用户提交的连接顺序生成折点，允许跨编号前缀。 */
    private TowerChain towerChain(String workspace, List<String> towerIds) {
        if (db == null) throw new RuleException(500, "内部错误：杆塔存储不可用");
        String placeholders = String.join(",", java.util.Collections.nCopies(towerIds.size(), "?"));
        List<Object> args = new ArrayList<>(List.of(workspace)); args.addAll(towerIds);
        Map<String, String[]> towers = new HashMap<>();
        for (Map<String, Object> row : db.queryForList("SELECT id,tower_no,lng,lat FROM construction_tower WHERE workspace_id=? AND id IN (" + placeholders + ")", args.toArray())) {
            towers.put(String.valueOf(row.get("id")), new String[]{String.valueOf(row.get("tower_no")), String.valueOf(row.get("lng")), String.valueOf(row.get("lat"))});
        }
        if (towers.size() != towerIds.size()) throw new RuleException(404, "部分杆塔不存在或已删除，请刷新杆塔列表");
        List<String> ordered = new ArrayList<>(towerIds);
        List<Point> points = new ArrayList<>();
        List<String> nos = new ArrayList<>();
        for (String id : ordered) {
            String[] t = towers.get(id);
            points.add(new Point(Double.parseDouble(t[1]), Double.parseDouble(t[2]))); nos.add(t[0]);
        }
        String error = ConstructionLineGeometry.validate(points);
        if (!error.isEmpty()) throw new RuleException(400, error);
        return new TowerChain(points, nos);
    }
}
