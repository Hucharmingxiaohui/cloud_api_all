package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.electricInspectionDf.dao.PubBayDfMapper;
import com.dji.sample.df.electricInspectionDf.dao.PubDeviceDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubBayDfEntity;
import com.dji.sample.df.electricInspectionDf.model.PubDeviceDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubBayDfService;
import com.dji.sample.df.electricInspectionDf.service.PubDeviceDfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PubBayDfServiceImpl implements PubBayDfService {
    @Autowired
    private PubBayDfMapper pubBayDfMapper;
    @Autowired
    private PubDeviceDfMapper pubDeviceDfMapper;
    @Autowired
    private PubDeviceDfService pubDeviceDfService;

    //新增间隔
    @Override
    public boolean addBay(PubBayDfEntity pubBayDfEntity) {
        //校验间隔id是否重复
        PubBayDfEntity entity = pubBayDfMapper.selectOne(new LambdaQueryWrapper<PubBayDfEntity>()
                .eq(PubBayDfEntity::getBayId, pubBayDfEntity.getBayId()));
        if (entity == null) {
            pubBayDfMapper.insert(pubBayDfEntity);
            return true;
        }
        return false;
    }

    //根据场站id查询间隔
    @Override
    public List<PubBayDfEntity> getBayListByAreaId(String area_id) {
        List<PubBayDfEntity> pubBayDfEntityList = pubBayDfMapper.selectList(new LambdaQueryWrapper<PubBayDfEntity>().
                eq(PubBayDfEntity::getAreaId, area_id));
        return pubBayDfEntityList;
    }

    //更新间隔
    @Override
    public boolean updateBayById(PubBayDfEntity pubBayDfEntity) {
        int flag = pubBayDfMapper.updateById(pubBayDfEntity);
        return flag > 0;
    }

    //删除间隔
    @Override
    public boolean deleteBayById(int id) {
        //1.根据id查询间隔信息
        PubBayDfEntity pubBayDfEntity=pubBayDfMapper.selectById(id);
        String bay_id = pubBayDfEntity.getBayId();
        //查询关联的设备
        List<PubDeviceDfEntity> pubDeviceDfEntities= pubDeviceDfMapper.selectList(new LambdaQueryWrapper<PubDeviceDfEntity>()
                .eq(PubDeviceDfEntity::getBayId,bay_id));
        //循环删除设备
        for(int i=0;i<pubDeviceDfEntities.size();i++){
            PubDeviceDfEntity pubDeviceDfEntity=pubDeviceDfEntities.get(i);
            pubDeviceDfService.deleteDeviceById(pubDeviceDfEntity.getId());
        }
        //删除间隔
        int flag = pubBayDfMapper.deleteById(id);
        return flag > 0;
    }
}
