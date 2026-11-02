package com.dji.sample.df.constructionDf;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;
@RestController
@RequestMapping("/api/construction/analysis-config")
public class ConstructionAnalysisConfigController {
    private final ConstructionAnalysisConfigService service;
    public ConstructionAnalysisConfigController(ConstructionAnalysisConfigService service){this.service=service;}
    private String workspace(HttpServletRequest request){CustomClaim c=(CustomClaim)request.getAttribute(TOKEN_CLAIM);if(c==null||c.getWorkspaceId()==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录工作空间");return c.getWorkspaceId();}
    @GetMapping public HttpResultResponse get(HttpServletRequest request,@RequestParam(required=false) String area_id,@RequestParam(required=false) String wayline_id,@RequestParam(required=false) String overall_id){try{return HttpResultResponse.success(service.get(workspace(request),area_id,wayline_id,overall_id));}catch(ConstructionAreaGeometry.RuleException e){return HttpResultResponse.error(e.getCode(),e.getMessage());}}
    @PutMapping public HttpResultResponse save(HttpServletRequest request,@RequestParam(required=false) String area_id,@RequestParam(required=false) String wayline_id,@RequestParam(required=false) String overall_id,@RequestBody Map<String,Object> body){try{return HttpResultResponse.success(service.save(workspace(request),area_id,wayline_id,overall_id,body));}catch(ConstructionAreaGeometry.RuleException e){return HttpResultResponse.error(e.getCode(),e.getMessage());}}
}
