package com.df.server.dto.UniBay;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新入参
 */
@Data
public class UniBayUpdateDTO implements Serializable {

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
