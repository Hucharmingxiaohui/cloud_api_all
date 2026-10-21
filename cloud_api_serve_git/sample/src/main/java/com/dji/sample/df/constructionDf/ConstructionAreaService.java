package com.dji.sample.df.constructionDf;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.Point;
import com.dji.sample.df.constructionDf.dao.ConstructionAreaMapper;
import com.dji.sample.df.constructionDf.dao.ConstructionAreaVersionMapper;
import com.dji.sample.df.constructionDf.model.entity.ConstructionAreaEntity;
import com.dji.sample.df.constructionDf.model.entity.ConstructionAreaVersionEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Construction area CRUD. Updates append immutable version rows;
 * archived areas are kept, never deleted (plan doc section 7).
 */
@Service
public class ConstructionAreaService {
    private final ConstructionAreaMapper areas;
    private final ConstructionAreaVersionMapper versions;

    public ConstructionAreaService(ConstructionAreaMapper areas, ConstructionAreaVersionMapper versions) {
        this.areas = areas;
        this.versions = versions;
    }

    private void requireWorkspace(String workspace) {
        if (workspace == null || workspace.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录工作空间");
        }
    }

    private ConstructionAreaEntity require(String workspace, String id) {
        requireWorkspace(workspace);
        ConstructionAreaEntity entity = areas.selectById(id);
        if (entity == null || !workspace.equals(entity.getWorkspaceId())) {
            throw new ConstructionAreaGeometry.RuleException(404, "区域不存在");
        }
        return entity;
    }

