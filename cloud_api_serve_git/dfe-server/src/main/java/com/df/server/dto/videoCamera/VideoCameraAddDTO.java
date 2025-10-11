package com.df.server.dto.videoCamera;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 新增入参
 */
@Data
public class VideoCameraAddDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 摄像机编码
     */
    private String code;
    /**
     * 摄像机名称
     */
    private String des;
    /**
     * 类型：字典表camera_type
     */
    private Integer type;
    /**
     * 通道号
     */
    private Integer channel;
    /**
     * 是否可见 0：不可见，1：可见
     */
    private Integer visible;
    /**
     * 所属设备 df_video_device.code
     */
    private String deviceCode;
    /**
     * 录像设备编码
     */
    private String cssName;
    /**
     * 录像设备通道
     */
    private String cssChan;
    /**
     * SIP
     */
    private String sipCode;
    /**
     * 解码插件标签  默认100
     */
    private Integer decodertag;
    /**
     * 插入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date insertTime;
    /**
     * rtspurl
     */
    private String rtspUrl;
}
