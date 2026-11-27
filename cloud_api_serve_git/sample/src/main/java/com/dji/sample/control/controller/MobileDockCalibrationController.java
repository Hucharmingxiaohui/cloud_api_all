package com.dji.sample.control.controller;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.control.service.impl.MobileDockCalibrationService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/mobile-docks")
public class MobileDockCalibrationController {
    private final MobileDockCalibrationService service;
    public MobileDockCalibrationController(MobileDockCalibrationService service) {this.service=service;}
    private String workspace(HttpServletRequest request) {
        CustomClaim claim=(CustomClaim)request.getAttribute(TOKEN_CLAIM);
        if(claim==null||claim.getWorkspaceId()==null||claim.getWorkspaceId().isBlank()) throw new IllegalArgumentException("请先登录工作空间");
        return claim.getWorkspaceId();
    }
    @GetMapping("/{sn}/calibration")
    public HttpResultResponse status(HttpServletRequest request,@PathVariable String sn) {
        try {return HttpResultResponse.success(service.status(workspace(request),sn));}
        catch(IllegalArgumentException e) {return HttpResultResponse.error(e.getMessage());}
    }
    @PostMapping("/{sn}/calibration")
    public HttpResultResponse start(HttpServletRequest request,@PathVariable String sn) {
        try {return HttpResultResponse.success(service.start(workspace(request),sn));}
        catch(IllegalArgumentException e) {return HttpResultResponse.error(e.getMessage());}
    }
}
