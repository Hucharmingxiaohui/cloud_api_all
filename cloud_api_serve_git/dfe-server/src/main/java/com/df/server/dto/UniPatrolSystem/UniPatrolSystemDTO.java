package com.df.server.dto.UniPatrolSystem;


import com.df.framework.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询入参
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UniPatrolSystemDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 变电站编码
     */
    private String subCode;

}
