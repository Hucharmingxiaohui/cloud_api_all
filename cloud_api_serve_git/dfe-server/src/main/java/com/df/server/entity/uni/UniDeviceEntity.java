package com.df.server.entity.uni;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 主设备接口
 * <p>
 * Created by lianyc on 2022-11-17
 */
@Data
@TableName("df_uni_device")
public class UniDeviceEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 主设备ID
     */
    private String deviceId;
    /**
     * 主设备名称
     */
    private String deviceName;
    /**
     * 主设备类型，字典device_type
     */
    private Integer deviceType;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 所属间隔ID
     */
    private String bayId;
    /**
     * 数据类型(此字段暂时不用了)
     */
    private Integer dataType;
    /**
     * 设备所在地图序号，从1开始编码，0表示没有关联地图
     */
    private Integer mapfileId;
    /**
     * 主设备区域坐标框(像素点)
     * 格式: x1,y1,z1;x2,y2,z2;x3,y3,z3;x4,y4,z4
     */
    private String coordinatePixel;
    /**
     * 所属巡视系统编码
     */
    private String sysCode;

}
