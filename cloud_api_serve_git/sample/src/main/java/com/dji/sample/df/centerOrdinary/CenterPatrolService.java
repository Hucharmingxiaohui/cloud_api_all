package com.dji.sample.df.centerOrdinary;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dji.sample.center.config.SwitchConfig;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.center.v2022.command.control.CenterTaskCommandItem;
import com.dji.sample.df.centerOrdinary.dao.*;
import com.dji.sample.df.cqDockDf.service.CqEuaPatrolPlanService;
import com.dji.sample.df.cqDockDf.service.CqEuaPlanService;
import com.dji.sample.df.cqDockDf.model.entity.CqEuaPlanEntity;
import com.dji.sample.df.manageDf.dao.IWorkspaceMapperDf;
import com.dji.sample.df.manageDf.model.entity.WorkspaceEntity;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import java.util.*;

/** Plan lock serializes admission and enable/disable; flight calls occur only after a committed claim. */
@Service
@Slf4j
public class CenterPatrolService {
    private final CenterPatrolPlanMapper plans;
    private final CenterOrdinaryTaskMapper tasks;
    private final CenterOrdinaryTaskService ordinary;
    private final PubWaylineJobPlanDfMapper templates;
    private final UniPointMapper2 points;
    private final SwitchConfig config;
    private final TransactionTemplate tx;
    private final CqEuaPatrolPlanService euaPatrol;
    private final CqEuaPlanService euaPlans;
    private final IWorkspaceMapperDf workspaces;
    @Value("${solar.center-plan-prefix:上级}")
    private String solarCenterPlanPrefix = "上级";

