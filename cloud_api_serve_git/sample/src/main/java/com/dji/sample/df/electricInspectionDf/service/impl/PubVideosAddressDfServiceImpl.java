package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.electricInspectionDf.dao.PubVideosAddressDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubVideosAddressDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubVideosAddressDfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PubVideosAddressDfServiceImpl implements PubVideosAddressDfService {
    @Autowired
    private PubVideosAddressDfMapper pubVideosAddressDfMapper;
    @Override
    public boolean addVideosAddress(PubVideosAddressDfEntity pubVideosAddressDfEntity) {
        PubVideosAddressDfEntity entity = pubVideosAddressDfMapper.selectOne(new LambdaQueryWrapper<PubVideosAddressDfEntity>()
                .eq(PubVideosAddressDfEntity::getDeviceSn,pubVideosAddressDfEntity.getDeviceSn()));
        if(entity!=null){
            //查到重复就不入库
            return false;
        }else{
            pubVideosAddressDfMapper.insert(pubVideosAddressDfEntity);
            return true;
        }
    }
    //查询所有的视频地址列表
    @Override
    public List<PubVideosAddressDfEntity> getVideosList() {
        List<PubVideosAddressDfEntity> pubVideosAddressDfEntityList = pubVideosAddressDfMapper.selectList(null);
        return pubVideosAddressDfEntityList;
    }

    //按场站编码查询视频地址
    @Override
    public List<PubVideosAddressDfEntity> getVideosBySubCode(String sub_code) {
        List<PubVideosAddressDfEntity> pubVideosAddressDfEntityList = pubVideosAddressDfMapper.selectList(new LambdaQueryWrapper<PubVideosAddressDfEntity>()
                .eq(PubVideosAddressDfEntity::getSubCode,sub_code));
        return pubVideosAddressDfEntityList;
    }
    //更新视频地址
    @Override
    public boolean updateVideosAddress(PubVideosAddressDfEntity pubVideosAddressDfEntity) {
        System.out.println(pubVideosAddressDfEntity);
        int flag = pubVideosAddressDfMapper.updateById(pubVideosAddressDfEntity);
//        System.out.println(flag);
        if(flag>0){
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteVideosById(int id) {
        int flag = pubVideosAddressDfMapper.deleteById(id);
        if(flag>0){
            return true;
        }
//        System.out.println(flag);
        return false;
    }

    @Override
    public PubVideosAddressDfEntity getVideoAddressByDeviceSn(String device_sn) {
        PubVideosAddressDfEntity entity = pubVideosAddressDfMapper.selectOne(new LambdaQueryWrapper<PubVideosAddressDfEntity>()
                .eq(PubVideosAddressDfEntity::getDeviceSn,device_sn));
        return entity;
    }
}
