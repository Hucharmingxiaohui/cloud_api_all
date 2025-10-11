package com.df.server.dto.videoCamera;

import com.df.framework.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询入参
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VideoCameraPageDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 变电站编码
     */
    private String subCode;
    /**
     *
     */
    private Integer status;
    /**
     * 相机名字
     */
    private String cameraName;

    private String deviceCode;

    private Integer type;
}
