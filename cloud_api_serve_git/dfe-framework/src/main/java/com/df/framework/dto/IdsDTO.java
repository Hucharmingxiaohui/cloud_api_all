package com.df.framework.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 只有id的入参，公共类
 */
@Data
public class IdsDTO implements Serializable {
    private static final long serialVersionUID = 2067967778873739751L;
    /**
     * id主键
     */
    private String ids;
}
