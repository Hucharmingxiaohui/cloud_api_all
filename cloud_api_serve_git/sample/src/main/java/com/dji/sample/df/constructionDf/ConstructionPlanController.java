package com.dji.sample.df.constructionDf;
import com.dji.sample.common.model.CustomClaim;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
@RestController
@RequestMapping("/api/construction")
public class ConstructionPlanController {
    private final ConstructionPlanService service;
    public ConstructionPlanController(ConstructionPlanService service) { this.service=service; }
    private interface Call { Object run(CustomClaim user); }
    private HttpResultResponse<Object> run(HttpServletRequest request,Call call) {
        CustomClaim user=(CustomClaim)request.getAttribute(TOKEN_CLAIM);
        if(user==null||user.getWorkspaceId()==null) return HttpResultResponse.error(401,"请先登录工作空间");
        try { return HttpResultResponse.success(call.run(user)); } catch(ConstructionAreaGeometry.RuleException e) {return HttpResultResponse.error(e.getCode(),e.getMessage());}
    }
    @GetMapping("/areas/{area}/plans") public HttpResultResponse<Object> list(HttpServletRequest r,@PathVariable String area,@RequestParam(defaultValue="false") boolean archived) {return run(r,u->service.list(u.getWorkspaceId(),area,archived));}
    @PostMapping("/areas/{area}/plans") public HttpResultResponse<Object> create(HttpServletRequest r,@PathVariable String area,@RequestBody Map<String,Object> body) {return run(r,u->service.create(u,area,body));}
    @PostMapping("/plans/{id}/dispatch") public HttpResultResponse<Object> dispatch(HttpServletRequest r,@PathVariable String id,@RequestBody Map<String,Object> body) {return run(r,u->service.dispatch(u,id,body));}
    @PostMapping("/plans/{id}/archive") public HttpResultResponse<Object> archive(HttpServletRequest r,@PathVariable String id) {return run(r,u->{service.archive(u.getWorkspaceId(),id);return null;});}
    @PostMapping("/plans/{id}/refresh-dispatch") public HttpResultResponse<Object> refresh(HttpServletRequest r,@PathVariable String id) {return run(r,u->service.refreshDispatch(u.getWorkspaceId(),id));}
}
