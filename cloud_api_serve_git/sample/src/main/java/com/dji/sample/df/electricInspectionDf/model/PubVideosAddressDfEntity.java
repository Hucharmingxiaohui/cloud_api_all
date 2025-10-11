package com.dji.sample.df.electricInspectionDf.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
@Data
@TableName("pub_videos_address_df")
public class PubVideosAddressDfEntity implements Serializable {
    @TableId(type = IdType.AUTO) // MyBatis-Plus注解，指定主键字段及其生成策略
    private Integer id; // 主键id

    @TableField("video_name") // MyBatis-Plus注解，指定数据库字段名
    private String videoName; // 视频名称

    @TableField("sub_code") // MyBatis-Plus注解，指定数据库字段名
    private String subCode; // 场站id

    @TableField("workspace_id") // MyBatis-Plus注解，指定数据库字段名
    private String workspaceId; // 工作空间id

    @TableField("belonging_equipment") // MyBatis-Plus注解，指定数据库字段名
    private String belongingEquipment; // 所属设备（机场，无人机）

    @TableField("device_sn") // MyBatis-Plus注解，指定数据库字段名
    private String deviceSn; // 设备sn唯一标识码，有无人机和机场两类

    @TableField("camera_type") // MyBatis-Plus注解，指定数据库字段名
    private String cameraType; // 摄像头类型 0:机场 1:无人机

    @TableField("18_code") // MyBatis-Plus注解，指定数据库字段名
    private String code18; // 18位编码

    @TableField("rtmp_address") // MyBatis-Plus注解，指定数据库字段名
    private String rtmpAddress; // RTMP服务器视频流地址
}
