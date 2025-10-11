package com.df.server.entity.video;


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
 * 摄像头接口
 * <p>
 * Created by lianyc on 2025-02-11
 */
@Data
@TableName("df_video_camera")
public class VideoCameraEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId
    private Integer id;
    /**
     * 摄像头id
     */
    private String code;
    /**
     * 描述
     */
    private String des;
    /**
     * 通道号
     */
    private Integer channel;
    /**
     * 是否可见 0：不可见，1：可见
     */
    private Integer visible;
    /**
     * 类型0 ：字典表camera_type
     */
    private Integer type;
    /**
     * 所属设备 df_video_device.code
     */
    private String deviceCode;

    /**
     * 所属 变电站
     */
    private String subCode;
    /**
     * 在线状态 1：在线 0：离线
     */
    private Integer status;
    /**
     * 安装位置
     */
    private String location;
    /**
     * 插入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date insertTime;
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    /**
     * 录像服务器名称
     */
    private String cssName;
    /**
     * 录像服务器通道
     */
    private String cssChan;
    /**
     * 录像状态 1:可用 0：不可用
     */
    private Integer recStatus;
    /**
     * 显示顺序
     */
    private Integer displayOrder;
    /**
     * 机器人系统唯一编号，关联表df_uni_patrol_sys表sys_code
     */
    private String sysCode;
    /**
     * 解码插件标签
     */
    private Integer decodertag;
    /**
     * 是否关注
     */
    private Integer isfocus;
    /**
     *
     */
    private String rtspUrl;
    private String sipCode;
}
