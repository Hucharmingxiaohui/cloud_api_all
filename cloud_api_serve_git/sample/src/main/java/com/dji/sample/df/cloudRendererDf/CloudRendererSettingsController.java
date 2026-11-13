package com.dji.sample.df.cloudRendererDf;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sdk.common.HttpResultResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/platform/cloud-renderer-config")
public class CloudRendererSettingsController {
    private final CloudRendererSettingsService service;
    public CloudRendererSettingsController(CloudRendererSettingsService service) { this.service=service; }
    private void authorize(HttpServletRequest request, HttpServletResponse response) {
        CustomClaim claim=(CustomClaim)request.getAttribute(TOKEN_CLAIM);
        if(claim==null || claim.getWorkspaceId()==null || claim.getWorkspaceId().isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录工作空间");
        if(!Integer.valueOf(1).equals(claim.getUserType())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"仅平台用户可访问云渲染配置");
        response.setHeader("Cache-Control","no-store");
    }
    @GetMapping public HttpResultResponse read(HttpServletRequest request,HttpServletResponse response) throws Exception {
        authorize(request,response);return HttpResultResponse.success(service.read());
    }
    @PutMapping public HttpResultResponse save(@RequestBody JsonNode body,HttpServletRequest request,HttpServletResponse response) throws Exception {
        authorize(request,response);return HttpResultResponse.success(service.save(body));
    }
}
