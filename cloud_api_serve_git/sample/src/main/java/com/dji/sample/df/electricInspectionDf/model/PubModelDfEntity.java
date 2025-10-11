package com.dji.sample.df.electricInspectionDf.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@TableName(value = "model_df")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PubModelDfEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("model_id")
    private String modelId;

    @TableField("model_name")
    private String modelName;

    @TableField("workspace_id")
    private String workspaceId;

    @TableField("object_key")
    private String objectKey;

    @TableField(value = "update_time", fill = FieldFill.INSERT)
    private Long updateTime;

    @TableField("sub_code")
    private String subCode;

    @TableField("sub_name")
    private String subName;

    @TableField("json_name")
    private String jsonName;

}

