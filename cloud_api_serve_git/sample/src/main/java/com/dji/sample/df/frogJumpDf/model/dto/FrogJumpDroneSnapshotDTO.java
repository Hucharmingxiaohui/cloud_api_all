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
public class FrogJumpDroneSnapshotDTO {

    private String sn;

    private Object bestLinkGateway;

    private Object wirelessLinkTopo;

    private Map<String, Object> osdData;

    private Map<String, Object> stateData;
}
