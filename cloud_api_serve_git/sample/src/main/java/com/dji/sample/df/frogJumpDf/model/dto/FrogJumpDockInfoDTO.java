package com.dji.sample.df.frogJumpDf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrogJumpDockInfoDTO {

    private String sn;

    private String dockType;

    private Integer index;

    private Object latitude;

    private Object longitude;

    private Object height;

    private Object heading;

    private Object homePositionIsValid;

    private Object alternateLandPoint;

    private Object rtcmInfo;

    private Map<String, Object> osdData;

    private Map<String, Object> stateData;
}
