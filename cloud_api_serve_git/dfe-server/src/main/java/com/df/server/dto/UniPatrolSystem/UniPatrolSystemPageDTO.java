package com.df.server.dto.UniPatrolSystem;


import com.df.framework.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分页查询入参
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UniPatrolSystemPageDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 巡视系统名称
     */
    private String sysName;
    /**
     * 巡视系统类型(字典表patrol_sys_type)：
     * center主站，
     * host巡视主机，
     * edge边缘节点，
     * video视频系统，
     * robot机器人系统，
     * uav无人机系统
     */
    private String sysType;
}
