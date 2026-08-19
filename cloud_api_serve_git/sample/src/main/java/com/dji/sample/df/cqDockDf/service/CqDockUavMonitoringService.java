package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.cqDockDf.dao.CqDockUavMonitoringMapper;
import com.dji.sample.df.cqDockDf.model.entity.CqDockUavMonitoringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

@Service
public class CqDockUavMonitoringService {

    @Autowired
    private CqDockUavMonitoringMapper mapper;

    public void saveDroneOsd(String topic, String payload) {
        JSONObject json = parse(payload);
        CqDockUavMonitoringEntity entity = loadOrCreate(topic, json.getString("dockId"), json.getString("droneId"));
        CqDockMonitoringPayloadConverter.applyDroneOsd(entity, topic, json, payload);
        persist(entity);
    }

    public void saveDockOsd(String topic, String payload) {
        JSONObject json = parse(payload);
        CqDockUavMonitoringEntity entity = loadOrCreate(topic, json.getString("dockId"), json.getString("droneId"));
        CqDockMonitoringPayloadConverter.applyDockOsd(entity, topic, json, payload);
        persist(entity);
    }

    public CqDockUavMonitoringEntity findLatestByDockSn(String dockSn) {
        if (!StringUtils.hasText(dockSn)) {
            return null;
        }
        return mapper.selectOne(new LambdaQueryWrapper<CqDockUavMonitoringEntity>()
                .eq(CqDockUavMonitoringEntity::getDockSn, dockSn)
                .orderByDesc(CqDockUavMonitoringEntity::getUpdateTime)
                .last("limit 1"));
    }

    public CqDockUavMonitoringEntity findLatestByDroneSn(String droneSn) {
        if (!StringUtils.hasText(droneSn)) {
            return null;
        }
        return mapper.selectOne(new LambdaQueryWrapper<CqDockUavMonitoringEntity>()
                .eq(CqDockUavMonitoringEntity::getDroneSn, droneSn)
                .orderByDesc(CqDockUavMonitoringEntity::getUpdateTime)
                .last("limit 1"));
    }

    public CqDockUavMonitoringEntity findLatestAny() {
        return mapper.selectOne(new LambdaQueryWrapper<CqDockUavMonitoringEntity>()
                .orderByDesc(CqDockUavMonitoringEntity::getUpdateTime)
                .last("limit 1"));
    }

    private CqDockUavMonitoringEntity loadOrCreate(String topic, String dockSn, String droneSn) {
        CqDockUavMonitoringEntity entity = null;
        String topicDockSn = topicDockSn(topic);
        String key = StringUtils.hasText(dockSn) ? dockSn : (StringUtils.hasText(droneSn) ? droneSn : topicDockSn);
        if (StringUtils.hasText(key)) {
            entity = mapper.selectOne(new LambdaQueryWrapper<CqDockUavMonitoringEntity>()
                    .and(w -> w.eq(CqDockUavMonitoringEntity::getDockSn, key)
                            .or()
                            .eq(CqDockUavMonitoringEntity::getDroneSn, key)
                            .or()
                            .eq(CqDockUavMonitoringEntity::getTopicDockSn, key))
                    .orderByDesc(CqDockUavMonitoringEntity::getUpdateTime)
                    .last("limit 1"));
        }
        if (entity == null) {
            entity = new CqDockUavMonitoringEntity();
            entity.setCreateTime(new Date());
        }
        if (!StringUtils.hasText(entity.getDockSn())) {
            entity.setDockSn(dockSn);
        }
        if (!StringUtils.hasText(entity.getDroneSn())) {
            entity.setDroneSn(droneSn);
        }
        if (!StringUtils.hasText(entity.getTopicDockSn()) && StringUtils.hasText(topicDockSn)) {
            entity.setTopicDockSn(topicDockSn);
        }
        if (!StringUtils.hasText(entity.getUnitCode()) && StringUtils.hasText(topic)) {
            String[] parts = topic.split("/");
            if (parts.length > 1) {
                entity.setUnitCode(parts[1]);
            }
        }
        return entity;
    }

    private String topicDockSn(String topic) {
        if (!StringUtils.hasText(topic)) {
            return null;
        }
        String[] parts = topic.split("/");
        return parts.length > 2 ? parts[2] : null;
    }

    private void persist(CqDockUavMonitoringEntity entity) {
        Date now = new Date();
        entity.setUpdateTime(now);
        if (entity.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
    }

    private JSONObject parse(String payload) {
        try {
            return StringUtils.hasText(payload) ? JSONObject.parseObject(payload) : new JSONObject();
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
