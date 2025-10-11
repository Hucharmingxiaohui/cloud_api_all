package com.df.framework.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 只有id的入参，公共类
 */
@Data
public class IdDTO implements Serializable {
    private static final long serialVersionUID = 8037293905333475722L;
    /**
     * id主键
     */
    private Long id;
}
