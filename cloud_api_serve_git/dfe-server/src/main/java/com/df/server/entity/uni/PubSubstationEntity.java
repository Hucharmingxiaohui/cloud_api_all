package com.df.server.entity.uni;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 场站接口
 * <p>
 * Created by lianyc on 2025-02-11
 */
@Data
@TableName("df_pub_substation")
public class PubSubstationEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 场站id
     */
    @TableId
    private Integer id;
    /**
     * 场站编码
     */
    private String subCode;
    /**
     * 名称
     */
    private String subName;
    /**
     *
     */
    private String des;
    /**
     * 所属公司ID
     */
    private Integer deptId;
    /**
     *
     */
    private String uri;
    /**
     * 地址
     */
    private String addr;
    /**
     * 电压等级
     */
    private Integer vlevel;
    /**
     * 经度
     */
    private Double longitude;
    /**
     * 纬度
     */
    private Double latitude;
    /**
     * 海拔
     */
    private Double altitude;
    /**
     * 变电站类型：AIS站、GIS站、HGIS站
     */
    private String subType;

}
