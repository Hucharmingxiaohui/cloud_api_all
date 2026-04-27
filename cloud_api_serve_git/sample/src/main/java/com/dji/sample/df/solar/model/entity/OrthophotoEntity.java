package com.dji.sample.df.solar.model.entity;

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
    @TableId(type = IdType.AUTO)
    private Long id;

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

}
