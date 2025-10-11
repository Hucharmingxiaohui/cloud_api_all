package com.df.server.dto.UniTaskPlanItemPoint;


import com.df.framework.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询入参
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UniTaskPlanItemPointPageDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 方案编码
     */
    private String planNo;
    /**
     * 点位编码
     */
    private String pointCode;
    /**
     * 点位名称
     */
    private String pointName;
    /**
     * 采集保存文件类型，详见字典表类型save_type
     */
    private String saveTypeList;
    /**
     * 识别类型，详见字典表类型recognition_type
     */
    private String recognitionTypeList;
}