    public CenterPatrolService(CenterPatrolPlanMapper plans, CenterOrdinaryTaskMapper tasks,
            @Lazy CenterOrdinaryTaskService ordinary, PubWaylineJobPlanDfMapper templates, UniPointMapper2 points,
            CqEuaPatrolPlanService euaPatrol, CqEuaPlanService euaPlans, IWorkspaceMapperDf workspaces,
            SwitchConfig config, PlatformTransactionManager manager) {
        this.plans=plans; this.tasks=tasks; this.ordinary=ordinary; this.templates=templates;
        this.points=points; this.config=config; this.tx=new TransactionTemplate(manager);
        this.euaPatrol=euaPatrol; this.euaPlans=euaPlans; this.workspaces=workspaces;
        this.tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public CenterOrdinaryTaskEntity accept(CenterTaskCommandItem c, String sub, String center) {
        if (!StringUtils.hasText(center) || !StringUtils.hasText(sub) || !StringUtils.hasText(c.getTask_code()))
            throw new IllegalArgumentException("上级中心、厂站及任务编码不能为空");
        int state = state(c.getIsenable());
        return tx.execute(s -> {
            plans.ensure(center, sub, c.getTask_code());
            CenterPatrolPlan p=plans.lockIdentity(center, sub, c.getTask_code());
            if (state!=0) {
                changeLocked(p, state);
                return p.getExecutionId()==null ? null : tasks.selectById(p.getExecutionId());
            }
            if (Integer.valueOf(2).equals(p.getIsenable())) throw new IllegalArgumentException("上级计划已删除，不可重新启用");
            long time=ordinary.parseTime(c.getFixed_start_time());
            CenterOrdinaryTaskEntity old=tasks.findIdentity(sub, c.getTask_code(), time);
            if (old!=null) {
                if (!Objects.equals(old.getUpperPlanId(), p.getId())) throw new IllegalArgumentException("跨中心任务标识冲突，拒绝覆盖");
                CenterTaskCommandItem previous=JSON.parseObject(p.getCommandJson(), CenterTaskCommandItem.class);
                if(previous==null || !Objects.equals(p.getExecutionId(),old.getId())) throw new IllegalArgumentException("历史安排不能覆盖当前安排");
                if (!Objects.equals(previous.getDevice_list(),c.getDevice_list()) || previous.getDevice_level()!=c.getDevice_level()
                        || !Objects.equals(old.getTaskName(),c.getTask_name())) throw new IllegalArgumentException("同次安排禁止覆盖已冻结配置");
                if ("DISABLED".equals(old.getStatus())) {
                    if (time<=System.currentTimeMillis()) throw new IllegalArgumentException("安排已过期，禁止补飞");
                    tasks.finish(old.getId(), "DISABLED", "SCHEDULED", "重新启用原安排，保留快照");
                }
                p.setIsenable(0); plans.updateById(p);
                return tasks.selectById(old.getId());
            }
            if (p.getExecutionId()!=null) {
                CenterOrdinaryTaskEntity current=tasks.selectById(p.getExecutionId());
                if (current!=null && Arrays.asList("SCHEDULED","DISABLED","DISPATCHING","UNKNOWN").contains(current.getStatus()))
                    throw new IllegalArgumentException("已有未消费或不确定安排，请先核验；禁止覆盖");
            }
            CenterOrdinaryTaskEntity record=freeze(c,sub);
            record.setUpperPlanId(p.getId());
            if (tasks.insert(record)!=1) throw new IllegalStateException("上级执行安排保存失败");
            p.setTaskName(c.getTask_name()); p.setCommandJson(JSON.toJSONString(c)); p.setIsenable(0);
            p.setWorkspaceId(record.getWorkspaceId()); p.setExecutionId(record.getId());
            if (plans.updateById(p)!=1) throw new IllegalStateException("上级计划保存失败");
            log.info("上级巡视计划已冻结: planId={}, taskCode={}, executionRecordId={}, planType={}",p.getId(),p.getTaskCode(),record.getId(),record.getPlanType());
            return record;
        });
    }

    private CenterOrdinaryTaskEntity freeze(CenterTaskCommandItem c,String sub) {
        if (!StringUtils.hasText(c.getTask_name())) throw new IllegalArgumentException("任务名称不能为空");
        if (c.getDevice_level()==3 && ordinary.enabled()
                && (c.getDevice_list()==null || c.getDevice_list().contains(",") || !c.getDevice_list().trim().endsWith("-bay")))
            return ordinary.freeze(c,sub);
        String device=c.getDevice_list();
        int type;
        QueryWrapper<PubWaylineJobPlanDfEntity> q=new QueryWrapper<>();
        String snapshot="[]", codes="[]", route=null;
        if (c.getDevice_level()==3 && ("true".equalsIgnoreCase(config.getCenterCqDockTaskEnable()) || "1".equals(config.getCenterCqDockTaskEnable()))) {
            return freezeEua(c,sub);
        } else {
            if (!StringUtils.hasText(device) || device.contains(",")) throw new IllegalArgumentException("此任务类型仅支持单设备或间隔");
            device=device.trim();
            if(c.getDevice_level()==2) {type=1; q.eq("fan_id",device);}
            else if(c.getDevice_level()==1 && device.endsWith("-bay")) {
                type=4; q.eq("orthophoto_id",device.substring(0,device.length()-4))
                        .likeRight(StringUtils.hasText(solarCenterPlanPrefix),"name",solarCenterPlanPrefix);
            }
            else if(c.getDevice_level()==1) {
                type=0;
                UniPoint selected=points.selectOne(new QueryWrapper<UniPoint>().eq("sub_code",sub).eq("bay_id",device)
                        .orderByDesc("id").last("LIMIT 1"));
                if(selected==null || !StringUtils.hasText(selected.getWaylineId())) throw new IllegalArgumentException("间隔航线不存在");
                route=selected.getWaylineId(); q.eq("file_id",route);
            } else throw new IllegalArgumentException("不支持的上级任务类型");
        }
        // 风机/光伏沿用设备或正射图范围；厂站编码只用于已验证的点位选择，不额外排除历史模板。
        PubWaylineJobPlanDfEntity template=templates.selectOne(q.eq("task_type",0).eq("plan_type",type).orderByDesc("create_time","id").last("LIMIT 1"));
        if(template==null || !StringUtils.hasText(template.getWorkspaceId()) || !StringUtils.hasText(template.getPlanId()))
            throw new IllegalArgumentException("没有匹配原设备范围及正确类型的本地立即计划模板");
        if (!StringUtils.hasText(template.getFileId()) || !StringUtils.hasText(template.getDockSn())
                || template.getWaylineType()==null || template.getWaylineType()<0 || template.getWaylineType()>3
                || template.getOutOfControl()==null || template.getOutOfControl()<0 || template.getOutOfControl()>2
                || template.getRthAltitude()==null)
            throw new IllegalArgumentException("最新本地计划缺少有效航线、机巢、航线类型、失控动作或返航高度");
        long time=ordinary.parseTime(c.getFixed_start_time());
        if(time<=System.currentTimeMillis()) throw new IllegalArgumentException("定时时间已过期，禁止补飞");
        CenterOrdinaryTaskEntity r=new CenterOrdinaryTaskEntity();
        r.setSubCode(sub);r.setTaskCode(c.getTask_code());r.setTaskName(c.getTask_name());r.setScheduledTime(time);
        r.setFixedStartTime(c.getFixed_start_time());r.setStatus("SCHEDULED");r.setPlanType(type);
        r.setWorkspaceId(template.getWorkspaceId());r.setPlanId(template.getPlanId());r.setRouteId(template.getFileId());
        r.setPlanConfig(JSON.toJSONString(template));r.setPointCodes(codes);r.setPointSnapshot(snapshot);
        return r;
    }

    private CenterOrdinaryTaskEntity freezeEua(CenterTaskCommandItem c,String sub) {
        long time=ordinary.parseTime(c.getFixed_start_time());
        if(time<=System.currentTimeMillis()) throw new IllegalArgumentException("定时时间已过期，禁止补飞");
        List<WorkspaceEntity> available=workspaces.selectList(new QueryWrapper<WorkspaceEntity>().last("LIMIT 2"));
        if(available.size()!=1 || !StringUtils.hasText(available.get(0).getWorkspaceId()))
            throw new IllegalArgumentException("EUA工作空间不存在或不唯一，禁止猜测任务归属");
        CqEuaPlanEntity snapshot=euaPatrol.freeze(c.getDevice_list(),sub,c.getTask_code(),c.getTask_name());
        if(snapshot.getId()==null || !"PATROL".equals(snapshot.getPlanSource())
                || !Objects.equals(sub,snapshot.getSubCode()) || !Objects.equals(c.getTask_code(),snapshot.getPatrolTaskCode())
                || !StringUtils.hasText(snapshot.getRouteId()) || !StringUtils.hasText(snapshot.getPointCodes()))
            throw new IllegalArgumentException("EUA巡视计划快照无效");
        if(StringUtils.hasText(snapshot.getLastTaskCode()))
            throw new IllegalArgumentException("EUA巡视计划已领取，请核验原执行，禁止以新安排重复下发");
        CenterOrdinaryTaskEntity r=new CenterOrdinaryTaskEntity();
        r.setSubCode(sub);r.setTaskCode(c.getTask_code());r.setTaskName(c.getTask_name());r.setScheduledTime(time);
        r.setFixedStartTime(c.getFixed_start_time());r.setStatus("SCHEDULED");r.setPlanType(5);
        r.setWorkspaceId(available.get(0).getWorkspaceId());r.setPlanId(snapshot.getId().toString());r.setRouteId(snapshot.getRouteId());
        r.setPlanConfig(JSON.toJSONString(snapshot));r.setPointCodes(snapshot.getPointCodes());r.setPointSnapshot(snapshot.getPointCodes());
        log.info("上级EUA计划快照已冻结: taskCode={}, euaPlanId={}, routeId={}, workspaceId={}",r.getTaskCode(),snapshot.getId(),r.getRouteId(),r.getWorkspaceId());
        return r;
    }

    public void change(long id,int state,String workspace) {
        if(state<0 || state>2) throw new IllegalArgumentException("无效启停状态");
        tx.execute(s->{CenterPatrolPlan p=plans.lock(id);
            if(p==null || !Objects.equals(workspace,p.getWorkspaceId())) throw new IllegalArgumentException("上级计划不存在或无工作空间权限");
            changeLocked(p,state); return null;});
    }

    private void changeLocked(CenterPatrolPlan p,int state) {
        if(Integer.valueOf(2).equals(p.getIsenable())) {
            if(state==2) return;
            throw new IllegalArgumentException("已删除计划不可操作");
        }
        if(p.getExecutionId()!=null) {
            CenterOrdinaryTaskEntity r=tasks.selectById(p.getExecutionId());
            if(state==0 && r!=null && "DISABLED".equals(r.getStatus())) {
                if(r.getScheduledTime()<=System.currentTimeMillis()) throw new IllegalArgumentException("安排已过期，禁止补飞");
                tasks.finish(r.getId(),"DISABLED","SCHEDULED","重新启用原安排");
            } else if(state!=0 && r!=null) {
                tasks.finish(r.getId(),"SCHEDULED",state==2?"DELETED":"DISABLED","禁止未开始安排；不代表停止正在飞行任务");
                if(state==2) tasks.finish(r.getId(),"DISABLED","DELETED","软删除未开始安排");
            }
        }
        p.setIsenable(state); if(plans.updateById(p)!=1) throw new IllegalStateException("启停保存失败");
        log.info("上级巡视计划状态变更: planId={}, isenable={}；仅阻止未领取安排，不停止飞行",p.getId(),state);
    }

    public CenterOrdinaryTaskEntity immediate(String center,String code) {
        List<CenterPatrolPlan> candidates=plans.controlCandidates(center,code);
        if(candidates.size()!=1) throw new IllegalArgumentException("任务未下发或跨厂站编码歧义，拒绝立即执行");
        CenterPatrolPlan p=candidates.get(0);
        if(!Integer.valueOf(0).equals(p.getIsenable()) || p.getExecutionId()==null) throw new IllegalArgumentException("任务未启用或无安排");
        return dispatch(p.getExecutionId(),true);
    }

    public CenterOrdinaryTaskEntity dispatch(long id,boolean immediate) {
        CenterOrdinaryTaskEntity claimed=tx.execute(s->{
            CenterOrdinaryTaskEntity first=tasks.selectById(id);
            if(first==null || first.getUpperPlanId()==null) return null;
            CenterPatrolPlan p=plans.lock(first.getUpperPlanId());
            if(p==null || !Integer.valueOf(0).equals(p.getIsenable()) || !Objects.equals(p.getExecutionId(),id)) return null;
            int count=immediate?tasks.claimImmediate(id,System.currentTimeMillis()):tasks.claim(id,System.currentTimeMillis());
            return count==1?tasks.selectById(id):null;
        });
        if(claimed!=null) {
            log.info("上级巡视安排已原子领取: recordId={}, immediate={}",id,immediate);
            if(!immediate && System.currentTimeMillis()-claimed.getScheduledTime()>60_000L)
                tasks.finish(id,"DISPATCHING","EXPIRED","错过定时时间超过60秒，禁止补飞");
            else if(Integer.valueOf(5).equals(claimed.getPlanType())) executeEua(claimed);
            else ordinary.executeClaimed(claimed,immediate);
        }
        CenterOrdinaryTaskEntity result=tasks.selectById(id);
        if(immediate && (result==null || !StringUtils.hasText(result.executionId())
                || !Arrays.asList("DISPATCHED","REPORTING","COMPLETED").contains(result.getStatus())))
            throw new IllegalStateException("安排已领取或执行不确定，禁止重复起飞");
        return result;
    }

    private void executeEua(CenterOrdinaryTaskEntity r) {
        try {
            CqEuaPlanEntity snapshot=JSON.parseObject(r.getPlanConfig(),CqEuaPlanEntity.class);
            if(snapshot==null || snapshot.getId()==null || !snapshot.getId().toString().equals(r.getPlanId())
                    || !Objects.equals(snapshot.getPatrolTaskCode(),r.getTaskCode()) || !Objects.equals(snapshot.getSubCode(),r.getSubCode())
                    || !Objects.equals(snapshot.getRouteId(),r.getRouteId()) || !Objects.equals(snapshot.getPointCodes(),r.getPointCodes()))
                throw new IllegalStateException("EUA执行与冻结计划不一致");
            var response=euaPlans.executePatrolPlan(snapshot,r.getFixedStartTime(),prepared->{
                if(prepared.getId()==null || !Objects.equals(prepared.getBusinessId(),r.getTaskCode())
                        || !Objects.equals(prepared.getPlanId(),snapshot.getId())
                        || tasks.bindEuaRecord(r.getId(),prepared.getId())!=1)
                    throw new IllegalStateException("EUA独立执行记录关联失败，尚未调用下级");
            });
            // 无领取回调意味着独立PATROL计划已被其他链路消费，不可冒领历史执行。
            CenterOrdinaryTaskEntity bound=tasks.selectById(r.getId());
            if(response==null || !response.isSuccess() || !StringUtils.hasText(response.getEuaTaskId())
                    || bound.getEuaRecordId()==null || tasks.euaDispatched(r.getId(),response.getEuaTaskId())!=1)
                throw new IllegalStateException("EUA下发或真实任务ID关联未确认");
            log.info("上级EUA执行已关联: taskCode={}, recordId={}, euaRecordId={}, euaTaskId={}",r.getTaskCode(),r.getId(),bound.getEuaRecordId(),response.getEuaTaskId());
        } catch(Exception ex) {
            tasks.finish(r.getId(),"DISPATCHING","UNKNOWN","EUA下发或关联未确认，需人工核验，禁止自动重飞");
            log.error("上级EUA执行未确认，按UNKNOWN隔离；即使仅提交准备记录后中断，也必须人工核验，不自动重试或重新发布: taskCode={}, recordId={}",r.getTaskCode(),r.getId(),ex);
        }
    }

    private int state(String state) {
        if(!Arrays.asList("0","1","2").contains(state)) throw new IllegalArgumentException("isenable必须为0启用、1停用、2删除");
        return Integer.parseInt(state);
    }
}
