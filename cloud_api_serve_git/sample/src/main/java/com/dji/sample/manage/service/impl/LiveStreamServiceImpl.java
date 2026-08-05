package com.dji.sample.manage.service.impl;

import com.dji.sample.manage.model.dto.*;
import com.dji.sample.manage.model.param.DeviceQueryParam;
import com.dji.sample.manage.service.*;
import com.dji.sdk.cloudapi.device.DeviceDomainEnum;
import com.dji.sdk.cloudapi.device.VideoId;
import com.dji.sdk.cloudapi.livestream.*;
import com.dji.sdk.cloudapi.livestream.api.AbstractLivestreamService;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.SDKManager;
import com.dji.sdk.mqtt.services.ServicesReplyData;
import com.dji.sdk.mqtt.services.TopicServicesResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author sean.zhou
 * @date 2021/11/22
 * @version 0.1
 */
@Service
@Transactional
@Slf4j
public class LiveStreamServiceImpl implements ILiveStreamService {

    @Autowired
    private ICapacityCameraService capacityCameraService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private IDeviceRedisService deviceRedisService;

    @Autowired
    private AbstractLivestreamService abstractLivestreamService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<CapacityDeviceDTO> getLiveCapacity(String workspaceId) {

        // Query all devices in this workspace.
        List<DeviceDTO> devicesList = deviceService.getDevicesByParams(
                DeviceQueryParam.builder()
                        .workspaceId(workspaceId)
                        .domains(List.of(DeviceDomainEnum.DRONE.getDomain(), DeviceDomainEnum.DOCK.getDomain()))
                        .build());

        // Query the live capability of each drone.
        return devicesList.stream()
                .filter(device -> deviceRedisService.checkDeviceOnline(device.getDeviceSn()))
                .map(device -> CapacityDeviceDTO.builder()
                        .name(Objects.requireNonNullElse(device.getNickname(), device.getDeviceName()))
                        .sn(device.getDeviceSn())
                        .camerasList(capacityCameraService.getCapacityCameraByDeviceSn(device.getDeviceSn()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public HttpResultResponse liveStart(LiveTypeDTO liveParam) {
        // Check if this lens is available live.
        HttpResultResponse<DeviceDTO> responseResult = this.checkBeforeLive(liveParam.getVideoId());
        if (HttpResultResponse.CODE_SUCCESS != responseResult.getCode()) {
            return responseResult;
        }

        ILivestreamUrl url = LiveStreamProperty.get(liveParam.getUrlType());
        url = setExt(liveParam.getUrlType(), url, liveParam.getVideoId());
        log.info("Start livestream push. urlType={}, url={}, videoId={}",
                liveParam.getUrlType(), url, liveParam.getVideoId());

        SrsStreamStatus streamStatus = getSrsStreamStatus(liveParam.getVideoId());
        if (streamStatus.healthy) {
            log.info("SRS stream is already active, skip live_start_push. videoId={}", liveParam.getVideoId());
            return HttpResultResponse.success(buildLiveResponse(liveParam.getUrlType(), url));
        }
        if (streamStatus.exists) {
            log.warn("SRS stream exists but unhealthy, stop stale livestream before restart. videoId={}", liveParam.getVideoId());
            deleteSrsClient(streamStatus.cid);
            liveStop(liveParam.getVideoId());
            sleepQuietly(2000);
        }

        TopicServicesResponse<ServicesReplyData<String>> response = abstractLivestreamService.liveStartPush(
                SDKManager.getDeviceSDK(responseResult.getData().getDeviceSn()),
                new LiveStartPushRequest()
                        .setUrl(url)
                        .setUrlType(liveParam.getUrlType())
                        .setVideoId(liveParam.getVideoId())
                        .setVideoQuality(liveParam.getVideoQuality()));

        if (!response.getData().getResult().isSuccess()) {
            return HttpResultResponse.error(response.getData().getResult());
        }

        return HttpResultResponse.success(buildLiveResponse(liveParam.getUrlType(), url));
    }

    private LiveDTO buildLiveResponse(UrlTypeEnum urlType, ILivestreamUrl url) {
        LiveDTO live = new LiveDTO();
        switch (urlType) {
            case AGORA:
                break;
            case RTMP:
                live.setUrl(url.toString().replace("rtmp", "webrtc"));
                break;
            case GB28181:
                LivestreamGb28181Url gb28181 = (LivestreamGb28181Url) url;
                live.setUrl(new StringBuilder()
                        .append("webrtc://")
                        .append(gb28181.getServerIP())
                        .append("/live/")
                        .append(gb28181.getAgentID())
                        .append("@")
                        .append(gb28181.getChannel())
                        .toString());
                break;
            case RTSP:
                live.setUrl(url.toString());
                break;
            case WHIP:
                live.setUrl(url.toString().replace("whip", "whep"));
                break;
            default:
                throw new IllegalArgumentException("Unsupported live url type: " + urlType);
        }
        return live;
    }

    @Override
    public HttpResultResponse liveStop(VideoId videoId) {
        HttpResultResponse<DeviceDTO> responseResult = this.checkBeforeLive(videoId);
        if (HttpResultResponse.CODE_SUCCESS != responseResult.getCode()) {
            return responseResult;
        }

        TopicServicesResponse<ServicesReplyData> response = abstractLivestreamService.liveStopPush(
                SDKManager.getDeviceSDK(responseResult.getData().getDeviceSn()), new LiveStopPushRequest()
                        .setVideoId(videoId));
        if (!response.getData().getResult().isSuccess()) {
            return HttpResultResponse.error(response.getData().getResult());
        }

        return HttpResultResponse.success();
    }

    @Override
    public HttpResultResponse liveSetQuality(LiveTypeDTO liveParam) {
        HttpResultResponse<DeviceDTO> responseResult = this.checkBeforeLive(liveParam.getVideoId());
        if (responseResult.getCode() != 0) {
            return responseResult;
        }

        TopicServicesResponse<ServicesReplyData> response = abstractLivestreamService.liveSetQuality(
                SDKManager.getDeviceSDK(responseResult.getData().getDeviceSn()), new LiveSetQualityRequest()
                        .setVideoQuality(liveParam.getVideoQuality())
                        .setVideoId(liveParam.getVideoId()));
        if (!response.getData().getResult().isSuccess()) {
            return HttpResultResponse.error(response.getData().getResult());
        }

        return HttpResultResponse.success();
    }

    @Override
    public HttpResultResponse liveLensChange(LiveTypeDTO liveParam) {
        HttpResultResponse<DeviceDTO> responseResult = this.checkBeforeLive(liveParam.getVideoId());
        if (HttpResultResponse.CODE_SUCCESS != responseResult.getCode()) {
            return responseResult;
        }

        TopicServicesResponse<ServicesReplyData> response = abstractLivestreamService.liveLensChange(
                SDKManager.getDeviceSDK(responseResult.getData().getDeviceSn()), new LiveLensChangeRequest()
                        .setVideoType(liveParam.getVideoType())
                        .setVideoId(liveParam.getVideoId()));

        if (!response.getData().getResult().isSuccess()) {
            return HttpResultResponse.error(response.getData().getResult());
        }

        return HttpResultResponse.success();
    }

    /**
     * Check if this lens is available live.
     * @param videoId
     * @return
     */
    private HttpResultResponse<DeviceDTO> checkBeforeLive(VideoId videoId) {
        if (Objects.isNull(videoId)) {
            return HttpResultResponse.error(LiveErrorCodeEnum.ERROR_PARAMETERS);
        }

        Optional<DeviceDTO> deviceOpt = deviceService.getDeviceBySn(videoId.getDroneSn());
        // Check if the gateway device connected to this drone exists
        if (deviceOpt.isEmpty()) {
            return HttpResultResponse.error(LiveErrorCodeEnum.NO_AIRCRAFT);
        }

        if (DeviceDomainEnum.DOCK == deviceOpt.get().getDomain()) {
            return HttpResultResponse.success(deviceOpt.get());
        }
        List<DeviceDTO> gatewayList = deviceService.getDevicesByParams(
                DeviceQueryParam.builder()
                        .childSn(videoId.getDroneSn())
                        .build());
        if (gatewayList.isEmpty()) {
            return HttpResultResponse.error(LiveErrorCodeEnum.NO_FLIGHT_CONTROL);
        }

        return HttpResultResponse.success(gatewayList.get(0));
    }

    /**
     * This is business-customized logic and is only used for testing.
     * @param type
     * @param url
     * @param videoId
     */
    private ILivestreamUrl setExt(UrlTypeEnum type, ILivestreamUrl url, VideoId videoId) {
        switch (type) {
            case AGORA:
                LivestreamAgoraUrl agoraUrl = (LivestreamAgoraUrl) url.clone();
                return agoraUrl.setSn(videoId.getDroneSn());
            case RTMP:
                LivestreamRtmpUrl rtmpUrl = (LivestreamRtmpUrl) url.clone();
                return rtmpUrl.setUrl(rtmpUrl.getUrl() + videoId.getDroneSn() + "-" + videoId.getPayloadIndex().toString());
            case GB28181:
                String random = String.valueOf(Math.abs(videoId.getDroneSn().hashCode()) % 1000);
                LivestreamGb28181Url gbUrl = (LivestreamGb28181Url) url.clone();
                gbUrl.setAgentID(gbUrl.getAgentID().substring(0, 20 - random.length()) + random);
                String deviceType = String.valueOf(videoId.getPayloadIndex().getType().getType());
                return gbUrl.setChannel(gbUrl.getChannel().substring(0, 20 - deviceType.length()) + deviceType);
            case WHIP:
                LivestreamWhipUrl whipUrl = (LivestreamWhipUrl) url.clone();
                return whipUrl.setUrl(whipUrl.getUrl() + videoId.getDroneSn() + "-" + videoId.getPayloadIndex().toString());
        }
        return url;
    }

    private SrsStreamStatus getSrsStreamStatus(VideoId videoId) {
        try {
            String api = getSrsStreamsApiUrl();
            if (Objects.isNull(api)) {
                return SrsStreamStatus.missing();
            }
            HttpURLConnection connection = (HttpURLConnection) new URL(api).openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return SrsStreamStatus.missing();
            }
            JsonNode root = objectMapper.readTree(connection.getInputStream());
            JsonNode streams = root.get("streams");
            if (Objects.isNull(streams) || !streams.isArray()) {
                return SrsStreamStatus.missing();
            }
            String streamName = buildStreamName(videoId);
            for (JsonNode stream : streams) {
                JsonNode name = stream.get("name");
                if (Objects.isNull(name) || (!streamName.equals(name.asText()) && !(streamName + ".flv").equals(name.asText()))) {
                    continue;
                }
                JsonNode active = stream.path("publish").get("active");
                JsonNode video = stream.get("video");
                JsonNode frames = stream.get("frames");
                JsonNode recvKbps = stream.path("kbps").get("recv_30s");
                JsonNode cid = stream.path("publish").get("cid");
                boolean healthy = Objects.nonNull(active) && active.asBoolean(false)
                        && Objects.nonNull(video) && !video.isNull()
                        && ((Objects.nonNull(frames) && frames.asLong(0) > 0)
                        || (Objects.nonNull(recvKbps) && recvKbps.asLong(0) > 0));
                return new SrsStreamStatus(true, healthy, Objects.nonNull(cid) ? cid.asText() : null);
            }
        } catch (IOException e) {
            log.debug("Failed to check SRS stream status. videoId={}", videoId, e);
        }
        return SrsStreamStatus.missing();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void deleteSrsClient(String cid) {
        if (!StringUtils.hasText(cid)) {
            return;
        }
        try {
            String api = getSrsStreamsApiUrl();
            if (Objects.isNull(api)) {
                return;
            }
            String clientsApi = api.replace("/api/v1/streams/", "/api/v1/clients/") + cid;
            HttpURLConnection connection = (HttpURLConnection) new URL(clientsApi).openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setRequestMethod("DELETE");
            log.warn("Deleted unhealthy SRS publisher. cid={}, response={}", cid, connection.getResponseCode());
        } catch (IOException e) {
            log.warn("Failed to delete unhealthy SRS publisher. cid={}", cid, e);
        }
    }

    private String getSrsStreamsApiUrl() {
        ILivestreamUrl whip = LiveStreamProperty.get(UrlTypeEnum.WHIP);
        if (!(whip instanceof LivestreamWhipUrl)) {
            return null;
        }
        String whipUrl = whip.toString();
        int rtcIndex = whipUrl.indexOf("/rtc/");
        if (rtcIndex < 0) {
            return null;
        }
        return whipUrl.substring(0, rtcIndex) + "/api/v1/streams/";
    }

    private String buildStreamName(VideoId videoId) {
        return videoId.getDroneSn() + "-" + videoId.getPayloadIndex();
    }

    private static class SrsStreamStatus {
        private final boolean exists;
        private final boolean healthy;
        private final String cid;

        private SrsStreamStatus(boolean exists, boolean healthy, String cid) {
            this.exists = exists;
            this.healthy = healthy;
            this.cid = cid;
        }

        private static SrsStreamStatus missing() {
            return new SrsStreamStatus(false, false, null);
        }
    }
}
