package com.df.server.dto.uniPoint;

import lombok.Data;

import java.io.Serializable;

/**
 * 点位详情查询DTO
 *
 * @author 辛英迪
 * @date 2022/8/18 16:37
 */
@Data
public class PointDetailDTO implements Serializable {

    private static final long serialVersionUID = 549345422418816531L;


    /**
     * 点位编码
     */
    private String pointCode;

    /**
     * 变电站编码
     */
    private String subCode;

}
