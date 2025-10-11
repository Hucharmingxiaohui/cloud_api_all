package com.df.server.entity.uni;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 巡检系统接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
@Data
@TableName("df_uni_patrol_system")
public class UniPatrolSystemEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
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
    //private Integer isAnalyse;
    /**
     * 记录创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 记录更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

}
