package com.df.server.dto.uniPoint;


import lombok.Data;

import java.io.Serializable;

/**
 * 更新入参
 */
@Data
public class UniPointUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private Integer id;
    /**
     * 点位编码，对应标准规范的device_id
     */
    private String pointCode;
    /**
     * 点位名称，对应标准规范device_name
     */
    private String pointName;
    /**
     * 点位类型，1摄像机 2机器人 3摄像机+机器人 4无人机 5摄像机+无人机 8声纹 16在线监测
     */
    private Integer pointType;
    /**
     * 所属变电站编码
     */
    private String subCode;
    /**
     * 所属巡视系统编码，表df_uni_patrol_sys
     */
    private String sysCode;
    /**
     * 区域id，表df_uni_area
     */
    private String areaId;
    /**
     * 区域名称
     */
    private String areaName;
    /**
     * 间隔id，表df_uni_bay
     */
    private String bayId;
    /**
     * 间隔名称
     */
    private String bayName;
    /**
     * 主设备id，表df_uni_device
     */
    private String deviceId;
    /**
     * 主设备名称
     */
    private String deviceName;
    /**
     * 部件id，表df_uni_component
     */
    private String componentId;
    /**
     * 部件名称
     */
    private String componentName;
    /**
     * 实物ID
     */
    private String materialId;
    /**
     * 主设备类型，详见字典表类型device_type
     */
    private Integer deviceType;
    /**
     * 表计类型 ，详见字典表类型meter_type
     */
    private Integer meterType;
    /**
     * 外观类型，详见字典表类型appearance_type
     */
    private Integer appearanceType;
    /**
     * 采集保存文件类型，详见字典表类型save_type
     */
    private String saveTypeList;
    /**
     * 识别类型，详见字典表类型recognition_type
     */
    private String recognitionTypeList;
    /**
     * 相位，详见字典表类型phase
     */
    private String phase;
    /**
     * 点位描述
     */
    private String pointDes;
    /**
     * 点位关联的地图id，表df_uni_mapfile主键
     */
    private Integer mapfileId;
    /**
     * 点位在地图上的坐标，格式x，y，z
     */
    private String mapPos;
    /**
     * 点位正常范围下限
     */
    private String lowerValue;
    /**
     * 点位正常范围上限
     */
    private String upperValue;
    /**
     * 关联视频编码及预置位
     */
    private String videoPos;
    /**
     * 是否加入白名单 0否 1是
     */
    private Integer isWhitelist;
    /**
     * 点位关注，0未关注 1关注
     */
    private Integer isfocus;
    /**
     * 巡视任务类型，详见字典表类型task_type
     */
    private String taskType;
    /**
     * 所属巡视任务子类型，详见字典表类型task_sub_type_3，task_sub_type_4，task_sub_type_5
     */
    private String taskSubType;
    /**
     * 是否实物ID识别点位，0否 1是
     */
    private Integer isObj;
    /**
     * 重要等级，详见字典表类型patrol_point_level
     */
    private Integer level;
    /**
     * 智能分析大类，详见字典表类型point_analyse_category
     */
    private Integer pointAnalyseCategory;
    /**
     * 智能分析子类，多个用英文逗号分割，详见字典表类型point_analyse_type、qxsb_type、point_analyse_type_pb
     */
    private String pointAnalyseType;
    /**
     * 判别基准图，当配置为判别类型时需要有值
     */
    private String baseImagePath;
    /**
     * 是否有非同源冗余，0否 1是，只要重要等级I类点位都是，II类自己定
     */
    private Integer isRedundancy;
    /**
     * 机器狗预置位号
     */
    private String robotPos;
}
