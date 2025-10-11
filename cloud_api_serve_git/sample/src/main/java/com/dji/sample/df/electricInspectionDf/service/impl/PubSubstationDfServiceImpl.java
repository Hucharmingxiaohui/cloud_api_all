package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.electricInspectionDf.dao.PubAreaDfMapper;
import com.dji.sample.df.electricInspectionDf.dao.PubSubstationDfMapper;
import com.dji.sample.df.electricInspectionDf.dao.PubUavDeviceDfMapper;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineFileDfMapper;
import com.dji.sample.df.electricInspectionDf.model.*;
import com.dji.sample.df.electricInspectionDf.service.PubAreaDfService;
import com.dji.sample.df.electricInspectionDf.service.PubSubstationDfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PubSubstationDfServiceImpl implements PubSubstationDfService {
    @Autowired
    private PubSubstationDfMapper pubSubstationDfMapper;
    @Autowired
    private PubAreaDfMapper pubAreaDfMapper;
    @Autowired
    private PubAreaDfService pubAreaDfService;
    @Autowired
    private PubUavDeviceDfMapper pubUavDeviceDfMapper;
    @Autowired
    private PubWaylineFileDfMapper pubWaylineFileDfMapper;
    //新增一个场站
    @Override
    public Integer addPubStation(PubSubstationDfEntity pubSubstationDfEntity) {
        //场站名称不能重复
        PubSubstationDfEntity entity = pubSubstationDfMapper.selectOne(new LambdaQueryWrapper<PubSubstationDfEntity>().eq(PubSubstationDfEntity::getSubName,pubSubstationDfEntity.getSubName()));
        if(entity!=null){
            return -1;
        }else {
            pubSubstationDfMapper.insert(pubSubstationDfEntity);
            return 0;
        }
    }
    //查询所有的场站
    @Override
    public List<PubSubstationDfEntity> getPubStationList() {
        return pubSubstationDfMapper.selectList(null);
    }
    //按运维班、工作id查询场站
    @Override
    public List<PubSubstationDfEntity> getPubStationByWorkspaceId(String workspace_id) {
        List<PubSubstationDfEntity> pubSubstationDfEntities=pubSubstationDfMapper.selectList(new LambdaQueryWrapper<PubSubstationDfEntity>()
                .eq(PubSubstationDfEntity::getWorkspaceId,workspace_id));
        return pubSubstationDfEntities;
    }
    //删除场站
    @Override
    public boolean deleteSubStationById(Integer id) {
        //1.查询场站信息
        PubSubstationDfEntity pubSubstationDfEntity= pubSubstationDfMapper.selectById(id);
        //获取场站id
        String sub_code = pubSubstationDfEntity.getSubCode();;
        //2.根据场站id查询区域
        List<PubAreaDfEntity> pubAreaDfEntities=pubAreaDfMapper.selectList(new LambdaQueryWrapper<PubAreaDfEntity>()
                .eq(PubAreaDfEntity::getSubCode,sub_code));
        //3.循环删除区域
        for(int i =0;i<pubAreaDfEntities.size();i++){
            PubAreaDfEntity pubAreaDfEntity=pubAreaDfEntities.get(i);
            pubAreaDfService.deleteAreaById(pubAreaDfEntity.getId());
        }
        //4.删除无人机设备
        List<PubUavDeviceDfEntity> pubUavDeviceDfEntityList=pubUavDeviceDfMapper.selectList(new LambdaQueryWrapper<PubUavDeviceDfEntity>()
                .eq(PubUavDeviceDfEntity::getSubCode,sub_code));
        if(pubUavDeviceDfEntityList.size()>0){
            pubUavDeviceDfMapper.delete(new LambdaQueryWrapper<PubUavDeviceDfEntity>()
                    .eq(PubUavDeviceDfEntity::getSubCode,sub_code));
        }
        //5.删除航线
        List<PubWaylineFileDfEntity>pubWaylineFileDfEntities= pubWaylineFileDfMapper.selectList(new LambdaQueryWrapper<PubWaylineFileDfEntity>()
                .eq(PubWaylineFileDfEntity::getSubCode,sub_code));
        if(pubWaylineFileDfEntities.size()>0){
            pubWaylineFileDfMapper.delete(new LambdaQueryWrapper<PubWaylineFileDfEntity>()
                    .eq(PubWaylineFileDfEntity::getSubCode,sub_code));
        }
        //4.删除场站
        int flag=pubSubstationDfMapper.deleteById(id);
        if(flag>0){
            return true;
        }
        return false;
    }
    //根据id更新场站
    @Override
    public boolean updateSubSationById(PubSubstationDfEntity pubSubstationDfEntity) {
        int flag = pubSubstationDfMapper.updateById(pubSubstationDfEntity);
        if(flag>0){
            return true;
        }
        return false;
    }

    @Override
    public Map getSubstationsByParam(Map<String, Object> map) {
        PageUtil.setPageArgs(map);
        Map map1=new HashMap();
        List<PubSubstationDfEntity> entities = pubSubstationDfMapper.getSubstationsByParam(map);
        int pointsByParamCount = pubSubstationDfMapper.getSubstationsByParamCount(map);
        map1.put("count",pointsByParamCount);
        map1.put("data",entities);
        return map1;
    }
}
