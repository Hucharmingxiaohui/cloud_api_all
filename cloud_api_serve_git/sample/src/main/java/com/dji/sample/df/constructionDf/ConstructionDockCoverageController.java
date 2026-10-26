package com.dji.sample.df.constructionDf;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.frogJumpDf.service.FrogJumpLandingRecommendationService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/construction/docks/{sn}/coverage")
public class ConstructionDockCoverageController {
    private final JdbcTemplate db;
    private final FrogJumpLandingRecommendationService docks;
    public ConstructionDockCoverageController(JdbcTemplate db,FrogJumpLandingRecommendationService docks) {this.db=db;this.docks=docks;}
    private String workspace(HttpServletRequest request,String sn) {
        CustomClaim user=(CustomClaim)request.getAttribute(TOKEN_CLAIM);
        if(user==null||user.getWorkspaceId()==null) throw new ConstructionAreaGeometry.RuleException(401,"请先登录工作空间");
        if(!docks.isWorkspaceDock(user.getWorkspaceId(),sn)) throw new ConstructionAreaGeometry.RuleException(404,"当前工作空间未绑定该机巢");
        return user.getWorkspaceId();
    }
    public static int radius(Object raw) {
        if(!(raw instanceof Number)) throw new ConstructionAreaGeometry.RuleException(400,"覆盖半径须为100至5000米的整数");
        double n=((Number)raw).doubleValue();
        if(!Double.isFinite(n)||n!=Math.rint(n)||n<100||n>5000) throw new ConstructionAreaGeometry.RuleException(400,"覆盖半径须为100至5000米的整数");
        return (int)n;
    }
    @GetMapping public HttpResultResponse<Object> get(HttpServletRequest request,@PathVariable String sn) {
        try {
            String w=workspace(request,sn);
            List<Map<String,Object>> rows=db.queryForList("SELECT radius_m FROM construction_dock_coverage WHERE workspace_id=? AND dock_sn=?",w,sn);
            Map<String,Object> result=new LinkedHashMap<>();result.put("dock_sn",sn);result.put("radius_m",rows.isEmpty()?null:rows.get(0).get("radius_m"));
            return HttpResultResponse.success(result);
        } catch(ConstructionAreaGeometry.RuleException e) { return HttpResultResponse.error(e.getCode(),e.getMessage()); }
    }
    @PutMapping public HttpResultResponse<Object> save(HttpServletRequest request,@PathVariable String sn,@RequestBody Map<String,Object> body) {
        try {
            String w=workspace(request,sn);int value=radius(body.get("radius_m"));
            db.update("INSERT INTO construction_dock_coverage (workspace_id,dock_sn,radius_m) VALUES (?,?,?) ON DUPLICATE KEY UPDATE radius_m=VALUES(radius_m),updated_at=CURRENT_TIMESTAMP",w,sn,value);
            return HttpResultResponse.success(Map.of("dock_sn",sn,"radius_m",value));
        } catch(ConstructionAreaGeometry.RuleException e) { return HttpResultResponse.error(e.getCode(),e.getMessage()); }
    }
}
