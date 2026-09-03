package com.dji.sample.df.frogJumpDf.control;

import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpExecuteParam;
import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpMissionSnapshotDTO;
import com.dji.sample.df.frogJumpDf.service.FrogJumpExecuteService;
import com.dji.sample.df.frogJumpDf.service.FrogJumpSnapshotService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/frog-jump")
public class FrogJumpSnapshotController {

    @Autowired
    private FrogJumpSnapshotService frogJumpSnapshotService;

    @Autowired
    private FrogJumpExecuteService frogJumpExecuteService;

    @GetMapping("/snapshot")
    public HttpResultResponse<FrogJumpMissionSnapshotDTO> snapshot(@RequestParam String droneSn,
                                                                   @RequestParam String takeoffDockSn,
                                                                   @RequestParam String landingDockSn) {
        return frogJumpSnapshotService.buildMissionSnapshot(droneSn, takeoffDockSn, landingDockSn)
                .map(HttpResultResponse::success)
                .orElseGet(() -> HttpResultResponse.error("snapshot data not ready"));
    }

    @PostMapping("/execute")
    public HttpResultResponse execute(@Valid @RequestBody FrogJumpExecuteParam param) {
        return frogJumpExecuteService.execute(param);
    }
}
