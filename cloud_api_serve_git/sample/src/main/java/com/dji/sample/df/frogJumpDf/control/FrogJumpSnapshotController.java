package com.dji.sample.df.frogJumpDf.control;

import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpExecuteParam;
import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpMissionSnapshotDTO;
import com.dji.sample.df.frogJumpDf.service.FrogJumpExecuteService;
import com.dji.sample.df.frogJumpDf.service.FrogJumpSnapshotService;
import com.dji.sample.df.frogJumpDf.service.FrogJumpLandingRecommendationService;
import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpLandingRecommendationDTO;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/v1/frog-jump")
public class FrogJumpSnapshotController {

    @Autowired
    private FrogJumpSnapshotService frogJumpSnapshotService;

    @Autowired
    private FrogJumpExecuteService frogJumpExecuteService;

    @Autowired
    private FrogJumpLandingRecommendationService frogJumpLandingRecommendationService;

    @GetMapping("/landing-recommendation")
    public HttpResultResponse<FrogJumpLandingRecommendationDTO> landingRecommendation(HttpServletRequest request,
            @RequestParam("file_id") String fileId, @RequestParam("takeoff_dock_sn") String takeoffDockSn) {
        CustomClaim claim = (CustomClaim) request.getAttribute(TOKEN_CLAIM);
        return frogJumpLandingRecommendationService.recommend(claim == null ? null : claim.getWorkspaceId(), fileId, takeoffDockSn);
    }

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
