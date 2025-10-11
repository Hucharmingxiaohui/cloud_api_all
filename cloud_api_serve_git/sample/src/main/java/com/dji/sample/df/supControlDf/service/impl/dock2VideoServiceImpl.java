package com.dji.sample.df.supControlDf.service.impl;

import com.dji.sample.manage.model.dto.LiveTypeDTO;
import com.dji.sample.manage.service.ILiveStreamService;
import com.dji.sample.df.supControlDf.entity.LiveChange;
import com.dji.sample.df.supControlDf.service.dock2VideoService;
import com.dji.sdk.cloudapi.device.*;
import com.dji.sdk.cloudapi.livestream.*;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.mqtt.services.ServicesPublish;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class dock2VideoServiceImpl implements dock2VideoService {
    @Autowired
    private ILiveStreamService liveStreamService;
    @Resource
    private ServicesPublish servicesPublish;

    private static final long DEFAULT_TIMEOUT = 20_000;
    /**
     * 开启直播
     * String sn,设备sn,机场2或者无人机视频
     * String video_type视频类型 1机场 2无人机
     */
    @Override
    public String live_start_push(String sn,String video_type,String device_type) {
        if(video_type.equals("rtmp"))
        {
            LiveTypeDTO liveTypeDTO=new LiveTypeDTO();
            //ulr类型
            liveTypeDTO.setUrlType(UrlTypeEnum.RTMP);
            //镜头类型
            liveTypeDTO.setVideoType(LensChangeVideoTypeEnum.WIDE);
            //清晰度
            liveTypeDTO.setVideoQuality(VideoQualityEnum.AUTO);
            VideoId videoId=new VideoId();
            //视频id
            if(device_type.equals("1"))
            {

                //设备sn
                videoId.setDroneSn(sn);
                //镜头
                PayloadIndex payloadIndex = null;
                //主类型
                payloadIndex.setType(DeviceTypeEnum.DOCK_CAMERA);
                //子类型
                payloadIndex.setSubType(DeviceSubTypeEnum.ZERO);
                //位置
                payloadIndex.setPosition(PayloadPositionEnum.FPV);
                videoId.setPayloadIndex(payloadIndex);
            }else
            {
                //设备sn
                videoId.setDroneSn(sn);
                //镜头
                PayloadIndex payloadIndex = null;
                //主类型
                payloadIndex.setType(DeviceTypeEnum.M3TD_CAMERA);
                //子类型
                payloadIndex.setSubType(DeviceSubTypeEnum.ZERO);
                //位置
                payloadIndex.setPosition(PayloadPositionEnum.FRONT_LEFT);
                videoId.setPayloadIndex(payloadIndex);

            }

            liveTypeDTO.setVideoId(videoId);
            HttpResultResponse response= liveStreamService.liveStart(liveTypeDTO);
            if(response.getCode()==0){
                return "ture";
            }else{
                return "false";
            }

        }


        return "false";
    }

    /**
     * 停止直播
     * String sn,设备sn,机场2或者无人机视频
     * String video_type视频类型 1机场 2无人机
     */
    @Override
    public String live_stop_push(String sn, String device_type) {
        VideoId videoId=new VideoId();
        //视频id
        if(device_type.equals("1"))
        {

            //设备sn
            videoId.setDroneSn(sn);
            //镜头
            PayloadIndex payloadIndex = null;
            //主类型
            payloadIndex.setType(DeviceTypeEnum.DOCK_CAMERA);
            //子类型
            payloadIndex.setSubType(DeviceSubTypeEnum.ZERO);
            //位置
            payloadIndex.setPosition(PayloadPositionEnum.FPV);
            videoId.setPayloadIndex(payloadIndex);
        }else
        {
            //设备sn
            videoId.setDroneSn(sn);
            //镜头
            PayloadIndex payloadIndex = null;
            //主类型
            payloadIndex.setType(DeviceTypeEnum.M3TD_CAMERA);
            //子类型
            payloadIndex.setSubType(DeviceSubTypeEnum.ZERO);
            //位置
            payloadIndex.setPosition(PayloadPositionEnum.FRONT_LEFT);
            videoId.setPayloadIndex(payloadIndex);

        }
        HttpResultResponse response= liveStreamService.liveStop(videoId);

        if(response.getCode()==0){
            return "ture";
        }else{
            return "false";
        }

    }
    /**
     * 镜头切换
     * String sn,设备sn,机场2或者无人机视频
     * String video_type视频类型 1机场 2无人机
     */

    @Override
    public String  live_lens_change(String sn,String video_type, String device_type) {

        if(video_type.equals("rtmp"))
        {
            LiveTypeDTO liveTypeDTO=new LiveTypeDTO();
            //ulr类型
            liveTypeDTO.setUrlType(UrlTypeEnum.RTMP);
            //镜头类型
            liveTypeDTO.setVideoType(LensChangeVideoTypeEnum.WIDE);
            //清晰度
            liveTypeDTO.setVideoQuality(VideoQualityEnum.AUTO);
            VideoId videoId=new VideoId();
            //视频id
            if(device_type.equals("1"))
            {

                //设备sn
                videoId.setDroneSn(sn);
                //镜头
                PayloadIndex payloadIndex = null;
                //主类型
                payloadIndex.setType(DeviceTypeEnum.DOCK_CAMERA);
                //子类型
                payloadIndex.setSubType(DeviceSubTypeEnum.ZERO);
                //位置
                payloadIndex.setPosition(PayloadPositionEnum.FPV);
                videoId.setPayloadIndex(payloadIndex);
            }else
            {
                //设备sn
                videoId.setDroneSn(sn);
                //镜头
                PayloadIndex payloadIndex = null;
                //主类型
                payloadIndex.setType(DeviceTypeEnum.M3TD_CAMERA);
                //子类型
                payloadIndex.setSubType(DeviceSubTypeEnum.ZERO);
                //位置
                payloadIndex.setPosition(PayloadPositionEnum.FRONT_LEFT);
                videoId.setPayloadIndex(payloadIndex);

            }

            liveTypeDTO.setVideoId(videoId);
            HttpResultResponse response= liveStreamService.liveLensChange(liveTypeDTO);
            if(response.getCode()==0){
                return "ture";
            }else{
                return "false";
            }

        }


        return "false";
    }
    /**
     *  直播镜头切换,仓内、仓外
     * String sn,设备sn,机场2或者无人机视频
     * String device_type1机场 2无人机
     */
    @Override
    public String live_camera_change(String sn, String video_position, String device_type) {
        LiveChange liveChange=new LiveChange();
        liveChange.setVideo_id("xxx");
        liveChange.setCamera_position(video_position);
        if(device_type.equals("1"))
        {
            servicesPublish.publish(new TypeReference<String>() {},
                    sn,
                   " live_camera_change",
                    liveChange
                    );
            return "true";
        }


        return "false";
    }

    @Override
    public String live_set_quality(String sn,String video_type,String device_type) {
        if(video_type.equals("rtmp"))
        {
            LiveTypeDTO liveTypeDTO=new LiveTypeDTO();
            //ulr类型
            liveTypeDTO.setUrlType(UrlTypeEnum.RTMP);
            //镜头类型
            liveTypeDTO.setVideoType(LensChangeVideoTypeEnum.WIDE);
            //清晰度
            liveTypeDTO.setVideoQuality(VideoQualityEnum.AUTO);
            VideoId videoId=new VideoId();
            //视频id
            if(device_type.equals("1"))
            {

                //设备sn
                videoId.setDroneSn(sn);
                //镜头
                PayloadIndex payloadIndex = null;
                //主类型
                payloadIndex.setType(DeviceTypeEnum.DOCK_CAMERA);
                //子类型
                payloadIndex.setSubType(DeviceSubTypeEnum.ZERO);
                //位置
                payloadIndex.setPosition(PayloadPositionEnum.FPV);
                videoId.setPayloadIndex(payloadIndex);
            }else
            {
                //设备sn
                videoId.setDroneSn(sn);
                //镜头
                PayloadIndex payloadIndex = null;
                //主类型
                payloadIndex.setType(DeviceTypeEnum.M3TD_CAMERA);
                //子类型
                payloadIndex.setSubType(DeviceSubTypeEnum.ZERO);
                //位置
                payloadIndex.setPosition(PayloadPositionEnum.FRONT_LEFT);
                videoId.setPayloadIndex(payloadIndex);
            }
            liveTypeDTO.setVideoId(videoId);
            HttpResultResponse response= liveStreamService.liveLensChange(liveTypeDTO);
            if(response.getCode()==0){
                return "ture";
            }else{
                return "false";
            }
        }
        return "false";
    }
}
