package com.dji.sample.df.constructionDf.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

@Data
@TableName("construction_line_version")
public class ConstructionLineVersionEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String lineId;
    private Integer version;
    private String name;
    private String type;
    private String color;
    private Boolean enabled;
    private String notes;
    private Boolean archived;
    private String points;
    private String towerNos;
    private Double lengthM;
    private Date gmtCreate;
}
