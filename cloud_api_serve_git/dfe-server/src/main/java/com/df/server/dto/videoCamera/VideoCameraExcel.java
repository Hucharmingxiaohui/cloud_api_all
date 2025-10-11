package com.df.server.dto.videoCamera;

import com.df.framework.annotation.Excel;
import lombok.Data;

import java.io.Serializable;

/**
 * 视频设备Excel导入Bean
 *
 * @author yuzaobo
 * @date 2021/6/2
 */
@Data
public class VideoCameraExcel implements Serializable {
    private static final long serialVersionUID = -2649147529644586914L;
    @Excel(name = "所属设备编码")
    String deviceCode;
    @Excel(name = "摄像机编码")
    String code;
    @Excel(name = "摄像机名称")
    String des;
    @Excel(name = "摄像机类型")
    Integer type;
    @Excel(name = "变电站编码")
    String subCode;
    @Excel(name = "摄像机通道")
    Integer channel;
    @Excel(name = "所属SIP域")
    String sipCode;
    @Excel(name = "CSS通道名称")
    String cssName;
    @Excel(name = "CSS通道号")
    String cssChan;
}
