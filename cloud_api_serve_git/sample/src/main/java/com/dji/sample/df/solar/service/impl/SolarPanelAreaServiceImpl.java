package com.dji.sample.df.solar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.solar.dao.SolarPanelAreaMapper;
import com.dji.sample.df.solar.model.entity.SolarPanelArea;
import com.dji.sample.df.solar.service.SolarPanelAreaService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SolarPanelAreaServiceImpl extends ServiceImpl<SolarPanelAreaMapper, SolarPanelArea> implements SolarPanelAreaService {
    @Resource
    private SolarPanelAreaMapper solarPanelAreaMapper;

    @Override
    public boolean saveSolarPanelArea(SolarPanelArea solarPanelArea) {
        // 主键自增，无需设置 ID；如有默认值字段可在此初始化
        // solarPanel.setXXX(默认值);
        int insert = solarPanelAreaMapper.insert(solarPanelArea);
        return insert > 0;
    }

    @Override
    public boolean updateSolarPanelAreaById(SolarPanelArea solarPanelArea) {
        int i = solarPanelAreaMapper.updateById(solarPanelArea);
        return i > 0;
    }

    @Override
    public boolean removeSolarPanelAreaById(Long id) {
        int i = solarPanelAreaMapper.deleteById(id);
        return i > 0;
    }

    @Override
    public SolarPanelArea getSolarPanelAreaById(Long id) {
        return solarPanelAreaMapper.selectById(id);
    }

    @Override
    public Map<String, Object> selectList(Map<String, Object> params) {
        // 使用原风格的分页工具类 PageUtil（需存在）
        PageUtil.setPageArgs(params);
        List<SolarPanelArea> list = solarPanelAreaMapper.selectList(params);
        int count = solarPanelAreaMapper.selectListCount(params);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", Integer.parseInt(params.get("page").toString()));
        pagination.put("pageSize", Integer.parseInt(params.get("pageSize").toString()));
        pagination.put("total", count);
        result.put("list", list);
        result.put("pagination", pagination);
        return result;
    }

}
