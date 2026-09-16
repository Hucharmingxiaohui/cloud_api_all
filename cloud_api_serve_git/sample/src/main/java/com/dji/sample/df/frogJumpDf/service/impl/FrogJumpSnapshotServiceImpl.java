package com.dji.sample.df.frogJumpDf.service.impl;

import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpDockInfoDTO;
import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpDroneSnapshotDTO;
import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpMissionSnapshotDTO;
import com.dji.sample.df.frogJumpDf.service.FrogJumpSnapshotService;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sdk.mqtt.osd.IOsdCacheService;
import com.dji.sdk.mqtt.osd.TopicOsdRequest;
import com.dji.sdk.mqtt.state.TopicStateRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Service
public class FrogJumpSnapshotServiceImpl implements FrogJumpSnapshotService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    @Autowired
    private IDeviceRedisService deviceRedisService;

    @Autowired
    private IOsdCacheService osdCacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Optional<FrogJumpMissionSnapshotDTO> buildMissionSnapshot(String droneSn, String takeoffDockSn, String landingDockSn) {
        if (!StringUtils.hasText(droneSn) || !StringUtils.hasText(takeoffDockSn) || !StringUtils.hasText(landingDockSn)) {
            return Optional.empty();
        }

        return Optional.of(FrogJumpMissionSnapshotDTO.builder()
                .droneSn(droneSn)
                .takeoffDockSn(takeoffDockSn)
                .landingDockSn(landingDockSn)
                .droneSnapshot(buildDroneSnapshot(droneSn))
                .takeoffDockSnapshot(buildDockSnapshot(takeoffDockSn, "takeoff", 1))
                .landingDockSnapshot(buildDockSnapshot(landingDockSn, "landing", 2))
                .build());
    }

    private FrogJumpDroneSnapshotDTO buildDroneSnapshot(String droneSn) {
        Map<String, Object> osdData = getOsdData(droneSn);
        Map<String, Object> stateData = getStateData(droneSn);
        return FrogJumpDroneSnapshotDTO.builder()
                .sn(droneSn)
                .bestLinkGateway(value(osdData, "best_link_gateway"))
                .wirelessLinkTopo(value(stateData, "wireless_link_topo"))
                .osdData(osdData)
                .stateData(stateData)
                .build();
    }

    private FrogJumpDockInfoDTO buildDockSnapshot(String dockSn, String dockType, Integer index) {
        Map<String, Object> osdData = getOsdData(dockSn);
        Map<String, Object> stateData = getStateData(dockSn);
        return FrogJumpDockInfoDTO.builder()
                .sn(dockSn)
                .dockType(dockType)
                .index(index)
                .latitude(value(osdData, "latitude"))
                .longitude(value(osdData, "longitude"))
                .height(value(osdData, "height"))
                .heading(value(osdData, "heading"))
                .homePositionIsValid(value(osdData, "home_position_is_valid"))
                .alternateLandPoint(value(osdData, "alternate_land_point"))
                .rtcmInfo(value(stateData, "rtcm_info"))
                .osdData(osdData)
                .stateData(stateData)
                .build();
    }

    private Map<String, Object> getOsdData(String sn) {
        Optional<Map<String, Object>> rawOsdData = osdCacheService.getDeviceRawOsd(sn, TopicOsdRequest.class)
                .map(TopicOsdRequest::getData)
                .map(this::toMap);
        if (rawOsdData.isPresent()) {
            return rawOsdData.get();
        }
        return deviceRedisService.getDeviceOsd(sn, Object.class)
                .map(this::toMap)
                .orElse(null);
    }

    private Map<String, Object> getStateData(String sn) {
        return deviceRedisService.getDeviceRawState(sn, TopicStateRequest.class)
                .map(TopicStateRequest::getData)
                .map(this::toMap)
                .orElse(null);
    }

    private Map<String, Object> toMap(Object data) {
        if (data == null) {
            return null;
        }
        return objectMapper.convertValue(data, MAP_TYPE);
    }

    private Object value(Map<String, Object> data, String key) {
        return data == null ? null : data.get(key);
    }
}
