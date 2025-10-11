package com.df.server.entity.uni;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 区域接口
 * <p>
 * Created by lianyc on 2022-11-17
 */
@Data
@TableName("df_uni_area")
public class UniAreaEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 区域ID
     */
    private String areaId;
    /**
     * 区域名称
     */
    private String areaName;
    /**
     * 所属巡视系统编码
     */
    private String sysCode;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 区域数据来源
     */
    private Integer areaSrc;

}
