package com.df.server.dto.videoCamera;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增入参
 */
@Data
public class VideoCameraExportDTO implements Serializable {
    private static final long serialVersionUID = 96805974138170341L;
    /**
     * 设备描述
     */
    private String cameraName;
    /**
     * 视频设备类型，对应字典表video_device_type
     */
    private Integer type;
    /**
     * 所属变电站
     */
    private String subCode;
    /**
     *
     */
    private String deviceCode;

    /**
     * 状态 0：不可用 1：可用
     */
    private Integer status;

}
