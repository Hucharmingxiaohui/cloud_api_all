package com.df.server.vo.VideoDevice;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;


/**
 * 返回值
 */
@Data
public class VideoSipServiceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *
     */
    private Integer id;
    /**
     * 服务名称
     */
    private String code;
    /**
     * 描述
     */
    private String des;
    /**
     * 状态
     */
    private Integer status;
    /**
     * ip
     */
    private String ip;
    /**
     * 端口号
     */
    private String port;
    /**
     * 密码
     */
    private String password;
    /**
     * 根节点
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatetime;
    /**
     *
     */
    private Integer loads;
    /**
     *
     */
    private Integer timeout;
    /**
     * 0-udp,1-tcp
     */
    private Integer protocol;
}
