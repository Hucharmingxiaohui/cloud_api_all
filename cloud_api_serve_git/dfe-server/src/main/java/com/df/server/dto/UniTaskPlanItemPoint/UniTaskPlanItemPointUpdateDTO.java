package com.df.server.dto.UniTaskPlanItemPoint;


import lombok.Data;

import java.io.Serializable;

/**
 * 更新入参
 */
@Data
public class UniTaskPlanItemPointUpdateDTO implements Serializable {

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
     * 采集保存文件类型，详见字典表类型save_type 与df_uni_point保持一致
     */
    private Integer dataType;
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
}
