package com.df.server.vo.VideoCamera;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 返回值
 */
@Data
public class VideoCameraVO implements Serializable {

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
     *
     */
    private String deviceParent;
    /**
     * 在线状态 1：在线 0：离线
     */
    private Integer status;
    /**
     * 纬度
     */
    private Double lat;
    /**
     * 经度
     */
    private Double lng;
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
     *
     */
    private String sipCode;
    /**
     *
     */
    private String sipiauCode;
    /**
     * 当视频设备为机器人上挂载的，存储机器人编码，否则为空
     */
    private String robotCode;
    /**
     * 解码插件标签
     */
    private Integer decodertag;
    /**
     * 是否关注
     */
    private Integer isfocus;
    /**
     * df_uni_mapfile表主键
     */
    private Integer mapfileId;
    /**
     * 设备在地图上的坐标，格式：（x，y，z）
     */
    private String mapPoint;
    /**
     * 插入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date insertTime;
    /**
     * 机器人系统唯一编号，关联表df_uni_patrol_sys表sys_code
     */
    private String sysCode;
    /**
     *
     */
    private String rtspUrl;
}
