package com.dji.sample.df.solar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.solar.dao.OrthophotoEntityMapper;
import com.dji.sample.df.solar.dao.SolarPanelComponentMapper;
import com.dji.sample.df.solar.dao.SolarPanelMapper;
import com.dji.sample.df.solar.model.entity.OrthophotoEntity;
import com.dji.sample.df.solar.model.entity.SolarPanel;
import com.dji.sample.df.solar.model.entity.SolarPanelComponent;
import com.dji.sample.df.solar.service.SolarPanelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Service
public class SolarPanelServiceImpl extends ServiceImpl<SolarPanelMapper, SolarPanel> implements SolarPanelService {

    @Resource
    private SolarPanelMapper solarPanelMapper;
    @Resource
    private SolarPanelComponentMapper solarPanelComponentMapper;
    @Resource
    private OrthophotoEntityMapper orthophotoEntityMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析接口返回的JSON字符串并批量插入数据库
     * @param jsonResponse 接口返回的JSON字符串
     * @return true 插入成功（至少有一条数据且全部插入成功），false 插入失败
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean parseAndSave(String jsonResponse,String orthophotoId) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            int code = root.get("code").asInt();
            if (code != 200) {
                String message = root.has("message") ? root.get("message").asText() : "未知错误";
                log.error("接口返回错误");
                return false;
            }

            JsonNode arrays = root.get("data").get("arrays");
            String solarAreaName = root.get("data").get("solar_area_name").asText();
            if (arrays == null || !arrays.isArray() || arrays.size() == 0) {
                log.warn("返回的 arrays 为空，无数据需要存储");
                return false;
            }

