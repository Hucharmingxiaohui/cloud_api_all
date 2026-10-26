package com.dji.sample.df.constructionDf;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.dji.sample.df.frogJumpDf.service.FrogJumpLandingRecommendationService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.*;
import org.springframework.transaction.TransactionDefinition;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConstructionPlanChecks {
    interface Call { void run() throws Exception; }
    static void check(boolean value) {if(!value) throw new AssertionError();}
    static void rejects(Call call,int code) throws Exception {try {call.run();} catch(RuleException e) {if(e.getCode()!=code) throw new AssertionError(e);return;}throw new AssertionError("Expected "+code);}
    public static void main(String[] args) throws Exception {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(),"construction-plan-check"),PubWaylineJobPlanDfEntity.class);
        Map<String,Object> area=new HashMap<>(Map.of("id","area","name","Site","version",1,"enabled",true,"archived",false));
        Map<String,PubWaylineJobPlanDfEntity> catalog=new LinkedHashMap<>();
        Map<String,Map<String,Object>> links=new LinkedHashMap<>(),dispatches=new LinkedHashMap<>();
        List<Map<String,Object>> executionJobs=new ArrayList<>();
        AtomicInteger calls=new AtomicInteger(),failure=new AtomicInteger();
        PubWaylineJobPlanDfMapper mapper=(PubWaylineJobPlanDfMapper)Proxy.newProxyInstance(ConstructionPlanChecks.class.getClassLoader(),new Class[]{PubWaylineJobPlanDfMapper.class},(p,m,a)->{
            if(m.getName().equals("insert")) {PubWaylineJobPlanDfEntity plan=(PubWaylineJobPlanDfEntity)a[0];plan.setId(catalog.size()+1);catalog.put(plan.getPlanId(),plan);return 1;}
            if(m.getName().equals("selectById")) return catalog.values().stream().filter(plan->plan.getId().equals(a[0])).findFirst().orElse(null);
            if(m.getName().equals("selectOne")) {
                com.baomidou.mybatisplus.core.conditions.AbstractWrapper wrapper=(com.baomidou.mybatisplus.core.conditions.AbstractWrapper)a[0];wrapper.getSqlSegment();
                return catalog.values().stream().filter(plan->wrapper.getParamNameValuePairs().containsValue(plan.getPlanId())).findFirst().orElse(null);
            }
            throw new UnsupportedOperationException(m.getName());
        });
        JdbcTemplate db=new JdbcTemplate() {
            @Override public List<Map<String,Object>> queryForList(String sql,Object... a) {
                if(sql.contains("FROM construction_area WHERE")) return "w".equals(a[0])&&"area".equals(a[1])?List.of(area):List.of();
                if(sql.contains("FROM construction_area_wayline")) return List.of(Map.of("file_sign","signature"));
                if(sql.contains("FROM wayline_job j JOIN")) return executionJobs;
                if(sql.contains("FROM wayline_job")) return List.of();
                if(sql.contains("FROM pub_wayline_job_plan_df p JOIN construction_plan")) {
                    List<Map<String,Object>> out=new ArrayList<>();
                    for(PubWaylineJobPlanDfEntity plan:catalog.values()) if(plan.getWorkspaceId().equals(a[0])&&(sql.contains("p.plan_id=?")?plan.getPlanId().equals(a[1]):links.get(plan.getPlanId()).get("area_id").equals(a[1]))) {
                        Map<String,Object> r=new HashMap<>(links.get(plan.getPlanId()));r.put("name",plan.getName());r.put("task_type",plan.getTaskType());r.put("file_id",plan.getFileId());r.put("dock_sn",plan.getDockSn());out.add(r);
                    }return out;
                }
                if(sql.contains("FROM construction_plan_dispatch")) {
                    List<Map<String,Object>> out=new ArrayList<>();
                    for(Map<String,Object> d:dispatches.values()) {
                        if(!Objects.equals(d.get("workspace_id"),a[0])) continue;
                        if(sql.contains("request_id=?")&&!Objects.equals(d.get("request_id"),a[1])) continue;
                        if(sql.contains("plan_id=?")&&!Objects.equals(d.get("plan_id"),a[1])) continue;
                        if(sql.contains("status IN")&&!List.of("SUBMITTING","UNKNOWN").contains(d.get("status"))&&!(a.length>2&&Objects.equals(a[2],1)&&"SUCCEEDED".equals(d.get("status")))) continue;
                        out.add(d);
                    }
                    if(sql.contains("LIMIT 1")&&!out.isEmpty()) return List.of(out.get(out.size()-1));return out;
                }
                if(sql.contains("FROM construction_plan")) {
                    return links.values().stream().filter(r->Objects.equals(r.get("workspace_id"),a[0])&&Objects.equals(r.get(sql.contains("request_id=?")?"request_id":"plan_id"),a[1])).toList();
                }
                throw new UnsupportedOperationException(sql);
            }
            @Override public int update(String sql,Object... a) {
                if(sql.startsWith("INSERT INTO construction_plan (")) {
                    String[] keys={"plan_id","workspace_id","area_id","area_version","file_sign","snapshot_json","request_id","request_hash"};Map<String,Object> r=new HashMap<>();for(int i=0;i<keys.length;i++)r.put(keys[i],a[i]);r.put("archived",0);links.put((String)a[0],r);return 1;
                }
                if(sql.startsWith("INSERT INTO construction_plan_dispatch")) {
                    String[] keys={"id","workspace_id","plan_id","request_id","status","snapshot_json"};Map<String,Object> r=new HashMap<>();for(int i=0;i<keys.length;i++)r.put(keys[i],a[i]);dispatches.put((String)a[3],r);return 1;
                }
                if(sql.startsWith("UPDATE construction_plan_dispatch SET job_id")) {
                    for(Map<String,Object> r:dispatches.values()) if(Objects.equals(r.get("workspace_id"),a[1])&&Objects.equals(r.get("plan_id"),a[2])&&"SUBMITTING".equals(r.get("status"))) {r.put("job_id",a[0]);return 1;}return 0;
                }
                if(sql.startsWith("UPDATE construction_plan_dispatch SET status")) {Map<String,Object> r=dispatches.get(a[4]);r.put("status",a[0]);r.put("job_id",a[1]);r.put("message",a[2]);return 1;}
                if(sql.startsWith("UPDATE construction_plan SET archived")) {links.get(a[1]).put("archived",1);return 1;}
                throw new UnsupportedOperationException(sql);
            }
        };
        ConstructionAreaService areas=new ConstructionAreaService(null,null) {
            @Override public Map<String,Object> detail(String w,String id) {if(!w.equals("w")||!id.equals("area")) throw new RuleException(404,"missing");return area;}
        };
        ConstructionWaylineService routes=new ConstructionWaylineService(null,null,null,null,null) {
            @Override public Map<String,Object> detail(String w,String a,String id) {if(!id.equals("route")) throw new RuleException(404,"unbound");return Map.of("status","VALID","message","valid");}
            @Override public Map<String,Object> validate(String w,String a,String id) {return detail(w,a,id);}
        };
        FrogJumpLandingRecommendationService docks=new FrogJumpLandingRecommendationService(null,null,null) {
            @Override public boolean isWorkspaceDock(String w,String sn) {return w.equals("w")&&sn.equals("dock");}
        };
        ConstructionPlanGuard guard=new ConstructionPlanGuard(db,mapper,areas,routes,docks);
        PubWaylineJobPlanDfService executor=(PubWaylineJobPlanDfService)Proxy.newProxyInstance(ConstructionPlanChecks.class.getClassLoader(),new Class[]{PubWaylineJobPlanDfService.class},(p,m,a)->{
            check(m.getName().equals("expressPlan"));calls.incrementAndGet();
            PubWaylineJobPlanDfEntity actual=guard.resolve("w",(PubWaylineJobPlanDfEntity)a[1]);
            if(failure.get()==1) throw new RuntimeException("Before publish");
            guard.attachJob("w","job-"+calls.get());
            if(failure.get()==2) throw new RuntimeException("Timeout after job creation");
            return HttpResultResponse.success().setMessage("job-"+calls.get());
        });
        AbstractPlatformTransactionManager manager=new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() {return new Object();}
            protected void doBegin(Object t,TransactionDefinition d) {}
            protected void doCommit(DefaultTransactionStatus s) {}
            protected void doRollback(DefaultTransactionStatus s) {}
        };
        ConstructionPlanService service=new ConstructionPlanService(db,mapper,areas,guard,executor,manager);
        CustomClaim user=new CustomClaim();user.setWorkspaceId("w");user.setUsername("pilot");
        Map<String,Object> body=new LinkedHashMap<>(Map.of("name","Plan","file_id","route","dock_sn","dock","task_type",0,"rth_altitude",100,"out_of_control",0,"expected_version",1,"request_id","create-request-00001"));
        Map<String,Object> result=service.create(user,"area",body);String id=(String)result.get("plan_id");
        check(catalog.size()==1&&calls.get()==0&&catalog.get(id).getPlanType()==6);
        check(service.create(user,"area",body).get("plan_id").equals(id)&&catalog.size()==1);
        body.put("name","Different");rejects(()->service.create(user,"area",body),409);body.put("name","Plan");
        body.put("request_id","create-request-00002");body.put("file_id","unbound");rejects(()->service.create(user,"area",body),404);body.put("file_id","route");
        body.put("dock_sn","foreign");rejects(()->service.create(user,"area",body),400);body.put("dock_sn","dock");
        body.put("rth_altitude",20);rejects(()->service.create(user,"area",body),400);body.put("rth_altitude",100);
        check(catalog.size()==1);
        rejects(()->guard.require("foreign",id),404);
        PubWaylineJobPlanDfEntity spoof=new PubWaylineJobPlanDfEntity();spoof.setId(catalog.get(id).getId());spoof.setPlanType(3);spoof.setFileId("foreign");spoof.setDockSn("foreign");
        rejects(()->guard.resolve("w",spoof),400);
        check(guard.dispatch(id,()->guard.resolve("w",spoof)).getFileId().equals("route"));
        PubWaylineJobPlanDfEntity ordinary=new PubWaylineJobPlanDfEntity();ordinary.setPlanType(3);check(guard.resolve("w",ordinary)==ordinary);
        Map<String,Object> req=Map.of("request_id","dispatch-request-001");
        check(service.dispatch(user,id,req).get("status").equals("SUCCEEDED"));
        check(service.dispatch(user,id,req).get("job_id").equals("job-1")&&calls.get()==1);
        area.put("version",2);rejects(()->service.dispatch(user,id,Map.of("request_id","dispatch-request-002")),409);check(calls.get()==1);area.put("version",1);
        area.put("enabled",false);rejects(()->service.dispatch(user,id,Map.of("request_id","dispatch-request-002")),400);area.put("enabled",true);
        links.get(id).put("file_sign","changed");rejects(()->guard.validate(catalog.get(id),true),409);links.get(id).put("file_sign","signature");
        failure.set(1);check(service.dispatch(user,id,Map.of("request_id","dispatch-request-002")).get("status").equals("FAILED"));
        failure.set(2);check(service.dispatch(user,id,Map.of("request_id","dispatch-request-003")).get("status").equals("UNKNOWN"));
        int count=calls.get();service.dispatch(user,id,Map.of("request_id","dispatch-request-003"));check(calls.get()==count);
        rejects(()->service.dispatch(user,id,Map.of("request_id","dispatch-request-004")),409);
        rejects(()->service.archive("w",id),409);
        body.put("task_type",1);body.put("begin_time",System.currentTimeMillis()+3600000);String timed=(String)service.create(user,"area",body).get("plan_id");check(calls.get()==count);
        failure.set(0);service.dispatch(user,timed,Map.of("request_id","dispatch-request-005"));rejects(()->service.dispatch(user,timed,Map.of("request_id","dispatch-request-006")),409);
        Map<String,Object> job=new HashMap<>(Map.of("plan_id",timed,"file_id","route","dock_sn","dock"));executionJobs.add(job);
        catalog.get(timed).setBeginTime(1L);guard.beforeExecute("w","job-timed");
        area.put("version",2);rejects(()->guard.beforeExecute("w","job-timed"),409);area.put("version",1);
        job.put("file_id","tampered");rejects(()->guard.beforeExecute("w","job-timed"),409);
        com.dji.sample.df.electricInspectionDf.service.impl.PubWaylineJobPlanDfServiceImpl legacy=new com.dji.sample.df.electricInspectionDf.service.impl.PubWaylineJobPlanDfServiceImpl();
        try {legacy.createWaylineJObPlan(catalog.get(id));throw new AssertionError();} catch(IllegalArgumentException expected) {}
        System.out.println("Construction plan checks passed: shared plan storage/type, scoped creation, bound routes, device ownership, validation, save without dispatch, idempotent create/dispatch, stale version/sign, legacy spoof protection, ordinary passthrough, timed duplicate rejection, failure versus uncertain outcome. In-memory collaborators; no real flight/database.");
    }
}
