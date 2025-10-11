package com.df.server.dto.uniPoint;

import lombok.Data;

@Data
public class BindNodeDTO {
    /**
     * 预置位号
     */
    private String robotPos;
    /**
     * 变电站编码
     */
    private String subCode;
    /**
     * 点位编码
     */
    private String pointCode;
    /**
     * 机器人编码
     */
    private String robotCode;

}
