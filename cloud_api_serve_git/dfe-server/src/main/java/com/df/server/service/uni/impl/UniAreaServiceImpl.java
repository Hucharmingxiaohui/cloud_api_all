package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.server.entity.uni.UniAreaEntity;
import com.df.server.entity.uni.UniBayEntity;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.mapper.uni.UniAreaMapper;
import com.df.server.service.uni.*;
import com.df.server.vo.UniArea.UniAreaVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("uniAreaService")
public class UniAreaServiceImpl extends ServiceImpl<UniAreaMapper, UniAreaEntity> implements UniAreaService {

    @Autowired
    private UniBayService uniBayService;
    @Autowired
    private UniDeviceService uniDeviceService;
    @Autowired
    private UniComponentService uniComponentService;
    @Autowired
    @Lazy
    private UniPointService uniPointService;


    @Override
    public UniAreaEntity getByName(String areaName, String subCode) {
        return this.lambdaQuery()
                .eq(UniAreaEntity::getSubCode, subCode)
                .eq(UniAreaEntity::getAreaName, areaName)
                .one();
    }

    @Override
    public UniAreaEntity getByAreaId(String subCode, String areaId) {
        return this.lambdaQuery()
                .eq(UniAreaEntity::getSubCode, subCode)
                .eq(UniAreaEntity::getAreaId, areaId)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer tableId) {
        UniAreaEntity area = this.getById(tableId);
        if (area != null) {
            String subCode = area.getSubCode();
            String areaId = area.getAreaId();
            //删除点位
            uniPointService.lambdaUpdate().eq(UniPointEntity::getSubCode, subCode).eq(UniPointEntity::getAreaId, areaId).remove();
            //删除部件
            uniComponentService.deleteByArea(subCode, area.getAreaId());
            //删除设备
            uniDeviceService.deleteByArea(subCode, area.getAreaId());
            //删除间隔
            uniBayService.lambdaUpdate().eq(UniBayEntity::getSubCode, subCode).eq(UniBayEntity::getAreaId, areaId).remove();
            //删除区域
            this.removeById(tableId);
        }
    }

    @Override
    public int deleteBySubCode(String subCode) {
        return baseMapper.delete(
                this.lambdaQuery().eq(UniAreaEntity::getSubCode, subCode).getWrapper()
        );
    }

    @Override
    public List<UniAreaVO> listBySubCode(String subCode) {
        return baseMapper.listBySubCode(subCode);
    }
}
