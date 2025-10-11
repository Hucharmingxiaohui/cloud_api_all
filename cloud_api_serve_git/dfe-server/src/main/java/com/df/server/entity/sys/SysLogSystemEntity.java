package com.df.server.entity.sys;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统日志记录接口
 * <p>
 * Created by lianyc on 2023-11-02
 */
@Data
@TableName("sys_log_system")
public class SysLogSystemEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
    private Integer id;
    /**
     * 系统模块名称
     */
    private String logTitle;
    /**
     * 系统日志类型(字典sys_log_type)：1用户管理，2权限管理，3系统配置，4日志审计
     */
    private Integer logType;
    /**
     * 系统日志信息
     */
    private String logInfo;
    /**
     * 操作人用户名
     */
    private String operUserName;
    /**
     * 操作人姓名
     */
    private String operRealName;
    /**
     * 操作人IP
     */
    private String operIp;
    /**
     * 操作状态：0失败，1成功
     */
    private Integer operStatus;
    /**
     * 操作时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operTime;

}
