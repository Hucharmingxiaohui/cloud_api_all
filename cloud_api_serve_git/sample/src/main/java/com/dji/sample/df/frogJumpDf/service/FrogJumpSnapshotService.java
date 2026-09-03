package com.dji.sample.df.frogJumpDf.service;

import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpMissionSnapshotDTO;

import java.util.Optional;

public interface FrogJumpSnapshotService {

    Optional<FrogJumpMissionSnapshotDTO> buildMissionSnapshot(String droneSn, String takeoffDockSn, String landingDockSn);
}
