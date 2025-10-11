package com.dji.sample.manage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.component.redis.RedisConst;
import com.dji.sample.component.redis.RedisOpsUtils;
import com.dji.sample.manage.dao.IDevicePayloadMapper;
import com.dji.sample.manage.model.dto.CapacityCameraDTO;
import com.dji.sample.manage.model.dto.DeviceDictionaryDTO;
import com.dji.sample.manage.model.dto.DevicePayloadDTO;
import com.dji.sample.manage.model.entity.DevicePayloadEntity;
import com.dji.sample.manage.model.receiver.CapacityCameraReceiver;
import com.dji.sample.manage.service.ICameraVideoService;
import com.dji.sample.manage.service.ICapacityCameraService;
import com.dji.sample.manage.service.IDeviceDictionaryService;
import com.dji.sample.manage.service.IDevicePayloadService;
import com.dji.sdk.cloudapi.device.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sean.zhou
 * @date 2021/11/19
 * @version 0.1
 */
@Service
public class CapacityCameraServiceImpl implements ICapacityCameraService {

    @Autowired
    private ICameraVideoService cameraVideoService;

    @Autowired
    private IDeviceDictionaryService dictionaryService;
    @Autowired
    private IDevicePayloadMapper mapper;

    @Override
    public List<CapacityCameraDTO> getCapacityCameraByDeviceSn(String deviceSn) {
        //存储相机列表
        List<CapacityCameraDTO> cameraDTOList =new ArrayList<>();
        //
        List<DevicePayloadDTO> devicePayloadList=mapper.selectList(
                        new LambdaQueryWrapper<DevicePayloadEntity>()
                                .eq(DevicePayloadEntity::getDeviceSn, deviceSn))
                .stream()
                .map(this::payloadEntityConvertToDTO)
                .collect(Collectors.toList());
        System.out.println(devicePayloadList);
        for(int i=0;i<devicePayloadList.size();i++)
        {
            //相机信息
            CapacityCameraDTO capacityCameraDTO=new CapacityCameraDTO();
            //负载信息
            DevicePayloadDTO devicePayloadDTO=devicePayloadList.get(i);
            //sn
            capacityCameraDTO.setDeviceSn(deviceSn);
            //id/payload_sn
            capacityCameraDTO.setId(devicePayloadDTO.getPayloadSn());
            //name
            capacityCameraDTO.setName(devicePayloadDTO.getPayloadName());
            //index
            capacityCameraDTO.setIndex(devicePayloadDTO.getPayloadIndex().toString());
            //type
            capacityCameraDTO.setType(devicePayloadDTO.getPayloadIndex().getType().name());
            //
            cameraDTOList.add(capacityCameraDTO);
        }
        return cameraDTOList;
//        return (List<CapacityCameraDTO>) RedisOpsUtils.hashGet(RedisConst.LIVE_CAPACITY, deviceSn);
    }

    @Override
    public Boolean deleteCapacityCameraByDeviceSn(String deviceSn) {
        return RedisOpsUtils.hashDel(RedisConst.LIVE_CAPACITY, new String[]{deviceSn});
    }

    @Override
    public void saveCapacityCameraReceiverList(List<CapacityCameraReceiver> capacityCameraReceivers, String deviceSn) {
        List<CapacityCameraDTO> capacity = capacityCameraReceivers.stream()
                .map(this::receiver2Dto).collect(Collectors.toList());
        RedisOpsUtils.hashSet(RedisConst.LIVE_CAPACITY, deviceSn, capacity);
    }

    public CapacityCameraDTO receiver2Dto(CapacityCameraReceiver receiver) {
        CapacityCameraDTO.CapacityCameraDTOBuilder builder = CapacityCameraDTO.builder();
        if (receiver == null) {
            return builder.build();
        }
        PayloadIndex cameraIndex = receiver.getCameraIndex();
        // The cameraIndex consists of type and subType and the index of the payload hanging on the drone.
        // type-subType-index
        Optional<DeviceDictionaryDTO> dictionaryOpt = dictionaryService.getOneDictionaryInfoByTypeSubType(
                DeviceDomainEnum.PAYLOAD.getDomain(), cameraIndex.getType().getType(), cameraIndex.getSubType().getSubType());
        dictionaryOpt.ifPresent(dictionary -> builder.name(dictionary.getDeviceName()));

        return builder
                .id(UUID.randomUUID().toString())
                .videosList(receiver.getVideoList()
                        .stream()
                        .map(cameraVideoService::receiver2Dto)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .index(receiver.getCameraIndex().toString())
                .build();
    }
    /**
     * Convert database entity objects into payload data transfer object.
     * @param entity
     * @return
     */
    private DevicePayloadDTO payloadEntityConvertToDTO(DevicePayloadEntity entity) {
//        System.out.println(entity);
        DevicePayloadDTO.DevicePayloadDTOBuilder builder = DevicePayloadDTO.builder();
        if (entity != null) {
            builder.payloadSn(entity.getPayloadSn())
                    .payloadName(entity.getPayloadName())
                    .payloadDesc(entity.getPayloadDesc())
                    .index(entity.getPayloadIndex())
                    .payloadIndex(new PayloadIndex()
                            .setType(DeviceTypeEnum.find(entity.getPayloadType()))
                            .setSubType(DeviceSubTypeEnum.find(entity.getSubType()))
                            .setPosition(PayloadPositionEnum.find(entity.getPayloadIndex())))
                    .controlSource(ControlSourceEnum.find(entity.getControlSource()));
        }
        return builder.build();
    }
}