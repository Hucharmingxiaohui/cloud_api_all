package com.df.server.dto.HisUniTaskItemPoints;


import com.df.framework.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询入参
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HisUniTaskItemPointsPageDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 巡视任务执行ID
     */
    private String taskPatrolledId;
    /**
     * 所属区域ID
     */
    private String areaId;
    /**
     * 所属间隔ID
     */
    private String bayId;
    /**
     * 所属主设备ID
     */
    private String deviceId;
    /**
     * 所属部件ID
     */
    private String componentId;
    /**
     * 点位编码，对应标准规范device_id
     */
    private String pointCode;
    /**
     * 点位名称，对应标准规范device_name
     */
    private String pointName;
    /**
     * 0 失败 1正常 2判别异常
     */
    private Integer valid;
    /**
     * 是否告警 1是 0否
     */
    private Integer isAlarm;
    /**
     * 是否完成
     */
    private Integer isFinished;
    /**
     * 是否抓拍完成
     */
    private Integer isFinishedCapture;
}
