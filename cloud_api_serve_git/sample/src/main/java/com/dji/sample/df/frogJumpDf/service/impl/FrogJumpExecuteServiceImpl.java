package com.dji.sample.df.frogJumpDf.service.impl;

import com.df.framework.redis.RedisUtils;
import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpDockInfoDTO;
import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpExecuteParam;
import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpMissionSnapshotDTO;
import com.dji.sample.df.frogJumpDf.service.FrogJumpExecuteService;
import com.dji.sample.df.frogJumpDf.service.FrogJumpSnapshotService;
import com.dji.sample.df.uavCommonHandleDf.handler.JobControlHandler;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sample.wayline.model.enums.WaylineJobStatusEnum;
import com.dji.sample.wayline.service.IWaylineJobService;
import com.dji.sample.wayline.service.IWaylineRedisService;
import com.dji.sdk.cloudapi.wayline.FlighttaskProgress;
import com.dji.sdk.cloudapi.wayline.FlighttaskExecuteRequest;
import com.dji.sdk.cloudapi.wayline.MultiDockTask;
import com.dji.sdk.cloudapi.wayline.MultiDockTaskDockInfo;
import com.dji.sdk.cloudapi.wayline.api.AbstractWaylineService;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.mqtt.osd.IOsdCacheService;
import com.dji.sdk.mqtt.osd.TopicOsdRequest;
import com.dji.sdk.mqtt.services.ServicesReplyData;
import com.dji.sdk.mqtt.services.TopicServicesResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class FrogJumpExecuteServiceImpl implements FrogJumpExecuteService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    @Autowired
    private FrogJumpSnapshotService frogJumpSnapshotService;

    @Autowired
    private IOsdCacheService osdCacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IDeviceRedisService deviceRedisService;

    @Autowired
    private IWaylineJobService waylineJobService;

    @Autowired
    private IWaylineRedisService waylineRedisService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    @Qualifier("SDKWaylineService")
    private AbstractWaylineService abstractWaylineService;

    @Override
    public HttpResultResponse execute(FrogJumpExecuteParam param) {
        log.info("Frog jump execute start: flightId={}, takeoffDockSn={}, landingDockSn={}, inputDroneSn={}",
                param.getFlightId(), param.getTakeoffDockSn(), param.getLandingDockSn(), param.getDroneSn());
        if (param.getTakeoffDockSn().equals(param.getLandingDockSn())) {
            log.warn("Frog jump execute blocked: takeoffDockSn and landingDockSn are same, dockSn={}", param.getTakeoffDockSn());
            return HttpResultResponse.error("Takeoff dock and landing dock cannot be the same.");
        }
        String droneSn = StringUtils.hasText(param.getDroneSn()) ? param.getDroneSn() : getDroneSnFromDockOsd(param.getTakeoffDockSn());
        if (!StringUtils.hasText(droneSn)) {
            log.warn("Frog jump execute blocked: drone sn missing and cannot be inferred, takeoffDockSn={}", param.getTakeoffDockSn());
            return HttpResultResponse.error("Drone sn is missing and cannot be inferred from takeoff dock osd.");
        }
        log.info("Frog jump execute drone resolved: flightId={}, droneSn={}", param.getFlightId(), droneSn);
        Optional<FrogJumpMissionSnapshotDTO> snapshotOpt = frogJumpSnapshotService.buildMissionSnapshot(
                droneSn, param.getTakeoffDockSn(), param.getLandingDockSn());
        if (snapshotOpt.isEmpty()) {
            log.warn("Frog jump execute blocked: snapshot not ready, flightId={}, droneSn={}, takeoffDockSn={}, landingDockSn={}",
                    param.getFlightId(), droneSn, param.getTakeoffDockSn(), param.getLandingDockSn());
            return HttpResultResponse.error("Frog jump snapshot data is not ready.");
        }

        FrogJumpMissionSnapshotDTO snapshot = snapshotOpt.get();
        log.info("Frog jump snapshot summary: flightId={}, {}", param.getFlightId(), buildSnapshotSummary(snapshot));
        List<String> missingFields = validateSnapshot(snapshot);
        if (!missingFields.isEmpty()) {
            log.warn("Frog jump execute blocked: flightId={}, missingFields={}", param.getFlightId(), missingFields);
            return HttpResultResponse.error("Frog jump snapshot missing fields: " + String.join(",", missingFields));
        }

        if (!deviceRedisService.checkDeviceOnline(param.getTakeoffDockSn())) {
            log.warn("Frog jump execute blocked: takeoff dock offline, flightId={}, takeoffDockSn={}", param.getFlightId(), param.getTakeoffDockSn());
            return HttpResultResponse.error("Takeoff dock is offline.");
        }
        if (!deviceRedisService.checkDeviceOnline(param.getLandingDockSn())) {
            log.warn("Frog jump execute blocked: landing dock offline, flightId={}, landingDockSn={}", param.getFlightId(), param.getLandingDockSn());
            return HttpResultResponse.error("Landing dock is offline.");
        }

        FlighttaskExecuteRequest request = new FlighttaskExecuteRequest()
                .setFlightId(param.getFlightId())
                .setMultiDockTask(buildMultiDockTask(snapshot));
        log.info("Frog jump execute request: flightId={}, payload={}", param.getFlightId(), toSafeJson(request));

        TopicServicesResponse<ServicesReplyData> takeoffServiceReply = abstractWaylineService.flighttaskExecute(
                SDKManager.getDeviceSDK(param.getTakeoffDockSn()), request);
        log.info("Frog jump execute reply: flightId={}, targetName=takeoffDock, targetDockSn={}, result={}", param.getFlightId(),
                param.getTakeoffDockSn(), takeoffServiceReply == null || takeoffServiceReply.getData() == null ? null : takeoffServiceReply.getData().getResult());
        if (!takeoffServiceReply.getData().getResult().isSuccess()) {
            log.info("Frog jump execute ====> Error: {}, targetDockSn={}", takeoffServiceReply.getData().getResult(), param.getTakeoffDockSn());
            waylineJobService.updateJob(WaylineJobDTO.builder()
                    .jobId(param.getFlightId())
                    .executeTime(LocalDateTime.now())
                    .status(WaylineJobStatusEnum.FAILED.getVal())
                    .completedTime(LocalDateTime.now())
                    .code(takeoffServiceReply.getData().getResult().getCode()).build());
            return HttpResultResponse.error(takeoffServiceReply.getData().getResult().getCode(), "Failed to execute frog jump task.");
        }

        TopicServicesResponse<ServicesReplyData> landingServiceReply = abstractWaylineService.flighttaskExecute(
                SDKManager.getDeviceSDK(param.getLandingDockSn()), request);
        log.info("Frog jump execute reply: flightId={}, targetName=landingDock, targetDockSn={}, result={}", param.getFlightId(),
                param.getLandingDockSn(), landingServiceReply == null || landingServiceReply.getData() == null ? null : landingServiceReply.getData().getResult());
        if (!landingServiceReply.getData().getResult().isSuccess()) {
            log.info("Frog jump execute ====> Error: {}, targetDockSn={}", landingServiceReply.getData().getResult(), param.getLandingDockSn());
            waylineJobService.updateJob(WaylineJobDTO.builder()
                    .jobId(param.getFlightId())
                    .executeTime(LocalDateTime.now())
                    .status(WaylineJobStatusEnum.FAILED.getVal())
                    .completedTime(LocalDateTime.now())
                    .code(landingServiceReply.getData().getResult().getCode()).build());
            return HttpResultResponse.error(landingServiceReply.getData().getResult().getCode(), "Failed to execute frog jump task on landing dock.");
        }
        waylineJobService.updateJob(WaylineJobDTO.builder()
                .jobId(param.getFlightId())
                .executeTime(LocalDateTime.now())
                .status(WaylineJobStatusEnum.IN_PROGRESS.getVal())
                .build());
        redisUtils.set("jobId", param.getFlightId());
        redisUtils.set("isCenterTask", "0");
        JobControlHandler.startMonitoringTask(param.getFlightId(), param.getFlightId());
        waylineRedisService.setRunningWaylineJob(param.getTakeoffDockSn(), com.dji.sample.component.mqtt.model.EventsReceiver.<FlighttaskProgress>builder()
                .bid(param.getFlightId()).sn(param.getTakeoffDockSn()).build());
        return HttpResultResponse.success();
    }

    private String getDroneSnFromDockOsd(String dockSn) {
        return osdCacheService.getDeviceRawOsd(dockSn, TopicOsdRequest.class)
                .map(TopicOsdRequest::getData)
                .map(data -> objectMapper.convertValue(data, MAP_TYPE))
                .map(data -> asMap(data.get("sub_device")))
                .map(subDevice -> subDevice.get("device_sn"))
                .map(String::valueOf)
                .orElse(null);
    }

    private MultiDockTask buildMultiDockTask(FrogJumpMissionSnapshotDTO snapshot) {
        return new MultiDockTask()
                .setDockInfos(Arrays.asList(
                        buildDockInfo(snapshot.getTakeoffDockSnapshot()),
                        buildDockInfo(snapshot.getLandingDockSnapshot())))
                .setWirelessLinkTopo(buildWirelessLinkTopo(snapshot));
    }

    private MultiDockTaskDockInfo buildDockInfo(FrogJumpDockInfoDTO dockInfo) {
        return new MultiDockTaskDockInfo()
                .setDockType(dockInfo.getDockType())
                .setSn(dockInfo.getSn())
                .setIndex(dockInfo.getIndex())
                .setLatitude(dockInfo.getLatitude())
                .setLongitude(dockInfo.getLongitude())
                .setHeight(dockInfo.getHeight())
                .setHeading(dockInfo.getHeading())
                .setHomePositionIsValid(dockInfo.getHomePositionIsValid())
                .setAlternateLandPoint(dockInfo.getAlternateLandPoint())
                .setRtcmInfo(dockInfo.getRtcmInfo());
    }

    private Map<String, Object> buildWirelessLinkTopo(FrogJumpMissionSnapshotDTO snapshot) {
        Map<String, Object> droneTopo = asMap(snapshot.getDroneSnapshot().getWirelessLinkTopo());
        Map<String, Object> takeoffTopo = asMap(snapshot.getTakeoffDockSnapshot().getStateData().get("wireless_link_topo"));
        Map<String, Object> landingTopo = asMap(snapshot.getLandingDockSnapshot().getStateData().get("wireless_link_topo"));

        Map<String, Object> topo = new LinkedHashMap<>();
        topo.put("center_node", droneTopo.get("center_node"));
        topo.put("leaf_nodes", Arrays.asList(firstLeafNode(takeoffTopo, 1), firstLeafNode(landingTopo, 2)));
        topo.put("secret_code", droneTopo.get("secret_code"));
        return topo;
    }

    private Map<String, Object> firstLeafNode(Map<String, Object> topo, Integer controlSourceIndex) {
        List<Map<String, Object>> leafNodes = (List<Map<String, Object>>) topo.get("leaf_nodes");
        Map<String, Object> leafNode = new LinkedHashMap<>(leafNodes.get(0));
        leafNode.put("control_source_index", controlSourceIndex);
        return leafNode;
    }

    private List<String> validateSnapshot(FrogJumpMissionSnapshotDTO snapshot) {
        List<String> missing = new ArrayList<>();
        validateDockInfo(snapshot.getTakeoffDockSnapshot(), "takeoffDock", missing);
        validateDockInfo(snapshot.getLandingDockSnapshot(), "landingDock", missing);
        if (snapshot.getDroneSnapshot() == null) {
            missing.add("droneSnapshot");
            return missing;
        }
        Map<String, Object> droneTopo = asMap(snapshot.getDroneSnapshot().getWirelessLinkTopo());
        if (CollectionUtils.isEmpty(droneTopo)) {
            missing.add("drone.wireless_link_topo");
            return missing;
        }
        require(droneTopo.get("center_node"), "drone.wireless_link_topo.center_node", missing);
        require(droneTopo.get("secret_code"), "drone.wireless_link_topo.secret_code", missing);
        validateDockTopo(snapshot.getTakeoffDockSnapshot(), "takeoffDock", missing);
        validateDockTopo(snapshot.getLandingDockSnapshot(), "landingDock", missing);
        validateSameRtcmInfo(snapshot.getTakeoffDockSnapshot(), snapshot.getLandingDockSnapshot(), missing);
        return missing;
    }

    private void validateDockInfo(FrogJumpDockInfoDTO dockInfo, String prefix, List<String> missing) {
        if (dockInfo == null) {
            missing.add(prefix);
            return;
        }
        require(dockInfo.getLatitude(), prefix + ".latitude", missing);
        require(dockInfo.getLongitude(), prefix + ".longitude", missing);
        require(dockInfo.getHeight(), prefix + ".height", missing);
        require(dockInfo.getHeading(), prefix + ".heading", missing);
        require(dockInfo.getHomePositionIsValid(), prefix + ".home_position_is_valid", missing);
        require(dockInfo.getAlternateLandPoint(), prefix + ".alternate_land_point", missing);
        require(dockInfo.getRtcmInfo(), prefix + ".rtcm_info", missing);
        if (dockInfo.getRtcmInfo() != null) {
            Map<String, Object> rtcmInfo = asMap(dockInfo.getRtcmInfo());
            if (rtcmInfo == null) {
                missing.add(prefix + ".rtcm_info");
            } else {
                require(rtcmInfo.get("source_type"), prefix + ".rtcm_info.source_type", missing);
                require(rtcmInfo.get("host"), prefix + ".rtcm_info.host", missing);
                require(rtcmInfo.get("port"), prefix + ".rtcm_info.port", missing);
                require(rtcmInfo.get("mount_point"), prefix + ".rtcm_info.mount_point", missing);
            }
        }
    }

    private void validateSameRtcmInfo(FrogJumpDockInfoDTO takeoffDock, FrogJumpDockInfoDTO landingDock, List<String> missing) {
        Map<String, Object> takeoffRtcmInfo = takeoffDock == null ? null : asMap(takeoffDock.getRtcmInfo());
        Map<String, Object> landingRtcmInfo = landingDock == null ? null : asMap(landingDock.getRtcmInfo());
        if (takeoffRtcmInfo == null || landingRtcmInfo == null) {
            return;
        }
        requireSameRtcmField(takeoffRtcmInfo, landingRtcmInfo, "source_type", missing);
        requireSameRtcmField(takeoffRtcmInfo, landingRtcmInfo, "host", missing);
        requireSameRtcmField(takeoffRtcmInfo, landingRtcmInfo, "port", missing);
        requireSameRtcmField(takeoffRtcmInfo, landingRtcmInfo, "mount_point", missing);
    }

    private void requireSameRtcmField(Map<String, Object> takeoffRtcmInfo, Map<String, Object> landingRtcmInfo,
                                      String field, List<String> missing) {
        Object takeoffValue = takeoffRtcmInfo.get(field);
        Object landingValue = landingRtcmInfo.get(field);
        if (takeoffValue == null || landingValue == null) {
            return;
        }
        if (!String.valueOf(takeoffValue).equals(String.valueOf(landingValue))) {
            missing.add("rtcm_info." + field + "_not_same");
        }
    }

    private void validateDockTopo(FrogJumpDockInfoDTO dockInfo, String prefix, List<String> missing) {
        if (dockInfo == null || CollectionUtils.isEmpty(dockInfo.getStateData())) {
            missing.add(prefix + ".stateData");
            return;
        }
        Map<String, Object> topo = asMap(dockInfo.getStateData().get("wireless_link_topo"));
        if (CollectionUtils.isEmpty(topo)) {
            missing.add(prefix + ".wireless_link_topo");
            return;
        }
        List<Map<String, Object>> leafNodes = (List<Map<String, Object>>) topo.get("leaf_nodes");
        if (CollectionUtils.isEmpty(leafNodes)) {
            missing.add(prefix + ".wireless_link_topo.leaf_nodes");
        }
    }

    private void require(Object value, String field, List<String> missing) {
        if (value == null) {
            missing.add(field);
        }
    }

    private String buildSnapshotSummary(FrogJumpMissionSnapshotDTO snapshot) {
        Map<String, Object> droneTopo = asMap(snapshot.getDroneSnapshot().getWirelessLinkTopo());
        Map<String, Object> takeoffTopo = asMap(snapshot.getTakeoffDockSnapshot().getStateData().get("wireless_link_topo"));
        Map<String, Object> landingTopo = asMap(snapshot.getLandingDockSnapshot().getStateData().get("wireless_link_topo"));
        Map<String, Object> takeoffRtcm = asMap(snapshot.getTakeoffDockSnapshot().getRtcmInfo());
        Map<String, Object> landingRtcm = asMap(snapshot.getLandingDockSnapshot().getRtcmInfo());
        return String.format("droneSn=%s, takeoffDock=%s, landingDock=%s, droneCenter=%s, secretCodeLength=%s, takeoffLeaf=%s, landingLeaf=%s, takeoffRtcm=%s, landingRtcm=%s, takeoffHomeValid=%s, landingHomeValid=%s",
                snapshot.getDroneSn(),
                snapshot.getTakeoffDockSn(),
                snapshot.getLandingDockSn(),
                droneTopo == null ? null : droneTopo.get("center_node"),
                getListSize(droneTopo == null ? null : droneTopo.get("secret_code")),
                getFirstLeaf(takeoffTopo),
                getFirstLeaf(landingTopo),
                safeRtcmSummary(takeoffRtcm),
                safeRtcmSummary(landingRtcm),
                snapshot.getTakeoffDockSnapshot().getHomePositionIsValid(),
                snapshot.getLandingDockSnapshot().getHomePositionIsValid());
    }

    private Object getFirstLeaf(Map<String, Object> topo) {
        if (topo == null) {
            return null;
        }
        List<Map<String, Object>> leafNodes = (List<Map<String, Object>>) topo.get("leaf_nodes");
        return CollectionUtils.isEmpty(leafNodes) ? null : leafNodes.get(0);
    }

    private Integer getListSize(Object value) {
        return value instanceof List ? ((List<?>) value).size() : null;
    }

    private Map<String, Object> safeRtcmSummary(Map<String, Object> rtcmInfo) {
        if (rtcmInfo == null) {
            return null;
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("source_type", rtcmInfo.get("source_type"));
        summary.put("host", rtcmInfo.get("host"));
        summary.put("port", rtcmInfo.get("port"));
        summary.put("mount_point", rtcmInfo.get("mount_point"));
        return summary;
    }

    private String toSafeJson(FlighttaskExecuteRequest request) {
        try {
            Map<String, Object> payload = objectMapper.convertValue(request, MAP_TYPE);
            Map<String, Object> multiDockTask = asMap(payload.get("multi_dock_task"));
            if (multiDockTask != null) {
                Map<String, Object> topo = asMap(multiDockTask.get("wireless_link_topo"));
                if (topo != null && topo.get("secret_code") instanceof List) {
                    topo.put("secret_code", "length:" + ((List<?>) topo.get("secret_code")).size());
                    multiDockTask.put("wireless_link_topo", topo);
                }
                payload.put("multi_dock_task", multiDockTask);
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(request);
        }
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

}
