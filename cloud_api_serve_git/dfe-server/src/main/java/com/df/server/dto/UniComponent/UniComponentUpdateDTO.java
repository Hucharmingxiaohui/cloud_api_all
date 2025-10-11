package com.df.server.dto.UniComponent;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新入参
 */
@Data
public class UniComponentUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;
    /**
     * 部件ID
     */
    private String componentId;
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
