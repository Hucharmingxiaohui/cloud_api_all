package com.dji.sample.df.wind.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.wind.dao.WindTurbineMapper;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import com.dji.sample.df.wind.service.WindTurbineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class WindTurbineServiceImpl extends ServiceImpl<WindTurbineMapper, WindTurbine> implements WindTurbineService {

    @Resource
    private WindTurbineMapper windTurbineMapper;

    @Override
    public boolean saveWindTurbine(WindTurbine windTurbine) {
        windTurbine.setId(UUID.randomUUID().toString());
        windTurbine.setApproachYaw(0.0);
        windTurbine.setBladeStopAngle(0.0);
        int insert = windTurbineMapper.insert(windTurbine);
        if (insert > 0) {
            return true;
        }else  {
            return false;
        }
    }

    @Override
    public boolean updateWindTurbineById(WindTurbine windTurbine) {
        int i = windTurbineMapper.updateById(windTurbine);
        if (i > 0) {
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean removeWindTurbineById(String id) {
        int i = windTurbineMapper.deleteById(id);
        if (i > 0) {
            return true;
        }else {
            return false;
        }
    }

    @Override
    public WindTurbine getWindTurbineById(String id) {
        WindTurbine windTurbine = windTurbineMapper.selectById(id);
        return windTurbine;
    }

    @Override
    public Map<String,Object> selectList(Map map) {
        PageUtil.setPageArgs(map);
        List<WindTurbine> windTurbines = windTurbineMapper.selectList(map);
        int count = windTurbineMapper.selectListCount(map);
        Map result = new HashMap();
        Map pagination = new HashMap();
        pagination.put("page",Integer.parseInt(map.get("page").toString()));
        pagination.put("pageSize",Integer.parseInt(map.get("pageSize").toString()));
        pagination.put("total",count);
        result.put("list", windTurbines);
        result.put("pagination", pagination);
        return result;
    }

    @Override
    public WindTurbine selectByName(String name) {
        WindTurbine windTurbine = windTurbineMapper.selectOne(new LambdaQueryWrapper<WindTurbine>().eq(WindTurbine::getTurbineName, name));
        return windTurbine;
    }


}
