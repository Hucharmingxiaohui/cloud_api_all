package com.df.server.dto.UniArea;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新入参
 */
@Data
public class UniAreaUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;
    /**
     * 区域ID
     */
    private String areaId;
    /**
     * 区域名称
     */
    private String areaName;
    /**
     * 变电站编码
     */
    private String subCode;
}
