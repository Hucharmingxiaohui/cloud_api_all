package com.dji.sample.df.supControlDf.service;
//适配DJI dock2
public interface dock2VideoService {
    /**
     * 开启直播
     * String sn,设备sn,机场2或者无人机视频
     * String video_type视频类型 RTMP
     * String device_type1机场 2无人机
     */
    String live_start_push(String sn,String video_type,String device_type);
    /**
     * 停止直播
     * String sn,设备sn,机场2或者无人机视频
     * String device_type1机场 2无人机
     */
    String live_stop_push(String sn,String device_type);
    /**
     *  直播镜头切换
     * String sn,设备sn,机场2或者无人机视频
     * String device_type1机场 2无人机
     */
    String  live_lens_change(String sn,String video_type,String device_type);
    /**
     *  直播镜头切换
     * String sn,设备sn,机场2或者无人机视频
     * String device_type1机场 2无人机
     * String video_position 0内1 外
     */
    String live_camera_change(String sn,String video_position,String device_type);
    /**
     *  直播镜头切换
     * String sn,设备sn,机场2或者无人机视频
     * String device_type1机场 2无人机
     * String video_position 0内1 外
     */
    String live_set_quality(String sn,String video_type,String device_type);

}
