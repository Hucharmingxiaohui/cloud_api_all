package com.df.server.entity.his;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务日志记录接口
 * <p>
 * Created by lianyc on 2023-10-31
 */
@Data
@TableName("sys_his_log_business")
public class HisLogBusinessEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId
    private Integer id;

    private String subCode;
    /**
     * 业务模块名称
     */
    private String logTitle;
    /**
     * 业务日志类型(字典busi_log_type)：1查询，2新增，3修改，4删除，5设备控制
     */
    private Integer logType;
    /**
     * 业务日志信息
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
    private Date operTime;

}
