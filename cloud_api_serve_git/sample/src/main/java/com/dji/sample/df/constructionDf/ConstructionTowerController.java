package com.dji.sample.df.constructionDf;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

/** 集电线路杆塔标定：地图选点 + 编号（A1、A2…），线路按“起始杆塔→结束杆塔”引用杆塔链生成折线。 */
@RestController
@RequestMapping("/api/construction/towers")
public class ConstructionTowerController {
    private final JdbcTemplate db;
    public ConstructionTowerController(JdbcTemplate db) { this.db=db; }
    private String workspace(HttpServletRequest r) {
        CustomClaim c=(CustomClaim)r.getAttribute(TOKEN_CLAIM);
        if(c==null||c.getWorkspaceId()==null||c.getWorkspaceId().isBlank())throw new RuleException(401,"请先登录工作空间");
        return c.getWorkspaceId();
    }
    static String towerNo(Object raw) {
        if(!(raw instanceof String))throw new RuleException(400,"请填写杆塔编号");
        String no=((String)raw).trim();
        if(no.isEmpty()||no.length()>32)throw new RuleException(400,"杆塔编号须为1至32个字符");
        return no;
    }
    private double[] coordinate(Map<String,Object> body) {
        Object lng=body.get("lng"),lat=body.get("lat");
        if(!(lng instanceof Number)||!(lat instanceof Number))throw new RuleException(400,"请在地图上标定杆塔位置");
        double x=((Number)lng).doubleValue(),y=((Number)lat).doubleValue();
        if(!Double.isFinite(x)||!Double.isFinite(y)||Math.abs(x)>180||Math.abs(y)>90)throw new RuleException(400,"杆塔坐标无效");
        return new double[]{x,y};
    }
    private Map<String,Object> referenced(String w,String id) {
        List<Map<String,Object>> rows=db.queryForList("SELECT id,name,tower_ids FROM construction_line WHERE workspace_id=? AND tower_ids IS NOT NULL",w);
        for(Map<String,Object> row:rows) {
            Object raw=row.get("tower_ids");
            if(raw instanceof String && ((String)raw).contains("\""+id+"\"")) return row;
        }
        return null;
    }
    @GetMapping public HttpResultResponse list(HttpServletRequest r) {
        try {return HttpResultResponse.success(db.queryForList("SELECT id,tower_no,lng,lat,created_at,updated_at FROM construction_tower WHERE workspace_id=? ORDER BY tower_no,id",workspace(r)));}
        catch(RuleException e){return HttpResultResponse.error(e.getCode(),e.getMessage());}
    }
    @PostMapping public HttpResultResponse create(HttpServletRequest r,@RequestBody Map<String,Object> body) {
        try {
            String w=workspace(r);String no=towerNo(body.get("tower_no"));double[] xy=coordinate(body);
            if(!db.queryForList("SELECT id FROM construction_tower WHERE workspace_id=? AND tower_no=?",w,no).isEmpty())throw new RuleException(409,"杆塔编号 "+no+" 已存在，请使用其他编号");
            String id=UUID.randomUUID().toString();
            db.update("INSERT INTO construction_tower (id,workspace_id,tower_no,lng,lat) VALUES (?,?,?,?,?)",id,w,no,xy[0],xy[1]);
            Map<String,Object> out=new LinkedHashMap<>();out.put("id",id);out.put("tower_no",no);out.put("lng",xy[0]);out.put("lat",xy[1]);return HttpResultResponse.success(out);
        } catch(RuleException e){return HttpResultResponse.error(e.getCode(),e.getMessage());}
    }
    @PutMapping("/{id}") public HttpResultResponse update(HttpServletRequest r,@PathVariable String id,@RequestBody Map<String,Object> body) {
        try {
            String w=workspace(r);String no=towerNo(body.get("tower_no"));double[] xy=coordinate(body);
            if(!db.queryForList("SELECT id FROM construction_tower WHERE workspace_id=? AND tower_no=? AND id<>?",w,no,id).isEmpty())throw new RuleException(409,"杆塔编号 "+no+" 已存在，请使用其他编号");
            int count=db.update("UPDATE construction_tower SET tower_no=?,lng=?,lat=?,updated_at=CURRENT_TIMESTAMP WHERE workspace_id=? AND id=?",no,xy[0],xy[1],w,id);
            if(count!=1)throw new RuleException(409,"杆塔已删除，请刷新列表");
            Map<String,Object> out=new LinkedHashMap<>();out.put("id",id);out.put("tower_no",no);out.put("lng",xy[0]);out.put("lat",xy[1]);return HttpResultResponse.success(out);
        } catch(RuleException e){return HttpResultResponse.error(e.getCode(),e.getMessage());}
    }
    @DeleteMapping("/{id}") public HttpResultResponse delete(HttpServletRequest r,@PathVariable String id) {
        try {
            String w=workspace(r);
            if(db.queryForList("SELECT id FROM construction_tower WHERE workspace_id=? AND id=?",w,id).isEmpty())throw new RuleException(404,"杆塔不存在或已删除");
            Map<String,Object> used=referenced(w,id);
            if(used!=null)throw new RuleException(409,"杆塔已被线路「"+used.get("name")+"」引用，请先处理该线路");
            db.update("DELETE FROM construction_tower WHERE workspace_id=? AND id=?",w,id);
            return HttpResultResponse.success(null);
        } catch(RuleException e){return HttpResultResponse.error(e.getCode(),e.getMessage());}
    }
}
