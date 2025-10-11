package com.dji.sample.df.accessControlDf.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
@TableName(value = "ur_tab_df")//用于指定数据库表的名称:角色表
@Data
public class UrTabDfEntity implements Serializable {
    @TableId(type = IdType.AUTO) // 主键自增
    private Integer id; // 主键ID

    @TableField(value = "user_id") // 用户ID
    private String userId;

    @TableField(value = "role_id") // 角色ID
    private String roleId;

    @TableField(value = "ur_des") // 用户角色描述
    private String urDes;

    @TableField(value = "create_time") // 创建时间
    private Long createTime;

    @TableField(value = "update_time") // 更新时间
    private Long updateTime;
}
