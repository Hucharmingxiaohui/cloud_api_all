package com.dji.sample.df.electricInspectionDf.service;

import com.dji.sample.df.electricInspectionDf.model.PubVideosAddressDfEntity;

import java.util.List;

public interface PubVideosAddressDfService {
    //添加视频地址
    boolean addVideosAddress(PubVideosAddressDfEntity pubVideosAddressDfEntity);
    //查询所有的视频地址
    List<PubVideosAddressDfEntity> getVideosList();
    //按场站编码查询视频地址
    List<PubVideosAddressDfEntity> getVideosBySubCode(String sub_code);
    //更新视频地址
    boolean updateVideosAddress(PubVideosAddressDfEntity pubVideosAddressDfEntity);
    //按id删除视频地址
    boolean deleteVideosById(int id);
    //根据设备sn查询视频信息
    PubVideosAddressDfEntity getVideoAddressByDeviceSn(String device_sn);
}
