package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.electricInspectionDf.dao.PubComponentDfMapper;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylinePointDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubComponentDfEntity;
import com.dji.sample.df.electricInspectionDf.model.PubWaylinePointDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubComponentDfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PubComponentDfServiceImpl implements PubComponentDfService {
    @Autowired
    private PubComponentDfMapper pubComponentDfMapper;
    @Autowired
    private PubWaylinePointDfMapper pubWaylinePointDfMapper;

    // 新增部件信息
    @Override
    public boolean addComponent(PubComponentDfEntity pubComponentDfEntity) {
        // 校验部件ID是否重复
        PubComponentDfEntity entity = pubComponentDfMapper.selectOne(new LambdaQueryWrapper<PubComponentDfEntity>()
                .eq(PubComponentDfEntity::getComponentId, pubComponentDfEntity.getComponentId()));
        if (entity == null) {
            pubComponentDfMapper.insert(pubComponentDfEntity);
            return true;
        }
        return false;
    }

    // 根据设备ID查询部件列表
    @Override
    public List<PubComponentDfEntity> getComponentListByDeviceId(String device_id) {
        List<PubComponentDfEntity> componentList = pubComponentDfMapper.selectList(new LambdaQueryWrapper<PubComponentDfEntity>()
                .eq(PubComponentDfEntity::getDeviceId, device_id));
        return componentList;
    }

    // 更新部件信息
    @Override
    public boolean updateComponentById(PubComponentDfEntity pubComponentDfEntity) {
        int flag = pubComponentDfMapper.updateById(pubComponentDfEntity);
        return flag > 0;
    }

    // 删除部件
    @Override
    public boolean deleteComponentById(int id) {
        //查询对应部件
        PubComponentDfEntity pubComponentDfEntity=pubComponentDfMapper.selectById(id);
        //获取对应部件id
        String component_id = pubComponentDfEntity.getComponentId();
        int flag = pubComponentDfMapper.deleteById(id);
        //删除部件时同时删除和部件关联的点位
        if(component_id!=null){
            pubWaylinePointDfMapper.delete(new LambdaQueryWrapper<PubWaylinePointDfEntity>()
                    .eq(PubWaylinePointDfEntity::getComponentId,component_id));
        }
        return flag > 0;
    }
}
