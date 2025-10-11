package com.df.server.vo.UniTaskPlanItemPoint;


import lombok.Data;

import java.io.Serializable;

/**
 * 返回值
 */
@Data
public class UniTaskPlanItemPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private Integer id;
    /**
     *
     */
    private String subCode;
    /**
     * 任务计划唯一编号
     */
    private String planNo;
    /**
     * 点位类型，1摄像机 2机器人 3摄像机+机器人 4无人机 5摄像机+无人机 8声纹 16在线监测
     */
    private Integer dataType;
    /**
     * 采集保存文件类型，详见字典表类型save_type 与df_uni_point保持一致
     */
    private Integer fileType;
    /**
     * 关联df_uni_point
     */
    private String pointCode;
    /**
     *
     */
    private Integer robotPos;
    /**
     *
     */
    private String robotCode;
    /**
     * 点位名称，对应标准规范device_name
     */
    private String pointName;
    /**
     * 区域名称
     */
    private String areaName;
    /**
     * 间隔名称
     */
    private String bayName;
    /**
     * 主设备名称
     */
    private String deviceName;
    /**
     * 部件名称
     */
    private String componentName;

    /**
     * 识别类型，详见字典表类型recognition_type
     */
    private String recognitionTypeList;
    /**
     * 采集保存文件类型描述
     */
    private String fileTypeName;
    /**
     * 识别类型描述
     */
    private String recognitionTypeName;
}
