package com.df.server.vo.UniPointThreshold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 点位告警阈值信息VO
 *
 * @author 辛英迪
 * @date 2022/8/18 15:36
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointAlarmThresholdVO implements Serializable {
    private static final long serialVersionUID = -1516298007572001035L;

    /**
     * 点位名称
     */
    private String pointName;

    /**
     * 最新巡视任务名称
     */
    private String taskName;

    /**
     * 最新巡视任务时间
     */
    private String taskTime;

    /**
     * 最新巡视结果
     */
    private String taskResult;

    /**
     * 结果描述
     */
    private String resultDes;


    /**
     * 判定规则 <1>: = 上限 <2>: = 下限 <3>: = 区间 <4>: = 状态
     */
    private Integer decisionRules;

    /**
     * 预警上限
     */
    private String warningUpperLimit;

    /**
     * 预警下限
     */
    private String warningLowerLimit;

    /**
     * 预警状态
     */
    private String warningState;

    /**
     * 一般上限
     */
    private String commonUpperLimit;

    /**
     * 一般下限
     */
    private String commonLowerLimit;

    /**
     * 一般状态
     */
    private String commonState;

    /**
     * 严重上限
     */
    private String seriousUpperLimit;

    /**
     * 严重下限
     */
    private String seriousLowerLimit;

    /**
     * 严重状态
     */
    private String seriousState;

    /**
     * 危急上限
     */
    private String criticalUpperLimit;

    /**
     * 危急下限
     */
    private String criticalLowerLimit;

    /**
     * 危急状态
     */
    private String criticalState;
}
