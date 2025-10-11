package com.df.server.dto.UniArea;


import com.df.framework.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询入参
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UniAreaPageDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;
}
