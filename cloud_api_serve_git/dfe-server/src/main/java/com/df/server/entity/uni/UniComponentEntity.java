package com.df.server.entity.uni;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 部件接口
 * <p>
 * Created by lianyc on 2022-11-17
 */
@Data
@TableName("df_uni_component")
public class UniComponentEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 部件ID
     */
    private String componentId;
    /**
     * 部件名称
     */
    private String componentName;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 所属设备ID
     */
    private String deviceId;
    /**
     * 所属巡视系统编码
     */
    private String sysCode;

}
