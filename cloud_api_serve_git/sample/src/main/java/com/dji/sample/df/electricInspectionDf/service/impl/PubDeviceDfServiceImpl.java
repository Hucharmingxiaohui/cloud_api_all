package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.electricInspectionDf.dao.PubComponentDfMapper;
import com.dji.sample.df.electricInspectionDf.dao.PubDeviceDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubComponentDfEntity;
import com.dji.sample.df.electricInspectionDf.model.PubDeviceDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubComponentDfService;
import com.dji.sample.df.electricInspectionDf.service.PubDeviceDfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PubDeviceDfServiceImpl implements PubDeviceDfService {
    @Autowired
    private PubDeviceDfMapper pubDeviceDfMapper;
    @Autowired
    private PubComponentDfMapper pubComponentDfMapper;
    @Autowired
    private PubComponentDfService pubComponentDfService;

    // 添加设备
    @Override
    public boolean addDevice(PubDeviceDfEntity pubDeviceDfEntity) {
        // 校验设备ID是否重复
        PubDeviceDfEntity entity = pubDeviceDfMapper.selectOne(new LambdaQueryWrapper<PubDeviceDfEntity>()
                .eq(PubDeviceDfEntity::getDeviceId, pubDeviceDfEntity.getDeviceId()));
        if (entity == null) {
            pubDeviceDfMapper.insert(pubDeviceDfEntity);
            return true;
        }
        return false;
    }

    // 根据主设备ID查询设备
    @Override
    public PubDeviceDfEntity getDeviceById(int id) {
        return pubDeviceDfMapper.selectById(id);
    }

    // 根据变电站编码查询设备列表
    @Override
    public List<PubDeviceDfEntity> getDeviceListByBayId(String bay_id) {
        return pubDeviceDfMapper.selectList(new LambdaQueryWrapper<PubDeviceDfEntity>()
                .eq(PubDeviceDfEntity::getBayId, bay_id));
    }

    // 更新设备信息
    @Override
    public boolean updateDeviceById(PubDeviceDfEntity pubDeviceDfEntity) {
        int flag = pubDeviceDfMapper.updateById(pubDeviceDfEntity);
        return flag > 0;
    }

    // 删除设备
    @Override
    public boolean deleteDeviceById(int id) {
        //1.查询设备信息
        PubDeviceDfEntity pubDeviceDfEntity=pubDeviceDfMapper.selectById(id);
        String device_id= pubDeviceDfEntity.getDeviceId();
        //2.在部件表里查询和设备关联的部件
        List<PubComponentDfEntity> pubComponentDfEntities=pubComponentDfMapper.selectList(new LambdaQueryWrapper<PubComponentDfEntity>()
                .eq(PubComponentDfEntity::getDeviceId,device_id));
        //3.循环删除部件
        for(int i=0;i<pubComponentDfEntities.size();i++){
            //部件里删除点位
            PubComponentDfEntity pubComponentDfEntity=pubComponentDfEntities.get(i);
            pubComponentDfService.deleteComponentById(pubComponentDfEntity.getId());
        }
        //删除设备
        int flag = pubDeviceDfMapper.deleteById(id);
        return flag > 0;
    }
}
