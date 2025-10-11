package com.df.server.mapper.video;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.dto.videoCamera.VideoCameraExcel;
import com.df.server.dto.videoCamera.VideoCameraExportDTO;
import com.df.server.dto.videoCamera.VideoCameraPageDTO;
import com.df.server.entity.video.VideoCameraEntity;
import com.df.server.vo.VideoCamera.VideoCameraInfoVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 摄像头接口
 * <p>
 * Created by lianyc on 2025-02-11
 */
@Mapper
public interface VideoCameraMapper extends BaseMapper<VideoCameraEntity> {
    int countCamera(String subCode);

    //自定义分页查询
    List<VideoCameraInfoVO> getPageList(VideoCameraPageDTO pageDTO);

    Integer getPageTotal(VideoCameraPageDTO pageDTO);

    int countCameraByCameraCode(String camCode);

    VideoCameraEntity getByCode(String code);

    void insertCameraBatch(List<VideoCameraExcel> subList);

    List<VideoCameraExcel> exportCameras(VideoCameraExportDTO params);

    void clearCamera(String subCode);
}