package com.dji.sample.df.accessControlDf.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@TableName(value = "user_grade_control_df")//用于指定数据库表的名称
@Data
public class UserGradeControlDfEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Integer id;//id

    @TableField(value = "user_id")
    private String userId;//用户id

    @TableField(value = "workspace_id")
    private String workspaceId;//工作空间id

    @TableField(value = "user_grade")
    private String userGrade;//用户等级
}
