package com.dji.sample.df.constructionDf;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.frogJumpDf.service.FrogJumpLandingRecommendationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;

/** Validation shared by the construction endpoint and the legacy dispatch path. */
@Service
public class ConstructionPlanGuard {
    @org.springframework.beans.factory.annotation.Autowired private ConstructionOverallService overall;
    private final ThreadLocal<String> dispatchPlan = new ThreadLocal<>();
    private final JdbcTemplate db;
    private final PubWaylineJobPlanDfMapper plans;
    private final ConstructionAreaService areas;
    private final ConstructionWaylineService routes;
    private final FrogJumpLandingRecommendationService docks;
    public ConstructionPlanGuard(JdbcTemplate db,PubWaylineJobPlanDfMapper plans,ConstructionAreaService areas,ConstructionWaylineService routes,FrogJumpLandingRecommendationService docks) {
        this.db=db;this.plans=plans;this.areas=areas;this.routes=routes;this.docks=docks;
    }
    public static boolean construction(PubWaylineJobPlanDfEntity p) { return p!=null&&Objects.equals(p.getPlanType(),ConstructionType.PLAN_TYPE); }
    public PubWaylineJobPlanDfEntity require(String workspace,String id) {
        PubWaylineJobPlanDfEntity p=plans.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>().eq(PubWaylineJobPlanDfEntity::getPlanId,id));
        if(!construction(p)||!Objects.equals(workspace,p.getWorkspaceId())) throw new RuleException(404,"工地计划不存在");
        return p;
    }
    public Map<String,Object> relation(String workspace,String id) {
        List<Map<String,Object>> result=db.queryForList("SELECT * FROM construction_plan WHERE workspace_id=? AND plan_id=?",workspace,id);
        if(result.isEmpty()) throw new RuleException(400,"工地计划缺少区域关联，请重新创建");
        return result.get(0);
    }
    public void dock(String workspace,String sn) {
        if(!docks.isWorkspaceDock(workspace,sn)) throw new RuleException(400,"执行机巢须绑定到当前工作空间");
    }
    public Map<String,Object> route(String workspace,String area,String file,int version,boolean readFile) {
        Map<String,Object> a=areas.detail(workspace,area);
        if(Boolean.TRUE.equals(a.get("archived"))||!Boolean.TRUE.equals(a.get("enabled"))) throw new RuleException(400,"巡检区域已暂停或归档");
        if(((Number)a.get("version")).intValue()!=version) throw new RuleException(409,"区域版本已变化，请刷新并重新创建计划");
        Map<String,Object> r=routes.detail(workspace,area,file);
        if(!"VALID".equals(r.get("status"))) throw new RuleException(409,String.valueOf(r.get("message")));
        if(readFile) try { routes.validate(workspace,area,file); } catch(IOException e) { throw new RuleException(400,"无法读取航线文件，请检查本地存储"); }
        List<Map<String,Object>> links=db.queryForList("SELECT file_sign FROM construction_area_wayline WHERE workspace_id=? AND area_id=? AND wayline_id=?",workspace,area,file);
        if(links.isEmpty()) throw new RuleException(409,"航线已解除区域关联");
        Map<String,Object> result=new LinkedHashMap<>();result.put("area",a);result.put("wayline",r);result.put("file_sign",links.get(0).get("file_sign"));return result;
    }
    public void validate(PubWaylineJobPlanDfEntity p,boolean readFile) {
        Map<String,Object> link=relation(p.getWorkspaceId(),p.getPlanId());
        if(((Number)link.get("archived")).intValue()!=0) throw new RuleException(400,"计划已归档");
        Map<String,Object> frozen=overall==null?Map.of():overall.decode(link.get("snapshot_json"));
        Map<String,Object> snapshot;
        if(frozen.get("overall_route") instanceof Map) {
            Map<String,Object> r=(Map<String,Object>)frozen.get("overall_route");
            if(!Objects.equals(p.getFileId(),r.get("wayline_id"))||!Objects.equals(p.getDockSn(),r.get("dock_sn"))) throw new RuleException(409,"总体计划航线或机巢与绑定快照不一致");
            overall.validate(p.getWorkspaceId(),r,readFile);snapshot=frozen;
        } else snapshot=route(p.getWorkspaceId(),(String)link.get("area_id"),p.getFileId(),((Number)link.get("area_version")).intValue(),readFile);
        if(!Objects.equals(link.get("file_sign"),snapshot.get("file_sign"))) throw new RuleException(409,"航线文件已变化，请重新创建计划");
        dock(p.getWorkspaceId(),p.getDockSn());
        if(Objects.equals(p.getTaskType(),1)&&(p.getBeginTime()==null||p.getBeginTime()<=System.currentTimeMillis())) throw new RuleException(400,"计划执行时间已过，请重新创建计划");
    }
    public PubWaylineJobPlanDfEntity resolve(String workspace,PubWaylineJobPlanDfEntity supplied) {
        PubWaylineJobPlanDfEntity byId=supplied.getId()==null?null:plans.selectById(supplied.getId());
        PubWaylineJobPlanDfEntity byPlan=supplied.getPlanId()==null?null:plans.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>().eq(PubWaylineJobPlanDfEntity::getPlanId,supplied.getPlanId()));
        if(!construction(supplied)&&!construction(byId)&&!construction(byPlan)) return supplied;
        PubWaylineJobPlanDfEntity actual=construction(byId)?byId:byPlan;
        if(actual==null||!Objects.equals(workspace,actual.getWorkspaceId())) throw new RuleException(404,"工地计划不存在");
        if(!actual.getPlanId().equals(dispatchPlan.get())) throw new RuleException(400,"请从工地巡检计划页面下发，避免重复提交");
        if(Boolean.TRUE.equals(supplied.getFrogJumpMode())) throw new RuleException(400,"工地计划当前按绑定机巢执行，暂不支持蛙跳下发");
        validate(actual,true);
        return actual; // Never trust client-supplied file/dock/type/workspace fields.
    }
    public <T> T dispatch(String id,java.util.concurrent.Callable<T> call) throws Exception {
        if(dispatchPlan.get()!=null) throw new IllegalStateException("Nested construction dispatch");
        dispatchPlan.set(id);try { return call.call(); } finally { dispatchPlan.remove(); }
    }
    public void attachJob(String workspace,String jobId) {
        String plan=dispatchPlan.get();if(plan==null) return;
        if(db.update("UPDATE construction_plan_dispatch SET job_id=? WHERE workspace_id=? AND plan_id=? AND status='SUBMITTING' AND job_id IS NULL",jobId,workspace,plan)!=1) throw new RuleException(409,"下发记录状态冲突，任务尚未发送至设备");
    }
    public void beforeExecute(String workspace,String jobId) {
        List<Map<String,Object>> rows=db.queryForList("SELECT p.plan_id,j.file_id,j.dock_sn FROM wayline_job j JOIN pub_wayline_job_plan_df p ON p.plan_id=j.plan_id WHERE j.workspace_id=? AND j.job_id=? AND p.plan_type=6",workspace,jobId);
        if(rows.isEmpty()) return;
        PubWaylineJobPlanDfEntity p=require(workspace,(String)rows.get(0).get("plan_id"));
        if(!Objects.equals(p.getFileId(),rows.get(0).get("file_id"))||!Objects.equals(p.getDockSn(),rows.get(0).get("dock_sn"))) throw new RuleException(409,"任务航线或机巢与工地计划不一致，拒绝执行");
        // Scheduled jobs have already reached begin_time; still recheck geometry and ownership.
        p.setTaskType(0);validate(p,true);
    }
}
