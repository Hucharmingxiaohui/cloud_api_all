package com.dji.sample.manage.timer;

import com.alibaba.fastjson.JSONObject;
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

import java.net.URI;
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
            for (DeviceDTO device : list) {
                if(device.getStatus()){
                    log.info("开启机巢直播---");
//                  开启机巢直播
                    JSONObject jsonObject = new JSONObject();
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
                    HttpResultResponse httpResultResponse = liveStreamService.liveStart(liveTypeDTO);
                }
//                if(device.getChildren().getStatus()){
                    log.info("开启无人机直播---");
//                  开启无人机直播
                    JSONObject jsonObject = new JSONObject();
                    LiveTypeDTO liveTypeDTO = new LiveTypeDTO();
                    liveTypeDTO.setUrlType(UrlTypeEnum.WHIP);
                    VideoId videoId = new VideoId();
                    videoId.setDroneSn(device.getChildDeviceSn());
                    PayloadIndex payloadIndex = new PayloadIndex();
                    payloadIndex.setType(DeviceTypeEnum.M4TD_CAMERA);
                    payloadIndex.setSubType(DeviceSubTypeEnum.ZERO);
                    payloadIndex.setPosition(PayloadPositionEnum.FRONT_LEFT);
                    videoId.setPayloadIndex(payloadIndex);
                    videoId.setVideoType(VideoTypeEnum.NORMAL);
                    liveTypeDTO.setVideoId(videoId);
                    liveTypeDTO.setVideoQuality(VideoQualityEnum.STANDARD_DEFINITION);
                    HttpResultResponse httpResultResponse = liveStreamService.liveStart(liveTypeDTO);
//                }

            }
        } catch (Exception e) {
            log.error("定时任务执行异常", e);
        }
    }

}
