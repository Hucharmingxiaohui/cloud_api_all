package com.df.server.vo.VideoDevice;

import com.df.framework.annotation.Excel;
import lombok.Data;

/**
 * 视频设备Excel导入Bean
 *
 * @author yuzaobo
 * @date 2021/6/2
 */
@Data
public class VideoDeviceExcel {
    @Excel(name = "设备编码")
    String code;
    @Excel(name = "设备名称")
    String des;
    @Excel(name = "设备类型")
    Integer type;
    @Excel(name = "变电站编码")
    String subCode;
    @Excel(name = "ip地址")
    String ip;
    @Excel(name = "端口")
    Integer port;
    @Excel(name = "用户名")
    String username;
    @Excel(name = "密码")
    String password;
    @Excel(name = "插件类型")
    String deviceType;
    @Excel(name = "扩展参数")
    String extraInfo;
    //    @Excel(name = "状态")
//    Integer status;
    @Excel(name = "所属SIP域")
    String sipCode;
}
