package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.electricInspectionDf.dao.PubUavDeviceDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubUavDeviceDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubUavDeviceDfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PubUavDeviceDfServiceImpl implements PubUavDeviceDfService {
    @Autowired
    private PubUavDeviceDfMapper pubUavDeviceDfMapper;
    //添加场站下的无人机设备
    @Override
    public boolean addDeviceToSub(PubUavDeviceDfEntity pubUavDeviceDfEntity) {
        PubUavDeviceDfEntity entity = pubUavDeviceDfMapper.selectOne(new LambdaQueryWrapper<PubUavDeviceDfEntity>()
                .eq(PubUavDeviceDfEntity::getDeviceSn,pubUavDeviceDfEntity.getDeviceSn()));
        if(entity==null){
            pubUavDeviceDfMapper.insert(pubUavDeviceDfEntity);
            return true;
        }
        return false;
    }
    //通过场站id查询无人机设备
    @Override
    public List<PubUavDeviceDfEntity> getUavDeviceBySubCode(String sub_code) {
        List<PubUavDeviceDfEntity> pubUavDeviceDfEntityList = pubUavDeviceDfMapper.selectList(new LambdaQueryWrapper<PubUavDeviceDfEntity>()
                .eq(PubUavDeviceDfEntity::getSubCode,sub_code));
        return pubUavDeviceDfEntityList;
    }

    @Override
    public boolean updateUavDeviceById(PubUavDeviceDfEntity pubUavDeviceDfEntity) {
        int falg= pubUavDeviceDfMapper.updateById(pubUavDeviceDfEntity);
        if(falg>0){
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteUavDeviceById(Integer id) {

        return false;
    }
}
