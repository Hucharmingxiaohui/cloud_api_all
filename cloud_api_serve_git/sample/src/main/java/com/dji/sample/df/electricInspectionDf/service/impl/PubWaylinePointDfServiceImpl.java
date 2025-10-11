package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.electricInspectionDf.dao.*;
import com.dji.sample.df.electricInspectionDf.model.*;
import com.dji.sample.df.electricInspectionDf.service.*;
import com.dji.sdk.common.Pagination;
import com.dji.sdk.common.PaginationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class PubWaylinePointDfServiceImpl implements PubWaylinePointDfService {
    @Autowired
    private PubWaylinePointDfMapper pubWaylinePointDfMapper;

    @Autowired
    private PubAreaDfMapper pubAreaDfMapper;
    @Autowired
    private PubBayDfService pubBayDfService;
    @Autowired
    private PubDeviceDfService pubDeviceDfService;
    @Autowired
    private PubComponentDfService pubComponentDfService;
    @Autowired
    private PubAreaDfService pubAreaDfService;
    @Autowired
    private PubBayDfMapper pubBayDfMapper;
    @Autowired
    private PubDeviceDfMapper pubDeviceDfMapper;
    @Autowired
    private PubComponentDfMapper pubComponentDfMapper;

    @Override
    public List<PubWaylinePointDfEntity> savePubWaylinePointDfEntity(String sub_code, List<PubWaylinePointDfEntity> pointsList) {
        //存储重复的点位数据
        List<PubWaylinePointDfEntity> repeatList = new ArrayList<>();
        for (int i = 0; i < pointsList.size(); i++) {
            //1.取出每一个数据
            PubWaylinePointDfEntity pubWaylinePointDfEntity=pointsList.get(i);
            //2.设置场站id
            pubWaylinePointDfEntity.setPointCode(sub_code);
            //3.校验区域有没有重复 根据场站id sub_code 区域名称判断
            PubAreaDfEntity repeatPubAreaDfEntity = pubAreaDfMapper.selectOne(new LambdaQueryWrapper<PubAreaDfEntity>()
                    .eq(PubAreaDfEntity::getSubCode,sub_code).
                    eq(PubAreaDfEntity::getAreaName,pubWaylinePointDfEntity.getAreaName()));
            System.out.println(repeatPubAreaDfEntity);
            System.out.println("这是为什么");
            //如果查询区域值不为空
            if(repeatPubAreaDfEntity!=null)
            {
                //直接拿数据库的区域id值赋值
                pubWaylinePointDfEntity.setAreaId(repeatPubAreaDfEntity.getAreaId());
                //4.校验间隔有没有重复
                PubBayDfEntity repeatPubBayDfEntity= pubBayDfMapper.selectOne(new LambdaQueryWrapper<PubBayDfEntity>()
                        .eq(PubBayDfEntity::getAreaId,pubWaylinePointDfEntity.getAreaId()).
                        eq(PubBayDfEntity::getBayName,pubWaylinePointDfEntity.getBayName()));
                //如果间隔值不为空
                if(repeatPubBayDfEntity!=null)
                {
                    //直接拿数据库的间隔id值赋值
                    pubWaylinePointDfEntity.setBayId(repeatPubBayDfEntity.getBayId());
                    //5.校验设备id有没有重复
                    PubDeviceDfEntity repeatDeviceDfEntity=pubDeviceDfMapper.selectOne(new LambdaQueryWrapper<PubDeviceDfEntity>()
                            .eq(PubDeviceDfEntity::getBayId,pubWaylinePointDfEntity.getBayId()).
                            eq(PubDeviceDfEntity::getDeviceName,pubWaylinePointDfEntity.getDeviceName()));
                    //如果设备不为空
                    if(repeatDeviceDfEntity!=null)
                    {
                        //直接拿数据库的设备id值赋值
                        pubWaylinePointDfEntity.setDeviceId(repeatDeviceDfEntity.getDeviceId());
                        //6.校验部件有没有重复
                        PubComponentDfEntity repeatPubComponentDfEntity = pubComponentDfMapper.selectOne(new LambdaQueryWrapper<PubComponentDfEntity>()
                                .eq(PubComponentDfEntity::getDeviceId,pubWaylinePointDfEntity.getDeviceId()).
                                eq(PubComponentDfEntity::getComponentName,pubWaylinePointDfEntity.getComponentName()));
                        if(repeatPubComponentDfEntity!=null)
                        {
                            //直接拿数据库的部件id值赋值
                            pubWaylinePointDfEntity.setComponentId(repeatPubComponentDfEntity.getComponentId());
                            //7.判断点位有没有重复
                            PubWaylinePointDfEntity repeatPubWaylinePointDfEntity=pubWaylinePointDfMapper.selectOne(new LambdaQueryWrapper<PubWaylinePointDfEntity>()
                                    .eq(PubWaylinePointDfEntity::getComponentId,pubWaylinePointDfEntity.getComponentId()).
                                    eq(PubWaylinePointDfEntity::getPointName,pubWaylinePointDfEntity.getPointName()));
                            if(repeatPubWaylinePointDfEntity!=null)
                            {//如果有重复
                                pubWaylinePointDfEntity.setPointCode(repeatPubWaylinePointDfEntity.getPointCode());
                                repeatList.add(pubWaylinePointDfEntity);

                            }else
                            {//如果没有重复，添加
                                pubWaylinePointDfEntity.setPointCode(UUID.randomUUID().toString());
                                pubWaylinePointDfMapper.insert(pubWaylinePointDfEntity);

                            }
                        }else
                        {
                            //component_id
                            pubWaylinePointDfEntity.setComponentId(UUID.randomUUID().toString());
                            //point_code
                            pubWaylinePointDfEntity.setPointCode(UUID.randomUUID().toString());
                            //初始化部件
                            PubComponentDfEntity componentDfEntity=new PubComponentDfEntity();
                            //部件
                            componentDfEntity.setSubCode(pubWaylinePointDfEntity.getSubCode());
                            componentDfEntity.setAreaId(pubWaylinePointDfEntity.getAreaId());
                            componentDfEntity.setBayId(pubWaylinePointDfEntity.getBayId());
                            componentDfEntity.setDeviceId(pubWaylinePointDfEntity.getDeviceId());
                            componentDfEntity.setComponentId(pubWaylinePointDfEntity.getComponentId());
                            componentDfEntity.setComponentName(pubWaylinePointDfEntity.getComponentName());
                            pubComponentDfService.addComponent(componentDfEntity);
                            //点位
                            pubWaylinePointDfMapper.insert(pubWaylinePointDfEntity);
                        }


                    }else
                    {
                        //device_id
                        pubWaylinePointDfEntity.setDeviceId(UUID.randomUUID().toString());
                        //component_id
                        pubWaylinePointDfEntity.setComponentId(UUID.randomUUID().toString());
                        //point_code
                        pubWaylinePointDfEntity.setPointCode(UUID.randomUUID().toString());
                        //初始化设备
                        PubDeviceDfEntity deviceDfEntity= new PubDeviceDfEntity();
                        //初始化部件
                        PubComponentDfEntity componentDfEntity=new PubComponentDfEntity();
                        //设备
                        deviceDfEntity.setSubCode(pubWaylinePointDfEntity.getSubCode());
                        deviceDfEntity.setAreaId(pubWaylinePointDfEntity.getAreaId());
                        deviceDfEntity.setBayId(pubWaylinePointDfEntity.getBayId());
                        deviceDfEntity.setDeviceId(pubWaylinePointDfEntity.getDeviceId());
                        deviceDfEntity.setDeviceName(pubWaylinePointDfEntity.getDeviceName());
                        pubDeviceDfService.addDevice(deviceDfEntity);
                        //部件
                        componentDfEntity.setSubCode(pubWaylinePointDfEntity.getSubCode());
                        componentDfEntity.setAreaId(pubWaylinePointDfEntity.getAreaId());
                        componentDfEntity.setBayId(pubWaylinePointDfEntity.getBayId());
                        componentDfEntity.setDeviceId(pubWaylinePointDfEntity.getDeviceId());
                        componentDfEntity.setComponentId(pubWaylinePointDfEntity.getComponentId());
                        componentDfEntity.setComponentName(pubWaylinePointDfEntity.getComponentName());
                        pubComponentDfService.addComponent(componentDfEntity);
                        //点位
                        pubWaylinePointDfMapper.insert(pubWaylinePointDfEntity);

                    }
                }else
                {

                    //为空则需要填充数据
                    //bay_id
                    pubWaylinePointDfEntity.setBayId(UUID.randomUUID().toString());
                    //device_id
                    pubWaylinePointDfEntity.setDeviceId(UUID.randomUUID().toString());
                    //component_id
                    pubWaylinePointDfEntity.setComponentId(UUID.randomUUID().toString());
                    //point_code
                    pubWaylinePointDfEntity.setPointCode(UUID.randomUUID().toString());

                    //初始化间隔
                    PubBayDfEntity bayDfEntity=new PubBayDfEntity();
                    //初始化设备
                    PubDeviceDfEntity deviceDfEntity= new PubDeviceDfEntity();
                    //初始化部件
                    PubComponentDfEntity componentDfEntity=new PubComponentDfEntity();
                    //间隔
                    bayDfEntity.setSubCode(pubWaylinePointDfEntity.getSubCode());
                    bayDfEntity.setAreaId(pubWaylinePointDfEntity.getAreaId());
                    bayDfEntity.setBayId(pubWaylinePointDfEntity.getBayId());
                    bayDfEntity.setBayName(pubWaylinePointDfEntity.getBayName());
                    pubBayDfService.addBay(bayDfEntity);
                    //设备
                    deviceDfEntity.setSubCode(pubWaylinePointDfEntity.getSubCode());
                    deviceDfEntity.setAreaId(pubWaylinePointDfEntity.getAreaId());
                    deviceDfEntity.setBayId(pubWaylinePointDfEntity.getBayId());
                    deviceDfEntity.setDeviceId(pubWaylinePointDfEntity.getDeviceId());
                    deviceDfEntity.setDeviceName(pubWaylinePointDfEntity.getDeviceName());
                    pubDeviceDfService.addDevice(deviceDfEntity);
                    //部件
                    componentDfEntity.setSubCode(pubWaylinePointDfEntity.getSubCode());
                    componentDfEntity.setAreaId(pubWaylinePointDfEntity.getAreaId());
                    componentDfEntity.setBayId(pubWaylinePointDfEntity.getBayId());
                    componentDfEntity.setDeviceId(pubWaylinePointDfEntity.getDeviceId());
                    componentDfEntity.setComponentId(pubWaylinePointDfEntity.getComponentId());
                    componentDfEntity.setComponentName(pubWaylinePointDfEntity.getComponentName());
                    pubComponentDfService.addComponent(componentDfEntity);
                    //点位
                    pubWaylinePointDfMapper.insert(pubWaylinePointDfEntity);
                }

            }else
            {
                //数据库为空,需要生成
                //初始化区域
                PubAreaDfEntity areaDfEntity=new PubAreaDfEntity();
                //初始化间隔
                PubBayDfEntity bayDfEntity=new PubBayDfEntity();
                //初始化设备
                PubDeviceDfEntity deviceDfEntity= new PubDeviceDfEntity();
                //初始化部件
                PubComponentDfEntity componentDfEntity=new PubComponentDfEntity();
                //area_id
                pubWaylinePointDfEntity.setAreaName(UUID.randomUUID().toString());
                //bay_id
                pubWaylinePointDfEntity.setBayId(UUID.randomUUID().toString());
                //device_id
                pubWaylinePointDfEntity.setDeviceId(UUID.randomUUID().toString());
                //component_id
                pubWaylinePointDfEntity.setComponentId(UUID.randomUUID().toString());
                //point_code
                pubWaylinePointDfEntity.setPointCode(UUID.randomUUID().toString());


                //插入区域表
                areaDfEntity.setSubCode(pubWaylinePointDfEntity.getSubCode());
                areaDfEntity.setAreaId(pubWaylinePointDfEntity.getAreaId());
                areaDfEntity.setAreaName(pubWaylinePointDfEntity.getAreaName());
                pubAreaDfService.addArea(areaDfEntity);
                //间隔
                bayDfEntity.setSubCode(pubWaylinePointDfEntity.getSubCode());
                bayDfEntity.setAreaId(pubWaylinePointDfEntity.getAreaId());
                bayDfEntity.setBayId(pubWaylinePointDfEntity.getBayId());
                bayDfEntity.setBayName(pubWaylinePointDfEntity.getBayName());
                pubBayDfService.addBay(bayDfEntity);
                //设备
                deviceDfEntity.setSubCode(pubWaylinePointDfEntity.getSubCode());
                deviceDfEntity.setAreaId(pubWaylinePointDfEntity.getAreaId());
                deviceDfEntity.setBayId(pubWaylinePointDfEntity.getBayId());
                deviceDfEntity.setDeviceId(pubWaylinePointDfEntity.getDeviceId());
                deviceDfEntity.setDeviceName(pubWaylinePointDfEntity.getDeviceName());
                pubDeviceDfService.addDevice(deviceDfEntity);
                //部件
                componentDfEntity.setSubCode(pubWaylinePointDfEntity.getSubCode());
                componentDfEntity.setAreaId(pubWaylinePointDfEntity.getAreaId());
                componentDfEntity.setBayId(pubWaylinePointDfEntity.getBayId());
                componentDfEntity.setDeviceId(pubWaylinePointDfEntity.getDeviceId());
                componentDfEntity.setComponentId(pubWaylinePointDfEntity.getComponentId());
                componentDfEntity.setComponentName(pubWaylinePointDfEntity.getComponentName());
                pubComponentDfService.addComponent(componentDfEntity);
                //点位
                pubWaylinePointDfMapper.insert(pubWaylinePointDfEntity);

            }

        }
        //如果没有重复的数据进行插入，返回空列表
        return repeatList;
    }

    @Override
    public PubWaylinePointDfEntity getPubWaylinePointDfEntityById(Integer id) {
        //根据ID获取点位信息
        PubWaylinePointDfEntity entity = pubWaylinePointDfMapper.selectById(id);
        return entity;
    }

    @Override
    public Integer updatePubWaylinePointDfEntity(PubWaylinePointDfEntity pubWaylinePointDfEntity) {
        //更新点位信息
        //先查询当前航线的点位有没有被关联，要保持一一对应关系
        if(pubWaylinePointDfEntity.getWaylineId()!=null && pubWaylinePointDfEntity.getWaypointSequence()!=null){
            //查询有没有重复
            PubWaylinePointDfEntity waylinePointDfEntity= pubWaylinePointDfMapper.selectOne(new LambdaQueryWrapper<PubWaylinePointDfEntity>()
                    .eq(PubWaylinePointDfEntity::getWaylineId,pubWaylinePointDfEntity.getWaylineId())
                    .eq(PubWaylinePointDfEntity::getWaypointSequence,pubWaylinePointDfEntity.getWaypointSequence()));
            //判断是否为空
            if(waylinePointDfEntity!=null){
                return -1;
            }
        }
        int result = pubWaylinePointDfMapper.updateById(pubWaylinePointDfEntity);
       return result;
    }

    @Override
    public Integer deletePubWaylinePointDfEntityById(Integer id) {
        //根据ID删除点位信息
        int result = pubWaylinePointDfMapper.deleteById(id);
       return result;
    }

    //查询所有点位信息
    @Override
    public PaginationData<PubWaylinePointDfEntity> getAllPubWaylinePointDfEntities(long page, long pageSize) {
        Page<PubWaylinePointDfEntity> pageData = pubWaylinePointDfMapper.selectPage(
                new Page<PubWaylinePointDfEntity>(page, pageSize),
                new LambdaQueryWrapper<PubWaylinePointDfEntity>()
                        .orderByDesc(PubWaylinePointDfEntity::getId));
        List<PubWaylinePointDfEntity> records = pageData.getRecords();
        return new PaginationData<PubWaylinePointDfEntity>(records, new Pagination(pageData.getCurrent(), pageData.getSize(), pageData.getTotal()));
    }

    @Override
    public List<PubWaylinePointDfEntity> getPointBySubCode(String sub_code) {

        // 根据 sub_code 查询点位信息
        List<PubWaylinePointDfEntity> entities = pubWaylinePointDfMapper.selectList(new LambdaQueryWrapper<PubWaylinePointDfEntity>().eq(PubWaylinePointDfEntity::getSubCode,sub_code));
        return entities;
    }

    @Override
    public PubWaylinePointDfEntity getPointByWaylineIdAndSequence(String wayline_id, String waypoint_sequence) {
        //根据航线id和航点序列查询
        PubWaylinePointDfEntity pubWaylinePointDfEntity=pubWaylinePointDfMapper.selectOne(new LambdaQueryWrapper<PubWaylinePointDfEntity>()
                .eq(PubWaylinePointDfEntity::getWaylineId,wayline_id).eq(PubWaylinePointDfEntity::getWaypointSequence,waypoint_sequence));
        return pubWaylinePointDfEntity;
    }

    @Override
    public List<PubWaylinePointDfEntity> getPointsByComponentId(String component_id) {
        List<PubWaylinePointDfEntity> entities = pubWaylinePointDfMapper.selectList(new LambdaQueryWrapper<PubWaylinePointDfEntity>().eq(PubWaylinePointDfEntity::getComponentId,component_id));
        return entities;
    }

    @Override
    public Map getPointsByParam(Map map) {
        PageUtil.setPageArgs(map);
        Map map1=new HashMap();
        List<PubWaylinePointDfEntity> entities = pubWaylinePointDfMapper.getPointsByParam(map);
        int pointsByParamCount = pubWaylinePointDfMapper.getPointsByParamCount(map);
        map1.put("count",pointsByParamCount);
        map1.put("data",entities);
        return map1;
    }
}
