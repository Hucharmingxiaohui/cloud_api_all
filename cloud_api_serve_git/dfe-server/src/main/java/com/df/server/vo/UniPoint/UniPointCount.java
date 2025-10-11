package com.df.server.vo.UniPoint;

import lombok.Data;

@Data
public class UniPointCount {
    /**
     * 点位数量
     */
    private int pointCount;
    /**
     * 相机数量
     */
    private int cameraCount;
    /**
     * 基准正常
     */
    private int baseImageOkNum;
    /**
     * 基准异常
     */
    private int baseImageErrNum;
}
