package com.df.server.dto.uniPoint;

import lombok.Data;

@Data
public class UniPointBaseImageSyncDTO {

    /**
     * 同步来源 1：全部  2：标定点位
     */
    private Integer baseImageSyncSource;

    private String subCode;

    private String pointCode;

}
