package com.df.server.entity.uni;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 预置位接口
 * <p>
 * Created by lianyc on 2022-11-25
 */
@Data
@TableName("df_video_preset")
public class VideoPresetEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId
    private Integer id;
    /**
     * 预置位编码
     */
    private String code;
    /**
     * 预置位名称
     */
    private String presetName;
    /**
     *
     */
    private String presetDes;
    /**
     * 所属摄像头code，df_video_camera.code
     */
    private String cameraCode;
    /**
     * 预置位号
     */
    private Integer presetNo;
    /**
     * 图片地址 相对路径
     */
    private String baseFilePath;

}
