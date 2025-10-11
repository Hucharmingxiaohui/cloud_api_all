package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.server.entity.uni.UniBayEntity;
import com.df.server.entity.uni.UniDeviceEntity;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.mapper.uni.UniBayMapper;
import com.df.server.service.uni.UniBayService;
import com.df.server.service.uni.UniComponentService;
import com.df.server.service.uni.UniDeviceService;
import com.df.server.service.uni.UniPointService;
import com.df.server.vo.UniBay.UniBayVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("uniBayService")
public class UniBayServiceImpl extends ServiceImpl<UniBayMapper, UniBayEntity> implements UniBayService {

    @Autowired
    private UniDeviceService uniDeviceService;
    @Autowired
    private UniComponentService uniComponentService;
    @Autowired
    @Lazy
    private UniPointService uniPointService;


    @Override
    public UniBayEntity getByName(String subCode, String areaId, String bayName) {
        return baseMapper.getByName(subCode, areaId, bayName);
    }

    @Override
    public UniBayEntity getByBayId(String subCode, String bayId) {
        return this.lambdaQuery()
                .eq(UniBayEntity::getSubCode, subCode)
                .eq(UniBayEntity::getBayId, bayId)
                .one();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBay(Integer tableId) {
        UniBayEntity bay = this.getById(tableId);
        if (bay != null) {
            String subCode = bay.getSubCode();
            String bayId = bay.getBayId();
            //删除点位
            uniPointService.lambdaUpdate()
                    .eq(UniPointEntity::getSubCode, subCode)
                    .eq(UniPointEntity::getBayId, bayId)
                    .remove();
            //；删除部件
            uniComponentService.deleteByBay(subCode, bayId);
            //；删除设备
            uniDeviceService.lambdaUpdate()
                    .eq(UniDeviceEntity::getSubCode, subCode)
                    .eq(UniDeviceEntity::getBayId, bayId)
                    .remove();
            //删除间隔
            this.removeById(tableId);
        }
    }

    @Override
    public int deleteBySubCode(String subCode) {
        return baseMapper.delete(
                this.lambdaQuery().eq(UniBayEntity::getSubCode, subCode).getWrapper()
        );
    }

    @Override
    public List<UniBayVO> listByAreaId(String subCode, String areaId) {
        return baseMapper.listByAreaId(subCode, areaId);
    }
}
