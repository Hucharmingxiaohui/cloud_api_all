package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineFileDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineFileDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineFileDfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PubWaylineFileDfServiceImpl implements PubWaylineFileDfService {
    @Autowired
    private PubWaylineFileDfMapper pubWaylineFileDfMapper;
    @Override
    public Integer addPubWayline(PubWaylineFileDfEntity pubWaylineFileDfEntity) {
        //校验场站和航线
        PubWaylineFileDfEntity pubWaylineFileDfEntity1 = pubWaylineFileDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineFileDfEntity>().eq(PubWaylineFileDfEntity::getWaylineId,pubWaylineFileDfEntity.getWaylineId()).eq(PubWaylineFileDfEntity::getSubCode,pubWaylineFileDfEntity.getSubCode()));
        if(pubWaylineFileDfEntity1 != null){
            return -1;//航线已经存在当前场站下
        }else{
            pubWaylineFileDfMapper.insert(pubWaylineFileDfEntity);
            return 0;
        }

    }
    //按场站专业查询
    @Override
    public List<PubWaylineFileDfEntity> getPubWaylineBySubCodeMajor(String sub_code, String major) {
        List<PubWaylineFileDfEntity> WaylineList = pubWaylineFileDfMapper.selectList(new LambdaQueryWrapper<PubWaylineFileDfEntity>().eq(PubWaylineFileDfEntity::getSubCode,sub_code).
                eq(PubWaylineFileDfEntity::getMajor,major));

        return WaylineList;
    }
}
