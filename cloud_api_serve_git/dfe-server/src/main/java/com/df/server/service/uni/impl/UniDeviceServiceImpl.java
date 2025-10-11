package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.server.entity.uni.UniBayEntity;
import com.df.server.entity.uni.UniDeviceEntity;
import com.df.server.mapper.uni.UniDeviceMapper;
import com.df.server.service.uni.UniBayService;
import com.df.server.service.uni.UniDeviceService;
import com.df.server.vo.UniDevice.UniDeviceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("uniDeviceService")
public class UniDeviceServiceImpl extends ServiceImpl<UniDeviceMapper, UniDeviceEntity> implements UniDeviceService {

    @Autowired
    private UniBayService uniBayService;


    @Override
    public UniDeviceEntity getByName(String subCode, String bayId, String deviceName) {
        return baseMapper.getByName(subCode, bayId, deviceName);
    }

    @Override
    public UniDeviceEntity getByDeviceId(String subCode, String deviceId) {
        return baseMapper.getByDeviceId(subCode, deviceId);
    }

    @Override
    public List<UniDeviceVO> listByBayId(String subCode, String bayId) {
        return baseMapper.listByBayId(subCode, bayId);
    }

    @Override
    public void deleteByArea(String subCode, String areaId) {
        List<String> bayIdList =
                uniBayService.lambdaQuery()
                        .eq(UniBayEntity::getSubCode, subCode)
                        .eq(UniBayEntity::getAreaId, areaId)
                        .list()
                        .stream()
                        .map(UniBayEntity::getBayId)
                        .collect(Collectors.toList());
        if (bayIdList.size() > 0) {
            this.lambdaUpdate().eq(UniDeviceEntity::getSubCode, subCode)
                    .in(UniDeviceEntity::getBayId, bayIdList).remove();
        }
    }

    @Override
    public int deleteBySubCode(String subCode) {
        return baseMapper.delete(
                this.lambdaQuery().eq(UniDeviceEntity::getSubCode, subCode).getWrapper()
        );
    }
}
