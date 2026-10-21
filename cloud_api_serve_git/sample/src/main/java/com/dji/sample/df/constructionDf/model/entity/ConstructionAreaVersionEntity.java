package com.dji.sample.df.constructionDf.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

@TableName("construction_area_version")
public class ConstructionAreaVersionEntity {
    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("area_id")
    private String areaId;

    @TableField("version")
    private Integer version;

    /** WGS84 boundary JSON: [{"lng":..,"lat":..},...] */
    @TableField("boundary")
    private String boundary;

    @TableField("area_sqm")
    private Double areaSqm;

    @TableField("gmt_create")
    private Date gmtCreate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAreaId() { return areaId; }
    public void setAreaId(String areaId) { this.areaId = areaId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getBoundary() { return boundary; }
    public void setBoundary(String boundary) { this.boundary = boundary; }
    public Double getAreaSqm() { return areaSqm; }
    public void setAreaSqm(Double areaSqm) { this.areaSqm = areaSqm; }
    public Date getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(Date gmtCreate) { this.gmtCreate = gmtCreate; }
}
