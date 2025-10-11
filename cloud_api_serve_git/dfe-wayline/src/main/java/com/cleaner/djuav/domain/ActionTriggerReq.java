package com.cleaner.djuav.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:Cleaner
 * Date: 2024/12/22 10:46
 **/
@Data
public class ActionTriggerReq implements Serializable {

    /**
     * 动作触发器类型
     */
    private String actionTriggerType;

    /**
     * 动作触发器参数
     */
    private Double actionTriggerParam;


}
