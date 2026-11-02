package com.dji.sample.df.constructionDf;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/construction/parking-points")
public class ConstructionParkingController {
    private final JdbcTemplate db;
    public ConstructionParkingController(JdbcTemplate db) { this.db=db; }
    private String workspace(HttpServletRequest r) {
        CustomClaim c=(CustomClaim)r.getAttribute(TOKEN_CLAIM);
        if(c==null||c.getWorkspaceId()==null||c.getWorkspaceId().isBlank())throw new RuleException(401,"请先登录工作空间");
        return c.getWorkspaceId();
    }
    static double coordinate(Object raw,double min,double max) {
        if(!(raw instanceof Number))throw new RuleException(400,"驻车点坐标无效");
        double n=((Number)raw).doubleValue();if(!Double.isFinite(n)||n<min||n>max)throw new RuleException(400,"驻车点坐标无效");return n;
    }
    static int version(Object raw) {
        if(!(raw instanceof Number))throw new RuleException(400,"缺少驻车点版本，请刷新");
        double n=((Number)raw).doubleValue();if(!Double.isFinite(n)||n<1||n>=Integer.MAX_VALUE||n!=Math.rint(n))throw new RuleException(400,"驻车点版本无效");return (int)n;
    }
    static Map<String,Object> validated(Map<String,Object> body) {
        Object raw=body.get("name");String name=raw instanceof String?((String)raw).trim():"";
        if(name.isEmpty()||name.length()>80)throw new RuleException(400,"驻车点名称须为1至80个字符");
        return new LinkedHashMap<>(Map.of("name",name,"lng",coordinate(body.get("lng"),-180,180),"lat",coordinate(body.get("lat"),-85,85),"radius_m",ConstructionDockCoverageController.radius(body.get("radius_m"))));
    }
    @GetMapping public HttpResultResponse list(HttpServletRequest r) {
        try {return HttpResultResponse.success(db.queryForList("SELECT id,name,lng,lat,radius_m,version FROM construction_parking_point WHERE workspace_id=? ORDER BY created_at,id",workspace(r)));}
        catch(RuleException e){return HttpResultResponse.error(e.getCode(),e.getMessage());}
    }
    @PostMapping public HttpResultResponse create(HttpServletRequest r,@RequestBody Map<String,Object> body) {
        try {
            String w=workspace(r);Map<String,Object> p=validated(body);String id=UUID.randomUUID().toString();
            db.update("INSERT INTO construction_parking_point (id,workspace_id,name,lng,lat,radius_m,version) VALUES (?,?,?,?,?,?,1)",id,w,p.get("name"),p.get("lng"),p.get("lat"),p.get("radius_m"));
            p.put("id",id);p.put("version",1);return HttpResultResponse.success(p);
        } catch(RuleException e){return HttpResultResponse.error(e.getCode(),e.getMessage());}
    }
    @PutMapping("/{id}") public HttpResultResponse update(HttpServletRequest r,@PathVariable String id,@RequestBody Map<String,Object> body) {
        try {
            String w=workspace(r);Map<String,Object> p=validated(body);int v=version(body.get("version"));
            int count=db.update("UPDATE construction_parking_point SET name=?,lng=?,lat=?,radius_m=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE workspace_id=? AND id=? AND version=?",p.get("name"),p.get("lng"),p.get("lat"),p.get("radius_m"),w,id,v);
            if(count!=1)throw new RuleException(409,"驻车点已修改或不存在，请刷新后重试");
            p.put("id",id);p.put("version",v+1);return HttpResultResponse.success(p);
        } catch(RuleException e){return HttpResultResponse.error(e.getCode(),e.getMessage());}
    }
    @DeleteMapping("/{id}") public HttpResultResponse delete(HttpServletRequest r,@PathVariable String id,@RequestParam int version) {
        try {
            String w=workspace(r);int v=version(version);
            if(db.update("DELETE FROM construction_parking_point WHERE workspace_id=? AND id=? AND version=?",w,id,v)!=1)throw new RuleException(409,"驻车点已修改或不存在，请刷新后重试");
            return HttpResultResponse.success();
        } catch(RuleException e){return HttpResultResponse.error(e.getCode(),e.getMessage());}
    }
}
