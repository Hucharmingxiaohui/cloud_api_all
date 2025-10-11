package com.df.server.dto.videoDevice;


import com.df.framework.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询入参
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VideoDevicePageDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String subCode;
    /**
     * 描述
     */
    private String deviceName;
    /**
     * 状态 0：不可用 1：可用
     */
    private String status;

    private Integer type;
}
