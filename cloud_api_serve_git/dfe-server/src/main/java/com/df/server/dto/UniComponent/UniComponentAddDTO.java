package com.df.server.dto.UniComponent;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增入参
 */
@Data
public class UniComponentAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备名称
     */
    private String componentName;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 设备ID
     */
    private String deviceId;
}
