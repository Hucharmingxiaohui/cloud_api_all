package com.cleaner.djuav.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:Cleaner
 * Date: 2024/12/22 10:46
 **/
@Data
public class CoordinatePointReq implements Serializable {

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 高度
     */
    private Double height;

}
