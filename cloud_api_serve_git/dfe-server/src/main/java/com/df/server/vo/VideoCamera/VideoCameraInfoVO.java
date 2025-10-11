package com.df.server.vo.VideoCamera;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * Created by lianyc on 2022-11-17
 */
@Data
public class VideoCameraInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    private Integer id;
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
     * 在线状态 1：在线 0：离线
     */
    private Integer status;
    /**
     * 录像服务器名称
     */
    private String cssName;
    /**
     * 录像服务器通道
     */
    private String cssChan;
    /**
     * 录像状态 1:录像 0：未录像
     */
    private Integer recStatus;
    /**
     * 显示顺序
     */
    private Integer displayOrder;
    /**
     * SIP
     */
    private String sipCode;
    /**
     * 解码插件标签
     */
    private Integer decodertag;
    /**
     * 插入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date insertTime;
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    /**
     * 变电站编码
     */
    private String subName;
    /**
     * 视频设备名称
     */
    private String deviceName;
    /**
     * rtspurl
     */
    private String rtspUrl;

}
