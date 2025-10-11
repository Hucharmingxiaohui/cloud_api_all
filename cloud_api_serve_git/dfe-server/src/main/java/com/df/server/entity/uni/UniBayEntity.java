package com.df.server.entity.uni;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 间隔接口
 * <p>
 * Created by lianyc on 2022-11-17
 */
@Data
@TableName("df_uni_bay")
public class UniBayEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 间隔ID
     */
    private String bayId;
    /**
     * 间隔名称
     */
    private String bayName;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 电压等级，单位kV
     */
    private Integer voltLevel;
    /**
     * 间隔数据来源
     */
    private Integer baySrc;
    /**
     * 所属巡视系统编码
     */
    private String sysCode;
    /**
     * 所属区域ID
     */
    private String areaId;

}
