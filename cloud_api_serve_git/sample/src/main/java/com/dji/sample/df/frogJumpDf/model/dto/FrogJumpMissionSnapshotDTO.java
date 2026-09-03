package com.dji.sample.df.frogJumpDf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrogJumpMissionSnapshotDTO {

    private String droneSn;

    private String takeoffDockSn;

    private String landingDockSn;

    private FrogJumpDroneSnapshotDTO droneSnapshot;

    private FrogJumpDockInfoDTO takeoffDockSnapshot;

    private FrogJumpDockInfoDTO landingDockSnapshot;
}
