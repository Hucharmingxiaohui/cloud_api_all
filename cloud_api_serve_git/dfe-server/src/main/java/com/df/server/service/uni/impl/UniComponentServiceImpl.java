package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.server.entity.uni.UniBayEntity;
import com.df.server.entity.uni.UniComponentEntity;
import com.df.server.entity.uni.UniDeviceEntity;
import com.df.server.mapper.uni.UniComponentMapper;
import com.df.server.service.uni.UniBayService;
import com.df.server.service.uni.UniComponentService;
import com.df.server.service.uni.UniDeviceService;
import com.df.server.vo.UniComponent.UniComponentVO;
import com.df.server.vo.UniDevice.UniDeviceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("uniComponentService")
public class UniComponentServiceImpl extends ServiceImpl<UniComponentMapper, UniComponentEntity> implements UniComponentService {

    @Autowired
    private UniDeviceService uniDeviceService;
    @Autowired
    private UniBayService uniBayService;

    @Override
    public UniComponentEntity getByName(String subCode, String deviceId, String componentName) {
        return baseMapper.getByName(subCode, deviceId, componentName);
    }

    @Override
    public UniComponentEntity getByComponentId(String subCode, String componentId) {
        return baseMapper.getByComponentId(subCode, componentId);
    }

    @Override
    public List<UniComponentVO> listByDeviceId(String subCode, String deviceId) {
        return baseMapper.listByDeviceId(subCode, deviceId);
    }

    @Override
    public void deleteByBay(String subCode, String bayId) {
        List<String> deviceIdList =
                uniDeviceService.listByBayId(subCode, bayId)
                        .stream().map(UniDeviceVO::getDeviceId).collect(Collectors.toList());
        this.lambdaUpdate().in(UniComponentEntity::getDeviceId, deviceIdList).remove();
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
            List<String> deviceIdList = uniDeviceService.lambdaQuery()
                    .eq(UniDeviceEntity::getSubCode, subCode)
                    .in(UniDeviceEntity::getBayId, bayIdList)
                    .list()
                    .stream()
                    .map(UniDeviceEntity::getDeviceId)
                    .collect(Collectors.toList());
            if (deviceIdList.size() > 0) {
                QueryWrapper<UniComponentEntity> comwrapper = new QueryWrapper<>();
                comwrapper.in("device_id", deviceIdList);
                this.remove(comwrapper);
            }
        }
    }

    @Override
    public int deleteBySubCode(String subCode) {
        return baseMapper.delete(
                this.lambdaQuery().eq(UniComponentEntity::getSubCode, subCode).getWrapper()
        );
    }
}
