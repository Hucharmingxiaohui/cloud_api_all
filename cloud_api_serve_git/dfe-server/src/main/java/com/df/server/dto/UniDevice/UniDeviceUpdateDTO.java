package com.df.server.dto.UniDevice;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新入参
 */
@Data
public class UniDeviceUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;
    /**
     * 设备ID
     */
    private String deviceId;
    /**
     * 设备名称
     */
    private String deviceName;
    /**
     * 设备类型(字典表device_type)
     */
    private Integer deviceType;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 所属间隔ID
     */
    private String bayId;
}
