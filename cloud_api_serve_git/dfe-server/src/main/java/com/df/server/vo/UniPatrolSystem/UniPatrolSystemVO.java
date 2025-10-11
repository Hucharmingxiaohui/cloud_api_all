package com.df.server.vo.UniPatrolSystem;

import lombok.Data;

import java.io.Serializable;

/**
 * 返回值
 */
@Data
public class UniPatrolSystemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 巡视系统编码
     */
    private String sysCode;
    /**
     * 巡视系统名称
     */
    private String sysName;
    /**
     * 巡视系统类型(字典表patrol_sys_type)：
     * center主站，
     * host巡视主机，
     * edge边缘节点，
     * video视频系统，
     * robot机器人系统，
     * uav无人机系统
     */
    private String sysType;
    /**
     * 巡视系统IP
     */
    private String sysIp;
    /**
     * 作为服务端的端口
     */
    private Integer port;
    /**
     * 通信协议版本：2022、2021(字段暂时不用)
     */
    private String version;
    /**
     * FTP端口
     */
    private Integer ftpPort;
    /**
     * FTP用户名
     */
    private String ftpUser;
    /**
     * FTP密码
     */
    private String ftpPassword;
    /**
     * FTP是否隐式：0显示，1隐式
     */
    private Integer ftpImplicit;
    /**
     * 作为服务端，要求客户端发送的心跳间隔(单位秒)
     */
    private Integer heartBeatInterval;
    /**
     * 作为服务端，要求客户端巡视设备运行数据上报间隔(单位秒)
     */
    private Integer patroldeviceRunInterval;
    /**
     * 作为服务端，要求客户端无人机机巢运行数据上报(单位秒)
     */
    private Integer nestRunInterval;
    /**
     * 作为服务端，要求客户端环境信息上报间隔(单位秒)
     */
    private Integer weatherInterval;
    /**
     * 下级上传的巡视结果是否需要二次分析：0否，1是 (对robot机器人系统，uav无人机系统 无效)
     */
    private Integer isAnalyse;
    /**
     * 对应变电站名字
     */
    private String subName;
}
