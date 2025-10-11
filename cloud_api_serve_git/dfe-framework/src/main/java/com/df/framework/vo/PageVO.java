package com.df.framework.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回数据
 *
 * @author lyc
 **/
@Data
public class PageVO<T> implements Serializable {
    private static final long serialVersionUID = -1684537217442501701L;
    /**
     * 数据结果
     */
    private List<T> list;
    /**
     * 总个数
     */
    private int total;
    /**
     * 总页数
     */
    private int pages;
    /**
     * 当前页数
     */
    private int page;
    /**
     * 每页个数
     */
    private int size;
}