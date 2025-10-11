package com.df.server.vo.UniComponent;

import lombok.Data;

import java.io.Serializable;

/**
 * 返回值
 */
@Data
public class UniComponentVO implements Serializable {

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
