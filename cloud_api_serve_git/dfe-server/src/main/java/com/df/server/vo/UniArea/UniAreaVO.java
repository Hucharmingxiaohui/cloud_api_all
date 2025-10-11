package com.df.server.vo.UniArea;

import lombok.Data;

import java.io.Serializable;

/**
 * 返回值
 */
@Data
public class UniAreaVO implements Serializable {

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
