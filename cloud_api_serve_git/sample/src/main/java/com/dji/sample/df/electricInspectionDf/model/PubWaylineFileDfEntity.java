package com.dji.sample.df.electricInspectionDf.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("pub_wayline_file_df")
public class PubWaylineFileDfEntity implements Serializable {
    @TableId(type = IdType.AUTO) // MyBatis-Plus注解，指定主键字段及其生成策略
    private Integer id; // 主键id

    @TableField("wayline_id") // MyBatis-Plus注解，指定数据库字段名
    private String waylineId; // 航线id

    @TableField("wayline_name") // MyBatis-Plus注解，指定数据库字段名
    private String waylineName; // 航线名称

    @TableField("sub_code") // MyBatis-Plus注解，指定数据库字段名
    private String subCode; // 变电站编码

    @TableField("major") // MyBatis-Plus注解，指定数据库字段名
    private String major; // 专业

    @TableField("workspace_id") // MyBatis-Plus注解，指定数据库字段名
    private String workspaceId; // 工作空间id
}