    public List<Map<String, Object>> list(String workspace, String keyword, Boolean enabled, Boolean archived) {
        requireWorkspace(workspace);
        LambdaQueryWrapper<ConstructionAreaEntity> wrapper = new LambdaQueryWrapper<ConstructionAreaEntity>()
                .eq(ConstructionAreaEntity::getWorkspaceId, workspace)
                .eq(ConstructionAreaEntity::getArchived, Boolean.TRUE.equals(archived))
                .like(keyword != null && !keyword.isBlank(), ConstructionAreaEntity::getName, keyword)
                .eq(enabled != null, ConstructionAreaEntity::getEnabled, enabled)
                .orderByDesc(ConstructionAreaEntity::getGmtModified);
        List<ConstructionAreaEntity> entities = areas.selectList(wrapper);
        Map<String, ConstructionAreaVersionEntity> currentRows = new LinkedHashMap<>();
        if (!entities.isEmpty()) {
            List<String> areaIds = new ArrayList<>();
            for (ConstructionAreaEntity entity : entities) areaIds.add(entity.getId());
            for (ConstructionAreaVersionEntity row : versions.selectList(new LambdaQueryWrapper<ConstructionAreaVersionEntity>()
                    .in(ConstructionAreaVersionEntity::getAreaId, areaIds))) {
                currentRows.put(row.getAreaId() + "@" + row.getVersion(), row);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (ConstructionAreaEntity entity : entities) {
            Map<String, Object> data = summary(entity);
            ConstructionAreaVersionEntity row = currentRows.get(entity.getId() + "@" + entity.getCurrentVersion());
            data.put("area_sqm", row == null ? 0 : row.getAreaSqm());
            data.put("boundary", pointsOf(row));
            result.add(data);
        }
        return result;
    }

    public Map<String, Object> detail(String workspace, String id) {
        ConstructionAreaEntity entity = require(workspace, id);
        ConstructionAreaVersionEntity version = currentVersion(entity);
        Map<String, Object> data = summary(entity);
        data.put("boundary", pointsOf(version));
        data.put("area_sqm", version == null ? 0 : version.getAreaSqm());
        return data;
    }

    @Transactional
    public Map<String, Object> create(String workspace, Map<String, Object> body) {
        requireWorkspace(workspace);
        ConstructionAreaEntity entity = new ConstructionAreaEntity();
        applyFields(entity, body, true);
        List<Point> points = ConstructionAreaGeometry.validated(parseBoundary(body));
        Date now = new Date();
        entity.setId(UUID.randomUUID().toString());
        entity.setWorkspaceId(workspace);
        entity.setCurrentVersion(1);
        entity.setArchived(false);
        entity.setGmtCreate(now);
        entity.setGmtModified(now);
        areas.insert(entity);
        insertVersion(entity.getId(), 1, points);
        return detail(workspace, entity.getId());
    }

    @Transactional
    public Map<String, Object> update(String workspace, String id, Map<String, Object> body) {
        ConstructionAreaEntity entity = require(workspace, id);
        Object expected = body.get("expected_version");
        if (!(expected instanceof Number) || ((Number) expected).intValue() != entity.getCurrentVersion()) {
            throw new ConstructionAreaGeometry.RuleException(409,
                    "区域已被其他端更新（当前 v" + entity.getCurrentVersion() + "），请刷新后重试");
        }
        applyFields(entity, body, false);
        List<Point> points = ConstructionAreaGeometry.validated(parseBoundary(body));
        int nextVersion = entity.getCurrentVersion() + 1;
        Date now = new Date();
        entity.setCurrentVersion(nextVersion);
        entity.setGmtModified(now);
        // Atomic compare-and-set: a concurrent update must fail here, not double-succeed.
        if (areas.update(entity, new LambdaUpdateWrapper<ConstructionAreaEntity>()
                .eq(ConstructionAreaEntity::getId, entity.getId())
                .eq(ConstructionAreaEntity::getCurrentVersion, entity.getCurrentVersion() - 1)) != 1) {
            throw new ConstructionAreaGeometry.RuleException(409,
                    "区域正在被其他端更新，请刷新后重试");
        }
        insertVersion(entity.getId(), nextVersion, points);
        return detail(workspace, entity.getId());
    }

    public List<Map<String, Object>> history(String workspace, String id) {
        ConstructionAreaEntity entity = require(workspace, id);
        List<Map<String, Object>> result = new ArrayList<>();
        LambdaQueryWrapper<ConstructionAreaVersionEntity> wrapper = new LambdaQueryWrapper<ConstructionAreaVersionEntity>()
                .eq(ConstructionAreaVersionEntity::getAreaId, entity.getId())
                .orderByDesc(ConstructionAreaVersionEntity::getVersion);
        for (ConstructionAreaVersionEntity version : versions.selectList(wrapper)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", version.getId());
            row.put("version", version.getVersion());
            row.put("area_sqm", version.getAreaSqm());
            row.put("gmt_create", version.getGmtCreate());
            result.add(row);
        }
        return result;
    }

    @Transactional
    public void archive(String workspace, String id, boolean archived) {
        ConstructionAreaEntity entity = require(workspace, id);
        entity.setArchived(archived);
        entity.setGmtModified(new Date());
        areas.updateById(entity);
    }

    private void insertVersion(String areaId, int version, List<Point> points) {
        ConstructionAreaVersionEntity row = new ConstructionAreaVersionEntity();
        row.setId(UUID.randomUUID().toString());
        row.setAreaId(areaId);
        row.setVersion(version);
        row.setBoundary(ConstructionAreaGeometry.toJson(points));
        row.setAreaSqm(ConstructionAreaGeometry.areaSquareMeters(points));
        row.setGmtCreate(new Date());
        versions.insert(row);
    }

    private ConstructionAreaVersionEntity currentVersion(ConstructionAreaEntity entity) {
        return versions.selectOne(new LambdaQueryWrapper<ConstructionAreaVersionEntity>()
                .eq(ConstructionAreaVersionEntity::getAreaId, entity.getId())
                .eq(ConstructionAreaVersionEntity::getVersion, entity.getCurrentVersion())
                .last("LIMIT 1"));
    }

    private List<Map<String, Object>> pointsOf(ConstructionAreaVersionEntity version) {
        List<Map<String, Object>> points = new ArrayList<>();
        if (version == null) return points;
        for (Point p : ConstructionAreaGeometry.parse(version.getBoundary())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("lng", p.lng);
            item.put("lat", p.lat);
            points.add(item);
        }
        return points;
    }

    private void applyFields(ConstructionAreaEntity entity, Map<String, Object> body, boolean requireAll) {
        String name = text(body.get("name"));
        if (name.isEmpty()) throw new ConstructionAreaGeometry.RuleException(400, "请填写区域名称");
        if (name.length() > 100) throw new ConstructionAreaGeometry.RuleException(400, "区域名称不能超过100字");
        entity.setName(name);
        String category = text(body.get("category"));
        if (category.isEmpty()) throw new ConstructionAreaGeometry.RuleException(400, "请选择施工类型");
        entity.setCategory(category);
        Object enabled = body.get("enabled");
        entity.setEnabled(enabled == null ? Boolean.TRUE : Boolean.TRUE.equals(enabled));
        String notes = text(body.get("notes"));
        if (notes.length() > 500) throw new ConstructionAreaGeometry.RuleException(400, "备注不能超过500字");
        entity.setNotes(notes.isEmpty() ? null : notes);
    }

    @SuppressWarnings("unchecked")
    private List<Point> parseBoundary(Map<String, Object> body) {
        Object raw = body.get("boundary");
        if (!(raw instanceof List)) throw new ConstructionAreaGeometry.RuleException(400, "请绘制区域边界");
        List<Point> points = new ArrayList<>();
        for (Object item : (List<Object>) raw) {
            if (!(item instanceof Map)) throw new ConstructionAreaGeometry.RuleException(400, "区域边界数据无效");
            Map<String, Object> point = (Map<String, Object>) item;
            if (!(point.get("lng") instanceof Number) || !(point.get("lat") instanceof Number)) {
                throw new ConstructionAreaGeometry.RuleException(400, "区域边界数据无效");
            }
            points.add(new Point(((Number) point.get("lng")).doubleValue(), ((Number) point.get("lat")).doubleValue()));
        }
        return points;
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }

    private Map<String, Object> summary(ConstructionAreaEntity entity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", entity.getId());
        data.put("name", entity.getName());
        data.put("category", entity.getCategory());
        data.put("enabled", entity.getEnabled());
        data.put("notes", entity.getNotes());
        data.put("version", entity.getCurrentVersion());
        data.put("archived", entity.getArchived());
        data.put("gmt_create", entity.getGmtCreate());
        data.put("gmt_modified", entity.getGmtModified());
        return data;
    }
}
