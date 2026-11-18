package com.dji.sample.df.constructionDf.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

@Data
@TableName("construction_line")
public class ConstructionLineEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String workspaceId;
    private String name;
    private String type;
    private String color;
    private Boolean enabled;
    private String notes;
    private String towerIds;
    private Integer currentVersion;
    private Boolean archived;
    private Date gmtCreate;
    private Date gmtModified;
}
