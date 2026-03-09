package com.dji.sample.df.solar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.solar.dao.SolarPanelMapper;
import com.dji.sample.df.solar.model.entity.SolarPanel;
import com.dji.sample.df.solar.service.SolarPanelService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SolarPanelServiceImpl extends ServiceImpl<SolarPanelMapper, SolarPanel> implements SolarPanelService {
    @Resource
    private SolarPanelMapper solarPanelMapper;

    @Override
    public boolean saveSolarPanel(SolarPanel solarPanel) {
        // 主键自增，无需设置 ID；如有默认值字段可在此初始化
        // solarPanel.setXXX(默认值);
        int insert = solarPanelMapper.insert(solarPanel);
        return insert > 0;
    }

    @Override
    public boolean updateSolarPanelById(SolarPanel solarPanel) {
        int i = solarPanelMapper.updateById(solarPanel);
        return i > 0;
    }

    @Override
    public boolean removeSolarPanelById(Long id) {
        int i = solarPanelMapper.deleteById(id);
        return i > 0;
    }

    @Override
    public SolarPanel getSolarPanelById(Long id) {
        return solarPanelMapper.selectById(id);
    }

    @Override
    public Map<String, Object> selectList(Map<String, Object> params) {
        // 使用原风格的分页工具类 PageUtil（需存在）
        PageUtil.setPageArgs(params);
        List<SolarPanel> list = solarPanelMapper.selectList(params);
        int count = solarPanelMapper.selectListCount(params);

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
