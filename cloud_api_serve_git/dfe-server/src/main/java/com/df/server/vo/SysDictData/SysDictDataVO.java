package com.df.server.vo.SysDictData;

import lombok.Data;

import java.io.Serializable;

/**
 * 返回值
 */
@Data
public class SysDictDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;
    /**
     * 字典排序
     */
    private Integer dictSort;
    /**
     * 字典标签
     */
    private String dictLabel;
    /**
     * 字典键值
     */
    private String dictValue;
    /**
     * 字典类型
     */
    private String dictType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 样式属性（其他样式扩展）
     */
    private String cssClass;
}
