package com.df.framework.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;


/**
 * 分页查询入参
 *
 * @author Administrator
 */
@Data
public class PageDTO implements Serializable {
    private static final long serialVersionUID = 5291971388080036886L;
    /**
     * 当前页数
     */
    private Integer page;
    /**
     * 每页个数
     */
    private Integer size;

    @JsonIgnore
    private Integer start;
}
