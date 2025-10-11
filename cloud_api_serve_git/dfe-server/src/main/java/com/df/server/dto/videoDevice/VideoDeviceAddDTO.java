package com.df.server.dto.videoDevice;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增入参
 */
@Data
public class VideoDeviceAddDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 设备编码
     */
    private String code;
    /**
     * 设备描述
     */
    private String des;
    /**
     * 视频设备类型，对应字典表video_device_type
     */
    private Integer type;
    /**
     * 所属变电站
     */
    private String subCode;
    /**
     * ip地址
     */
    private String ip;
    /**
     * 端口
     */
    private Integer port;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 插件类型
     */
    private String deviceType;
    /**
     * 设备配置附加信息
     * DFDH:ALARMMODE=1#DISABLEALARMSUBSCRIPTION=0#
     * DFHK:ALARMMODE=0#DISABLEALARMSUBSCRIPTION=0tcp_enable=1#
     * <p>
     * 参数说明：
     * <p>
     * ALARMMODE: 0  //可回调上传，报警期间持续发送信息，如HK
     * <p>
     * ALARMMODE:1  //可回调上传，报警发生或消失时发送信息，如DH
     * DISABLEALARMSUBSCRIPTION:是否禁用设备告警订阅,默认0或不配置此参数
     * tcp_enable: 连接设备或下级平台取流方式，默认0，udp取流， 1-tcp取流
     */
    private String extraInfo;
    /**
     * SIP
     */
    private String sipCode;

}
