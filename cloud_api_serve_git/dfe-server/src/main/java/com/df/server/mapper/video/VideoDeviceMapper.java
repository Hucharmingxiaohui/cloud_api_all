package com.df.server.mapper.video;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.framework.vo.Tree;
import com.df.server.dto.videoDevice.VideoDeviceAddDTO;
import com.df.server.dto.videoDevice.VideoDeviceExportDTO;
import com.df.server.dto.videoDevice.VideoDevicePageDTO;
import com.df.server.entity.video.VideoDeviceEntity;
import com.df.server.vo.VideoDevice.VideoDeviceExcel;
import com.df.server.vo.VideoDevice.VideoDeviceVO;
import com.df.server.vo.VideoDevice.VideoSipServiceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备接口
 * <p>
 * Created by lianyc on 2025-04-01
 */
@Mapper
public interface VideoDeviceMapper extends BaseMapper<VideoDeviceEntity> {
    //自定义分页查询
    List<VideoDeviceVO> getPageList(VideoDevicePageDTO pageDTO);

    Integer getPageTotal(VideoDevicePageDTO pageDTO);

    List<VideoSipServiceVO> listSips();

    void insertNewDeice(VideoDeviceAddDTO dto);

    int countByCode(String code);

    void updateCameraDeviceCode(@Param("oldCode") String oldCode,
                                @Param("newCode") String newCode);

    void deleteCamerasByDeviceCode(String deviceCode);

    /**
     * 批量导入
     *
     * @param subList
     */
    void insertDeviceBatch(List<VideoDeviceExcel> subList);

    List<VideoDeviceExcel> exportDevices(VideoDeviceExportDTO params);

    List<Tree> devicesTree();

    void clearDevice(String subCode);
}