package com.df.server.dto.UniDevice;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增入参
 */
@Data
public class UniDeviceAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
