package com.dji.sample.df.solar.model.entity;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 光伏区域正射图实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("manage_orthophoto")
public class OrthophotoEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 正射图名称
     */
    @TableField("name")
    private String name;

    /**
     * 正射图路径
     */
    @TableField("path")
    private String path;

    /**
     * 光伏板数量
     */
    @TableField(exist = false)
    private Integer solarPanelTotal;

    /**
     * 光伏组件数量
     */
    @TableField(exist = false)
    private Integer componentTotal;

    /**
     * 光伏组件列表
     */
    @TableField(exist = false)
    private JSONArray componentList;
}
