package com.dji.sample.df.accessControlDf.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@TableName(value = "role_tab_df")//用于指定数据库表的名称:角色表
@Data
public class RoleTabDfEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID

    @TableField(value = "role_id")
    private String roleId; // 角色ID

    @TableField(value = "role_name")
    private String roleName; // 角色名称

    @TableField(value = "role_des")
    private String roleDes; // 角色描述

    @TableField(value = "role_creator")
    private String roleCreator; // 创建者

    @TableField(value = "create_time")
    private Long createTime; // 创建时间

    @TableField(value = "update_time")
    private Long updateTime; // 更新时间
}
