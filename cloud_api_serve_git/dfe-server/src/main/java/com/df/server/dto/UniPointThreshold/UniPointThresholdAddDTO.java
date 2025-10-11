package com.df.server.dto.UniPointThreshold;


import lombok.Data;

import java.io.Serializable;

/**
 * 新增入参
 */
@Data
public class UniPointThresholdAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 点位唯一编号
     */
    private String pointCode;
    /**
     *
     */
    private String subCode;
    /**
     * 判定规则 <1>: = 上限 <2>: = 下限 <3>: = 区间 <4>: = 状态  <5>: = 趋势变化量
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