            for (JsonNode array : arrays) {
                // ========== 1. 处理阵列（光伏板）==========
                String arrayLabel = array.get("array_label").asText();
                String fullPanelName = solarAreaName + "_" + arrayLabel;

                // 构建面板实体并填充数据
                SolarPanel sp = new SolarPanel();
                sp.setSolarPanelName(fullPanelName);
                sp.setOrthophotoId(orthophotoId);
                // 设置阵列的几何坐标（四个角）
                JsonNode cornersGeo = array.get("corners_geo");
                if (cornersGeo != null && cornersGeo.size() >= 4) {
                    sp.setCorner1Lng(cornersGeo.get(0).get("lon").asDouble());
                    sp.setCorner1Lat(cornersGeo.get(0).get("lat").asDouble());
                    sp.setCorner2Lng(cornersGeo.get(1).get("lon").asDouble());
                    sp.setCorner2Lat(cornersGeo.get(1).get("lat").asDouble());
                    sp.setCorner3Lng(cornersGeo.get(2).get("lon").asDouble());
                    sp.setCorner3Lat(cornersGeo.get(2).get("lat").asDouble());
                    sp.setCorner4Lng(cornersGeo.get(3).get("lon").asDouble());
                    sp.setCorner4Lat(cornersGeo.get(3).get("lat").asDouble());
                }

                JsonNode cornersPixels = array.get("corners_pixels");
                if (cornersPixels != null && cornersPixels.size() >= 4) {
                    sp.setCorner1Col(cornersPixels.get(0).get("col").asInt());
                    sp.setCorner1Row(cornersPixels.get(0).get("row").asInt());
                    sp.setCorner2Col(cornersPixels.get(1).get("col").asInt());
                    sp.setCorner2Row(cornersPixels.get(1).get("row").asInt());
                    sp.setCorner3Col(cornersPixels.get(2).get("col").asInt());
                    sp.setCorner3Row(cornersPixels.get(2).get("row").asInt());
                    sp.setCorner4Col(cornersPixels.get(3).get("col").asInt());
                    sp.setCorner4Row(cornersPixels.get(3).get("row").asInt());
                }

                // 查询面板是否已存在（根据完整名称 + orthophotoId）
                LambdaQueryWrapper<SolarPanel> panelWrapper = new LambdaQueryWrapper<>();
                panelWrapper.eq(SolarPanel::getSolarPanelName, fullPanelName)
                        .eq(SolarPanel::getOrthophotoId, orthophotoId);
                SolarPanel existingPanel = solarPanelMapper.selectOne(panelWrapper);

                String realPanelId;
                if (existingPanel != null) {
                    // 更新已有面板
                    sp.setId(existingPanel.getId());
                    solarPanelMapper.updateById(sp);
                    realPanelId = existingPanel.getId();
                } else {
                    // 插入新面板
                    String newPanelId = UUID.randomUUID().toString();
                    sp.setId(newPanelId);
                    solarPanelMapper.insert(sp);
                    realPanelId = newPanelId;
                }

                // ========== 2. 处理光伏组件（components）==========
                JsonNode components = array.get("components");
                if (components != null && components.isArray()) {
                    for (JsonNode component : components) {
                        String compLabel = component.get("label").asText();
                        // row 和 col 如果实体类有对应字段，请取消注释并设置
                        // int row = component.get("row").asInt();
                        // int col = component.get("col").asInt();

                        SolarPanelComponent spc = new SolarPanelComponent();
                        spc.setSolarPanelComponentName(fullPanelName+"_"+compLabel);
                        spc.setOrthophotoId(orthophotoId);
                        spc.setSolarPanelId(realPanelId);   // 使用真实的面板ID
                        // spc.setRow(row);   // 若实体类有字段，设置
                        // spc.setCol(col);

                        JsonNode compGeo = component.get("corners_geo");
                        if (compGeo != null && compGeo.size() >= 4) {
                            spc.setCorner1Lng(compGeo.get(0).get("lon").asDouble());
                            spc.setCorner1Lat(compGeo.get(0).get("lat").asDouble());
                            spc.setCorner2Lng(compGeo.get(1).get("lon").asDouble());
                            spc.setCorner2Lat(compGeo.get(1).get("lat").asDouble());
                            spc.setCorner3Lng(compGeo.get(2).get("lon").asDouble());
                            spc.setCorner3Lat(compGeo.get(2).get("lat").asDouble());
                            spc.setCorner4Lng(compGeo.get(3).get("lon").asDouble());
                            spc.setCorner4Lat(compGeo.get(3).get("lat").asDouble());
                        }

                        JsonNode compPixels = component.get("corners_pixels");
                        if (compPixels != null && compPixels.size() >= 4) {
                            spc.setCorner1Col(compPixels.get(0).get("col").asInt());
                            spc.setCorner1Row(compPixels.get(0).get("row").asInt());
                            spc.setCorner2Col(compPixels.get(1).get("col").asInt());
                            spc.setCorner2Row(compPixels.get(1).get("row").asInt());
                            spc.setCorner3Col(compPixels.get(2).get("col").asInt());
                            spc.setCorner3Row(compPixels.get(2).get("row").asInt());
                            spc.setCorner4Col(compPixels.get(3).get("col").asInt());
                            spc.setCorner4Row(compPixels.get(3).get("row").asInt());
                        }

                        // 查询组件是否已存在（根据 真实面板ID + orthophotoId + 组件名称）
                        LambdaQueryWrapper<SolarPanelComponent> compWrapper = new LambdaQueryWrapper<>();
                        compWrapper.eq(SolarPanelComponent::getSolarPanelId, realPanelId)
                                .eq(SolarPanelComponent::getOrthophotoId, orthophotoId)
                                .eq(SolarPanelComponent::getSolarPanelComponentName, fullPanelName+"_"+compLabel);
                        SolarPanelComponent existingComp = solarPanelComponentMapper.selectOne(compWrapper);

                        if (existingComp != null) {
                            // 更新已有组件
                            spc.setId(existingComp.getId());
                            solarPanelComponentMapper.updateById(spc);
                        } else {
                            // 插入新组件
                            spc.setId(UUID.randomUUID().toString());
                            solarPanelComponentMapper.insert(spc);
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log.error("存储光伏数据失败", e);
            throw new RuntimeException("存储光伏数据失败", e); // 触发事务回滚
        }
    }

    @Override
    public boolean updateSolarPanelById(SolarPanel solarPanel) {
        int i = solarPanelMapper.updateById(solarPanel);
        return i > 0;
    }

    @Override
    public boolean removeSolarPanelById(String id) {
        int i = solarPanelMapper.deleteById(id);
        return i > 0;
    }

    @Override
    public SolarPanel getSolarPanelById(String id) {
        return solarPanelMapper.selectById(id);
    }

    @Override
    public List<SolarPanel> selectList(Map<String, Object> params) {
        // 使用原风格的分页工具类 PageUtil（需存在）
//        PageUtil.setPageArgs(params);
        List<SolarPanel> list = solarPanelMapper.selectListByMap(params);
        for (SolarPanel solarPanel : list) {
            OrthophotoEntity orthophotoEntity = orthophotoEntityMapper.selectOne(new LambdaQueryWrapper<OrthophotoEntity>()
                    .eq(OrthophotoEntity::getId, solarPanel.getOrthophotoId()));
            if (orthophotoEntity != null) {
                solarPanel.setOrthophotoName(orthophotoEntity.getName());
            }
        }
//        int count = solarPanelMapper.selectListCount(params);
//
//        Map<String, Object> result = new HashMap<>();
//        Map<String, Object> pagination = new HashMap<>();
//        pagination.put("page", Integer.parseInt(params.get("page").toString()));
//        pagination.put("pageSize", Integer.parseInt(params.get("pageSize").toString()));
//        pagination.put("total", count);
//        result.put("list", list);
//        result.put("pagination", pagination);
        return list;
    }
}
