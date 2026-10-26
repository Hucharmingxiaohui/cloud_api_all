package com.dji.sample.df.constructionDf;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ConstructionPlanService {
    private final JdbcTemplate db; private final PubWaylineJobPlanDfMapper plans;
    private final ConstructionAreaService areas; private final ConstructionPlanGuard guard;
    private final PubWaylineJobPlanDfService execution; private final TransactionTemplate tx;
    private final ObjectMapper json=new ObjectMapper();
    public ConstructionPlanService(JdbcTemplate db,PubWaylineJobPlanDfMapper plans,ConstructionAreaService areas,ConstructionPlanGuard guard,PubWaylineJobPlanDfService execution,PlatformTransactionManager manager) {
        this.db=db;this.plans=plans;this.areas=areas;this.guard=guard;this.execution=execution;this.tx=new TransactionTemplate(manager);
    }
    private String encode(Object value) { try { return json.writeValueAsString(value); } catch(Exception e) { throw new RuleException(400,"计划参数无效"); } }
    private String text(Map<String,Object> body,String key,int max) {
        Object value=body.get(key); if(!(value instanceof String)||((String)value).isBlank()||((String)value).length()>max) throw new RuleException(400,"请正确填写 "+key);
        return ((String)value).trim();
    }
    private int number(Map<String,Object> body,String key,int min,int max) {
        Object value=body.get(key);if(!(value instanceof Number)) throw new RuleException(400,"请正确填写 "+key);
        double n=((Number)value).doubleValue();if(!Double.isFinite(n)||n!=Math.rint(n)||n<min||n>max) throw new RuleException(400,key+" 超出允许范围");return (int)n;
    }
    private String request(Map<String,Object> body) { String id=text(body,"request_id",64);if(!id.matches("[a-zA-Z0-9-]{16,64}")) throw new RuleException(400,"请求标识无效");return id; }
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> create(CustomClaim user,String area,Map<String,Object> body) {
        String w=user.getWorkspaceId();
        if(db.queryForList("SELECT id FROM construction_area WHERE workspace_id=? AND id=? FOR UPDATE",w,area).isEmpty()) throw new RuleException(404,"区域不存在");
        String req=request(body);String digest=DigestUtils.md5DigestAsHex(encode(List.of(area,new TreeMap<>(body))).getBytes(StandardCharsets.UTF_8));
        List<Map<String,Object>> previous=db.queryForList("SELECT * FROM construction_plan WHERE workspace_id=? AND request_id=?",w,req);
        if(!previous.isEmpty()) { if(!digest.equals(previous.get(0).get("request_hash"))) throw new RuleException(409,"此请求已用于另一份计划");return detail(w,(String)previous.get(0).get("plan_id")); }
        int version=number(body,"expected_version",1,Integer.MAX_VALUE);
        String file=text(body,"file_id",64),dock=text(body,"dock_sn",64),name=text(body,"name",50);
        int type=number(body,"task_type",0,1),rth=number(body,"rth_altitude",30,1500),action=number(body,"out_of_control",0,2);
        long begin=System.currentTimeMillis();
        if(type==1) {
            Object raw=body.get("begin_time");if(!(raw instanceof Number)) throw new RuleException(400,"请选择定时执行时间");
            begin=((Number)raw).longValue();if(begin<=System.currentTimeMillis()+30000) throw new RuleException(400,"执行时间须至少晚于当前时间30秒");
        }
        guard.dock(w,dock);Map<String,Object> snapshot=guard.route(w,area,file,version,true);
        PubWaylineJobPlanDfEntity p=new PubWaylineJobPlanDfEntity();
        p.setPlanId(UUID.randomUUID().toString());p.setWorkspaceId(w);p.setUsername(user.getUsername());p.setPlanSource("系统创建");
        p.setName(name);p.setFileId(file);p.setDockSn(dock);p.setTaskType(type);p.setWaylineType(0);p.setPlanType(ConstructionType.PLAN_TYPE);
        p.setRthAltitude(rth);p.setOutOfControl(action);p.setBeginTime(begin);p.setEnableStatus(0);p.setPlanPriority(1);
        p.setCreateTime(System.currentTimeMillis());p.setUpdateTime(p.getCreateTime());
        if(plans.insert(p)!=1) throw new RuleException(500,"计划保存失败");
        db.update("INSERT INTO construction_plan (plan_id,workspace_id,area_id,area_version,file_sign,snapshot_json,request_id,request_hash) VALUES (?,?,?,?,?,?,?,?)",p.getPlanId(),w,area,version,snapshot.get("file_sign"),encode(snapshot),req,digest);
        return detail(w,p.getPlanId());
    }
    public List<Map<String,Object>> list(String w,String area,boolean archived) {
        areas.detail(w,area);
        List<Map<String,Object>> rows=db.queryForList("SELECT p.*,c.area_id,c.area_version,c.archived FROM pub_wayline_job_plan_df p JOIN construction_plan c ON c.plan_id=p.plan_id AND c.workspace_id=p.workspace_id WHERE c.workspace_id=? AND c.area_id=? AND p.plan_type=6 AND (?=1 OR c.archived=0) ORDER BY p.id DESC",w,area,archived?1:0);
        for(Map<String,Object> row:rows) decorate(w,row);return rows;
    }
    public Map<String,Object> detail(String w,String id) {
        guard.require(w,id);
        List<Map<String,Object>> rows=db.queryForList("SELECT p.*,c.area_id,c.area_version,c.archived FROM pub_wayline_job_plan_df p JOIN construction_plan c ON c.plan_id=p.plan_id AND c.workspace_id=p.workspace_id WHERE c.workspace_id=? AND p.plan_id=?",w,id);
        if(rows.isEmpty()) throw new RuleException(404,"计划不存在");Map<String,Object> row=rows.get(0);decorate(w,row);return row;
    }
    private void decorate(String w,Map<String,Object> row) {
        String id=(String)row.get("plan_id");row.put("status","READY");row.put("message","可下发");
        try { guard.validate(guard.require(w,id),false); } catch(RuleException e) { row.put("status","BLOCKED");row.put("message",e.getMessage()); }
        List<Map<String,Object>> last=db.queryForList("SELECT status,job_id,message,created_at FROM construction_plan_dispatch WHERE workspace_id=? AND plan_id=? ORDER BY dispatch_order DESC LIMIT 1",w,id);
        if(!last.isEmpty()) { row.put("last_dispatch",last.get(0));String state=(String)last.get(0).get("status");if(List.of("SUBMITTING","UNKNOWN").contains(state)) {row.put("status","BLOCKED");row.put("message","下发结果待确认，请先在飞行任务列表核查，勿重复下发");} }
        if(Objects.equals(row.get("task_type"),1)&&!last.isEmpty()&&"SUCCEEDED".equals(last.get(0).get("status"))) { row.put("status","BLOCKED");row.put("message","定时任务已提交，请在飞行任务列表查看或取消"); }
        if(!db.queryForList("SELECT job_id FROM wayline_job WHERE workspace_id=? AND plan_id=? AND status IN (1,2,6)",w,id).isEmpty()) {row.put("status","BLOCKED");row.put("message","此计划已有待执行或执行中的任务，请在飞行任务列表查看");}
    }
    @Transactional(rollbackFor=Exception.class)
    public void archive(String w,String id) {
        guard.require(w,id);db.queryForList("SELECT plan_id FROM construction_plan WHERE workspace_id=? AND plan_id=? FOR UPDATE",w,id);
        if(!db.queryForList("SELECT id FROM construction_plan_dispatch WHERE workspace_id=? AND plan_id=? AND status IN ('SUBMITTING','UNKNOWN')",w,id).isEmpty()) throw new RuleException(409,"请先确认未完成的下发结果");
        if(!db.queryForList("SELECT job_id FROM wayline_job WHERE workspace_id=? AND plan_id=? AND status IN (1,2,6)",w,id).isEmpty()) throw new RuleException(409,"请先在飞行任务列表取消或完成已有任务，再归档计划");
        db.update("UPDATE construction_plan SET archived=1 WHERE workspace_id=? AND plan_id=?",w,id);
    }
    public Map<String,Object> dispatch(CustomClaim user,String id,Map<String,Object> body) {
        String w=user.getWorkspaceId(),req=request(body);
        Map<String,Object> previous=tx.execute(status->{
            PubWaylineJobPlanDfEntity p=guard.require(w,id);
            db.queryForList("SELECT plan_id FROM construction_plan WHERE workspace_id=? AND plan_id=? FOR UPDATE",w,id);
            List<Map<String,Object>> old=db.queryForList("SELECT * FROM construction_plan_dispatch WHERE workspace_id=? AND request_id=?",w,req);
            if(!old.isEmpty()) {if(!id.equals(old.get(0).get("plan_id"))) throw new RuleException(409,"请求标识已被其他计划使用");return old.get(0);}
            guard.validate(p,true);
            if(!db.queryForList("SELECT job_id FROM wayline_job WHERE workspace_id=? AND plan_id=? AND status IN (1,2,6)",w,id).isEmpty()) throw new RuleException(409,"已有待执行或执行中的任务，请勿重复下发");
            if(!db.queryForList("SELECT id FROM construction_plan_dispatch WHERE workspace_id=? AND plan_id=? AND (status IN ('SUBMITTING','UNKNOWN') OR (?=1 AND status='SUCCEEDED'))",w,id,p.getTaskType()).isEmpty()) throw new RuleException(409,"此计划已有待确认或已提交的定时下发，请查看飞行任务列表");
            Map<String,Object> snapshot=new LinkedHashMap<>(guard.relation(w,id));snapshot.put("plan",p);
            db.update("INSERT INTO construction_plan_dispatch (id,workspace_id,plan_id,request_id,status,snapshot_json) VALUES (?,?,?,?,?,?)",UUID.randomUUID().toString(),w,id,req,"SUBMITTING",encode(snapshot));return null;
        });
        if(previous!=null) return previous;
        String state="UNKNOWN",message="下发结果待确认，请查看飞行任务列表",job=null;
        try {
            HttpResultResponse result=guard.dispatch(id,()->execution.expressPlan(user,guard.require(w,id)));
            if(result.getCode()==0) {state="SUCCEEDED";job=result.getMessage();message="已提交，请在飞行任务列表查看执行状态";}
            else { message="下发未完成："+result.getMessage()+"。请核查飞行任务列表后处理"; }
        } catch(Exception e) { message="下发结果待确认："+(e.getMessage()==null?"通信异常":e.getMessage()); }
        Map<String,Object> record=db.queryForList("SELECT * FROM construction_plan_dispatch WHERE workspace_id=? AND request_id=?",w,req).get(0);
        if(job==null) job=(String)record.get("job_id");
        if(job==null&&"UNKNOWN".equals(state)) {state="FAILED";message="任务尚未发送至设备："+message;}
        db.update("UPDATE construction_plan_dispatch SET status=?,job_id=?,message=? WHERE workspace_id=? AND request_id=?",state,job,message.substring(0,Math.min(1000,message.length())),w,req);
        return db.queryForList("SELECT * FROM construction_plan_dispatch WHERE workspace_id=? AND request_id=?",w,req).get(0);
    }
    public Map<String,Object> refreshDispatch(String w,String id) {
        guard.require(w,id);
        // Only reconcile from a correlated persisted job; absence or pending is not proof of failure.
        db.update("UPDATE construction_plan_dispatch d JOIN wayline_job j ON j.job_id=d.job_id AND j.workspace_id=d.workspace_id SET d.status=CASE WHEN j.status IN (2,3) THEN 'SUCCEEDED' ELSE 'FAILED' END,d.message='已按飞行任务状态核实' WHERE d.workspace_id=? AND d.plan_id=? AND d.status IN ('UNKNOWN','SUBMITTING') AND j.status IN (2,3,4,5)",w,id);
        return detail(w,id);
    }
}
