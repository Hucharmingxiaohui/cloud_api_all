package com.df.server.dto.UniDevice;


import com.df.framework.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询入参
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UniDevicePageDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;
}
