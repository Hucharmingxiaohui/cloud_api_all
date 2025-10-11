package com.dji.sample.df.accessControlDf.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName(value = "rp_tab_df")//用于指定数据库表的名称:角色表
@Data
public class RpTabDfEntity {
    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID，自增

    @TableField(value = "role_id")
    private String roleId; // 角色ID

    @TableField(value = "privs_id")
    private String privsId; // 权限ID

    @TableField(value = "rp_des")
    private String rpDes; // 角色权限描述

    @TableField(value = "create_time")
    private Long createTime; // 创建时间

    @TableField(value = "update_time")
    private Long updateTime; // 更新时间
}
