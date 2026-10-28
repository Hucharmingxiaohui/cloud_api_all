package com.dji.sample.df.constructionDf;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.servlet.http.HttpServletRequest;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/construction/transfers")
public class ConstructionTransferController {
    private final ConstructionTransferService service;
    public ConstructionTransferController(ConstructionTransferService service) {this.service=service;}
    private String workspace(HttpServletRequest request) {
        CustomClaim claim=(CustomClaim)request.getAttribute(TOKEN_CLAIM);
        if(claim==null||claim.getWorkspaceId()==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录工作空间");
        return claim.getWorkspaceId();
    }
    @GetMapping public HttpResultResponse list(HttpServletRequest request) throws Exception {return HttpResultResponse.success(service.status(workspace(request)));}
    @PostMapping("/scan") public HttpResultResponse scan(HttpServletRequest request) throws Exception {return HttpResultResponse.success(service.scan(workspace(request)));}
}
