package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.electricInspectionDf.dao.PubAreaDfMapper;
import com.dji.sample.df.electricInspectionDf.dao.PubBayDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubAreaDfEntity;
import com.dji.sample.df.electricInspectionDf.model.PubBayDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubAreaDfService;
import com.dji.sample.df.electricInspectionDf.service.PubBayDfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PubAreaDfServiceImpl implements PubAreaDfService {
    @Autowired
    private PubAreaDfMapper pubAreaDfMapper;
    @Autowired
    private PubBayDfMapper pubBayDfMapper;
    @Autowired
    private PubBayDfService pubBayDfService;
    //新增区域
    @Override
    public boolean addArea(PubAreaDfEntity pubAreaDfEntity) {
        //校验区域id是否重复
        PubAreaDfEntity entity = pubAreaDfMapper.selectOne(new LambdaQueryWrapper<PubAreaDfEntity>()
                .eq(PubAreaDfEntity::getAreaId,pubAreaDfEntity.getAreaId()));
        if(entity==null){
            pubAreaDfMapper.insert(pubAreaDfEntity);
            return true;
        }
        return false;
    }
    //根据场站id查询区域
    @Override
    public List<PubAreaDfEntity> getAreaListBySubCode(String sub_code) {
        List<PubAreaDfEntity> pubAreaDfEntityList=pubAreaDfMapper.selectList(new LambdaQueryWrapper<PubAreaDfEntity>().
                eq(PubAreaDfEntity::getSubCode,sub_code));
        return pubAreaDfEntityList;
    }
    //更新区域
    @Override
    public boolean updateAreaById(PubAreaDfEntity pubAreaDfEntity) {
        int flag = pubAreaDfMapper.updateById(pubAreaDfEntity);
        if(flag>0){
            return true;
        }
        return false;
    }
    //删除区域
    @Override
    public boolean deleteAreaById(int id) {
        //1.查询区域实体类
        PubAreaDfEntity pubAreaDfEntity = new PubAreaDfEntity();
        //获取area_id
        String area_id = pubAreaDfEntity.getAreaId();
        //2.根据area_id查询关联的间隔
        List<PubBayDfEntity> pubBayDfEntityList=pubBayDfMapper.selectList(new LambdaQueryWrapper<PubBayDfEntity>()
                .eq(PubBayDfEntity::getAreaId,area_id));
        //3.循环删除间隔
        for(int i=0;i<pubBayDfEntityList.size();i++){
            PubBayDfEntity pubBayDfEntity=pubBayDfEntityList.get(i);
            pubBayDfService.deleteBayById(pubBayDfEntity.getId());
        }
        //4.删除区域
        int flag = pubAreaDfMapper.deleteById(id);
        if(flag>0){
            return true;
        }
        return false;
    }



}
