package com.df.server.vo.UniBay;

import lombok.Data;

import java.io.Serializable;

/**
 * 返回值
 */
@Data
public class UniBayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;
    /**
     * 间隔ID
     */
    private String bayId;
    /**
     * 间隔名称
     */
    private String bayName;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 所属区域ID
     */
    private String areaId;
}
