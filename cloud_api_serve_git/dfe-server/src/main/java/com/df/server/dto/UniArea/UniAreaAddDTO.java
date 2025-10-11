package com.df.server.dto.UniArea;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增入参
 */
@Data
public class UniAreaAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 区域名称
     */
    private String areaName;
    /**
     * 变电站编码
     */
    private String subCode;
}
