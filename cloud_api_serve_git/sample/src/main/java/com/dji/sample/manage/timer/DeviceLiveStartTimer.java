package com.dji.sample.manage.timer;

import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.model.dto.LiveTypeDTO;
import com.dji.sample.manage.service.IDeviceService;
import com.dji.sample.manage.service.ILiveStreamService;
import com.dji.sdk.cloudapi.device.*;
import com.dji.sdk.cloudapi.livestream.UrlTypeEnum;
import com.dji.sdk.cloudapi.livestream.VideoQualityEnum;
import com.dji.sdk.cloudapi.livestream.VideoTypeEnum;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@Slf4j
public class DeviceLiveStartTimer {

    @Value("${server.base-url}")
    private String baseUrl;

    @Autowired
    private ILiveStreamService liveStreamService;

    @Autowired
    private IDeviceService deviceService;

    //  两套逻辑，机巢是每5分钟检测一次保证开启，无人机是mqtt上线的时候开启
    // 每5分钟执行一次
    @Scheduled(fixedRate = 300000) // 单位：毫秒
    public void checkAndStartLive() {
        log.info("开始执行设备状态检测...");
        try {
            PaginationData<DeviceDTO> boundDevicesWithDomain = deviceService.getBoundDevicesWithDomain("e3dea0f5-37f2-4d79-ae58-490af3228069", 1L, 10L, 3);
            List<DeviceDTO> list = boundDevicesWithDomain.getList();
            log.info("定时直播查询到绑定机巢数量: {}", list == null ? 0 : list.size());
            for (DeviceDTO device : list) {
                log.info("定时直播设备状态: dockSn={}, dockOnline={}, childSn={}, childOnline={}",
                        device.getDeviceSn(), device.getStatus(), device.getChildDeviceSn(),
                        device.getChildren() == null ? null : device.getChildren().getStatus());
                if(Boolean.TRUE.equals(device.getStatus())){
                    log.info("开启机巢直播---");
//                  开启机巢直播
                    LiveTypeDTO liveTypeDTO = new LiveTypeDTO();
                    liveTypeDTO.setUrlType(UrlTypeEnum.WHIP);
                    VideoId videoId = new VideoId();
                    videoId.setDroneSn(device.getDeviceSn());
                    PayloadIndex payloadIndex = new PayloadIndex();
                    payloadIndex.setType(DeviceTypeEnum.DOCK_CAMERA);
                    payloadIndex.setSubType(DeviceSubTypeEnum.ZERO);
                    payloadIndex.setPosition(PayloadPositionEnum.FPV);
                    videoId.setPayloadIndex(payloadIndex);
                    videoId.setVideoType(VideoTypeEnum.NORMAL);
                    liveTypeDTO.setVideoId(videoId);
                    liveTypeDTO.setVideoQuality(VideoQualityEnum.STANDARD_DEFINITION);
                    startLiveWithRecovery(liveTypeDTO, "机巢", device.getDeviceSn());
                }
                if (StringUtils.hasText(device.getChildDeviceSn())) {
                    DeviceDTO child = device.getChildren();
                    if (child != null && Boolean.TRUE.equals(child.getStatus()) && StringUtils.hasText(child.getDeviceSn())) {
                        log.info("开启无人机直播---");
//                      开启无人机直播
                        LiveTypeDTO liveTypeDTO = new LiveTypeDTO();
                        liveTypeDTO.setUrlType(UrlTypeEnum.WHIP);
                        VideoId videoId = new VideoId();
                        videoId.setDroneSn(child.getDeviceSn());
                        PayloadIndex payloadIndex = new PayloadIndex();
                        payloadIndex.setType(DeviceTypeEnum.M4TD_CAMERA);
                        payloadIndex.setSubType(DeviceSubTypeEnum.ZERO);
                        payloadIndex.setPosition(PayloadPositionEnum.FRONT_LEFT);
                        videoId.setPayloadIndex(payloadIndex);
                        videoId.setVideoType(VideoTypeEnum.NORMAL);
                        liveTypeDTO.setVideoId(videoId);
                        liveTypeDTO.setVideoQuality(VideoQualityEnum.STANDARD_DEFINITION);
                        startLiveWithRecovery(liveTypeDTO, "无人机", device.getChildDeviceSn());
                    }
                }

            }
        } catch (Exception e) {
            log.error("定时任务执行异常", e);
        }
    }

    private void startLiveWithRecovery(LiveTypeDTO liveTypeDTO, String sourceName, String sn) {
        HttpResultResponse result = liveStreamService.liveStart(liveTypeDTO);
        if (result.getCode() == 0) {
            log.info("{} {} 直播开启成功", sourceName, sn);
            return;
        }
        if (isLiveAlreadyStarted(result.getCode())) {
            log.warn("{} {} 直播状态已存在，执行停止后重启", sourceName, sn);
            restartLive(liveTypeDTO, sourceName, sn);
            return;
        }
        log.warn("{} {} 直播开启失败，code: {}, message: {}", sourceName, sn, result.getCode(), result.getMessage());
    }

    private boolean isLiveAlreadyStarted(Integer code) {
        return Integer.valueOf(513003).equals(code) || Integer.valueOf(13003).equals(code);
    }

    private void restartLive(LiveTypeDTO liveTypeDTO, String sourceName, String sn) {
        try {
            liveStreamService.liveStop(liveTypeDTO.getVideoId());
            Thread.sleep(2000);
            HttpResultResponse restartResult = liveStreamService.liveStart(liveTypeDTO);
            if (restartResult.getCode() == 0) {
                log.info("{} {} 直播重启成功", sourceName, sn);
            } else {
                log.warn("{} {} 直播重启失败，code: {}, message: {}", sourceName, sn,
                        restartResult.getCode(), restartResult.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("{} {} 直播重启异常", sourceName, sn, e);
        }
    }

}

