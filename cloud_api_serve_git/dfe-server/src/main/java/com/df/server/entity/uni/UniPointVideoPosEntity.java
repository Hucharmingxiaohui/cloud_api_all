package com.df.server.entity.uni;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 巡视点位和摄像机预置位关联接口
 * <p>
 * Created by lianyc on 2022-10-13
 */
@Data
@TableName("df_uni_point_video_pos")
public class UniPointVideoPosEntity implements Serializable {

    private static final long serialVersionUID = 7177771208165969106L;
    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 点位编码
     */
    private String pointCode;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 预置位归属：1视频 2机器人  4无人机 (默任1)
     */
    private Integer dataType;
    /**
     * 设备编码，摄像机编码、声纹设备编码、在线监测的主辅信号
     */
    private String deviceCode;
    /**
     * 预置位号，摄像机预置位、声纹设备存0、在线监测存0
     */
    private Integer devicePos;
    /**
     * 文件类型：1红外 2可见光 3音频；计划发给视频抓拍时需要这个字段，后来讨论还是用point表的save_type_list（此字段暂时不用）
     */
    private Integer fileType;
    /**
     * 关联类型：1预置位 2声纹装置 3在线监测 (默任1)
     */
    private Integer relType;

}
