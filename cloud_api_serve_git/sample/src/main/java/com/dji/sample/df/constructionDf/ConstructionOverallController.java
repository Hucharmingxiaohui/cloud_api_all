package com.dji.sample.df.constructionDf;
import com.dji.sample.common.model.CustomClaim;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
@RestController
@RequestMapping("/api/construction/overall-routes")
public class ConstructionOverallController {
    private final ConstructionOverallService routes;private final ConstructionPlanService plans;
    public ConstructionOverallController(ConstructionOverallService routes,ConstructionPlanService plans){this.routes=routes;this.plans=plans;}
    private interface Call {Object run(CustomClaim user);}
    private HttpResultResponse<Object> run(HttpServletRequest r,Call call) {
        CustomClaim u=(CustomClaim)r.getAttribute(TOKEN_CLAIM);
        if(u==null||u.getWorkspaceId()==null)return HttpResultResponse.error(401,"请先登录工作空间");
        try{return HttpResultResponse.success(call.run(u));}catch(ConstructionAreaGeometry.RuleException e){return HttpResultResponse.error(e.getCode(),e.getMessage());}
    }
    @GetMapping public HttpResultResponse<Object> list(HttpServletRequest r,@RequestParam String dock_sn){return run(r,u->routes.list(u.getWorkspaceId(),dock_sn));}
    @GetMapping("/inspect") public HttpResultResponse<Object> inspect(HttpServletRequest r,@RequestParam String dock_sn,@RequestParam String wayline_id){return run(r,u->routes.inspect(u.getWorkspaceId(),dock_sn,wayline_id));}
    @PostMapping("/prepare") public HttpResultResponse<Object> prepare(HttpServletRequest r,@RequestBody Map<String,String> body){return run(r,u->routes.prepare(u.getWorkspaceId(),body.get("dock_sn"),body.get("wayline_id"),body.get("file_sign"),u.getUsername()));}
    @PostMapping public HttpResultResponse<Object> save(HttpServletRequest r,@RequestBody Map<String,Object> body){return run(r,u->routes.save(u.getWorkspaceId(),body));}
    @GetMapping("/{id}/plans") public HttpResultResponse<Object> listPlans(HttpServletRequest r,@PathVariable String id,@RequestParam(defaultValue="false") boolean archived){return run(r,u->plans.listOverall(u.getWorkspaceId(),id,archived));}
    @PostMapping("/{id}/plans") public HttpResultResponse<Object> create(HttpServletRequest r,@PathVariable String id,@RequestBody Map<String,Object> body){return run(r,u->plans.createOverall(u,id,body));}
}
