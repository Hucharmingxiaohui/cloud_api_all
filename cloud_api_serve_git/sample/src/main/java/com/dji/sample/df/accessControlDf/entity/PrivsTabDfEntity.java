package com.dji.sample.df.accessControlDf.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@TableName(value = "privs_tab_df")//用于指定数据库表的名称
@Data
public class PrivsTabDfEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID

    @TableField(value = "privs_id")
    private String privsId; // 权限ID

    @TableField(value = "privs_name")
    private String privsName; // 权限名称

    @TableField(value = "privs_des")
    private String privsDes; // 权限描述

    @TableField(value = "prvis_creator")
    private String prvisCreator; // 权限创建者

    @TableField(value = "create_time")
    private Long createTime; // 创建时间

    @TableField(value = "update_time")
    private Long updateTime; // 更新时间
}
