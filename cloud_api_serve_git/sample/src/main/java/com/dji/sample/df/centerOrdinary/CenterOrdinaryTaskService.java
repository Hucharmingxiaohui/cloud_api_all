package com.dji.sample.df.centerOrdinary;

import com.alibaba.fastjson.JSON;
import com.dji.sample.df.centerOrdinary.dao.CenterOrdinaryTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dji.sample.center.config.SwitchConfig;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.center.v2022.command.control.CenterTaskCommandItem;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.dji.sample.df.uavCommonHandleDf.handler.JobControlHandler;
import com.dji.sdk.common.HttpResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.text.*;
import java.time.Clock;
import java.util.*;

@Slf4j
@Service
public class CenterOrdinaryTaskService {
    private final CenterOrdinaryTaskMapper tasks;
    private final UniPointMapper2 points;
    private final PubWaylineJobPlanDfMapper plans;
    private final PubWaylineJobPlanDfService execution;
    private final SwitchConfig config;
    private final Clock clock;

    @Autowired
    public CenterOrdinaryTaskService(CenterOrdinaryTaskMapper tasks, UniPointMapper2 points,
            PubWaylineJobPlanDfMapper plans, PubWaylineJobPlanDfService execution, SwitchConfig config) {
        this(tasks, points, plans, execution, config, Clock.systemDefaultZone());
    }

    CenterOrdinaryTaskService(CenterOrdinaryTaskMapper tasks, UniPointMapper2 points,
            PubWaylineJobPlanDfMapper plans, PubWaylineJobPlanDfService execution, SwitchConfig config, Clock clock) {
        this.tasks=tasks; this.points=points; this.plans=plans; this.execution=execution; this.config=config; this.clock=clock;
    }

    public boolean enabled() {
        return "true".equalsIgnoreCase(config.getCenterOrdinaryTaskEnable()) || "1".equals(config.getCenterOrdinaryTaskEnable());
    }

    public CenterOrdinaryTaskEntity accept(CenterTaskCommandItem command, String subCode) {
        if (!enabled()) throw new IllegalStateException("上级普通航线点位任务开关未开启");
        if (!StringUtils.hasText(subCode) || !StringUtils.hasText(command.getTask_code()) || !StringUtils.hasText(command.getTask_name()))
            throw new IllegalArgumentException("厂站、上级任务编码及名称不能为空");
        if (command.getDevice_level()!=1) throw new IllegalArgumentException("仅支持区域普通航线点位任务");
        List<String> codes = CenterOrdinarySelection.codes(command.getDevice_list());
        long time = parseTime(command.getFixed_start_time());
        String codesJson = JSON.toJSONString(codes);
        CenterOrdinaryTaskEntity old = tasks.findIdentity(subCode, command.getTask_code(), time);
        if (old != null) return same(old, command, codesJson);
        if (time <= clock.millis()) throw new IllegalArgumentException("定时时间已过期，拒绝立即起飞");
        if (!"1".equals(command.getIsenable())) throw new IllegalArgumentException("仅接受已启用的固定时间任务");
        List<UniPoint> selected = points.selectList(new QueryWrapper<UniPoint>().eq("sub_code", subCode).in("point_code", codes).orderByAsc("point_code"));
        CenterOrdinarySelection.freeze(command.getDevice_list(), subCode, selected);
        String route = selected.get(0).getWaylineId();
        List<UniPoint> routePoints = points.selectList(new QueryWrapper<UniPoint>().eq("wayline_id", route));
        String snapshot = CenterOrdinarySelection.freezeRoute(command.getDevice_list(), subCode, selected, routePoints);
        PubWaylineJobPlanDfEntity plan = plans.selectOne(new QueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq("file_id", route).eq("plan_type", 3).orderByDesc("create_time", "id").last("LIMIT 1"));
        if (plan == null || !Objects.equals(plan.getPlanType(), 3) || !route.equals(plan.getFileId())
                || !StringUtils.hasText(plan.getPlanId()) || !StringUtils.hasText(plan.getWorkspaceId()))
            throw new IllegalArgumentException("该航线没有可用的普通航线计划(plan_type=3)");
        if (!StringUtils.hasText(plan.getDockSn()) || plan.getWaylineType() == null
                || plan.getWaylineType()<0 || plan.getWaylineType()>3 || plan.getOutOfControl()==null
                || plan.getOutOfControl()<0 || plan.getOutOfControl()>2 || plan.getRthAltitude()==null)
            throw new IllegalArgumentException("最新普通航线计划缺少有效机巢、航线类型、失控动作或返航高度");
        CenterOrdinaryTaskEntity task = new CenterOrdinaryTaskEntity();
        task.setSubCode(subCode); task.setTaskCode(command.getTask_code()); task.setTaskName(command.getTask_name());
        task.setScheduledTime(time); task.setFixedStartTime(command.getFixed_start_time()); task.setStatus("SCHEDULED");
        task.setRouteId(route); task.setPlanId(plan.getPlanId()); task.setPlanConfig(JSON.toJSONString(plan));
        task.setPointCodes(codesJson); task.setPointSnapshot(snapshot);
        try {
            if (tasks.insert(task) != 1) throw new IllegalStateException("上级普通任务保存失败");
            return task;
        } catch (DuplicateKeyException e) {
            old = tasks.findIdentity(subCode, command.getTask_code(), time);
            if (old == null) throw e;
            return same(old, command, codesJson);
        }
    }

    private CenterOrdinaryTaskEntity same(CenterOrdinaryTaskEntity old, CenterTaskCommandItem c, String codes) {
        if (!codes.equals(old.getPointCodes()) || !Objects.equals(c.getTask_name(), old.getTaskName()) || !"1".equals(c.getIsenable()))
            throw new IllegalArgumentException("同一上级任务和定时时间已存在，禁止覆盖点位快照");
        return old;
    }

    private long parseTime(String text) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        Date date = text == null ? null : format.parse(text, position);
        if (date == null || position.getIndex()!=text.length() || !format.format(date).equals(text))
            throw new IllegalArgumentException("fixed_start_time必须为yyyy-MM-dd HH:mm:ss本地协议时间");
        return date.getTime();
    }

    @Scheduled(fixedDelay = 1000)
    public void scan() {
        if (!enabled()) return;
        try {
            tasks.quarantine(clock.millis()-10*60_000L);
            tasks.quarantineReports(clock.millis()-24*60*60_000L);
            for (CenterOrdinaryTaskEntity task : tasks.due(clock.millis())) dispatch(task);
            for (CenterOrdinaryTaskEntity task : tasks.monitoring()) JobControlHandler.ensureCenterOrdinaryMonitoring(task.getJobId(), task.getTaskName());
        } catch (Exception e) {
            log.error("上级普通任务扫描失败（请确认已部署独立任务表）", e);
        }
    }

    public void dispatch(CenterOrdinaryTaskEntity task) {
        long now = clock.millis();
        if (!enabled() || tasks.claim(task.getId(), now)!=1) return;
        if (now-task.getScheduledTime()>60_000L) {
            tasks.finish(task.getId(), "DISPATCHING", "EXPIRED", "错过定时时间超过60秒，禁止补飞");
            return;
        }
        try {
            PubWaylineJobPlanDfEntity plan = JSON.parseObject(task.getPlanConfig(), PubWaylineJobPlanDfEntity.class);
            if (!Objects.equals(plan.getPlanType(), 3) || !Objects.equals(task.getRouteId(), plan.getFileId())
                    || !Objects.equals(task.getPlanId(), plan.getPlanId())) throw new IllegalStateException("计划快照与任务航线不一致");
            // 到时新建执行，不能把模板原来的时间/周期重新下发或修改模板本身。
            plan.setId(null); plan.setTaskType(0); plan.setBeginTime(task.getScheduledTime());
            plan.setCenterOrdinaryTaskId(task.getId());
            CustomClaim user = new CustomClaim(); user.setWorkspaceId(plan.getWorkspaceId()); user.setUsername(plan.getUsername());
            HttpResultResponse response = execution.expressPlan(user, plan, task.getTaskName());
            if (response == null || response.getCode()!=0 || !StringUtils.hasText(response.getMessage())) {
                tasks.finish(task.getId(), "DISPATCHING", "UNKNOWN", "发布未确认，禁止自动重试");
                return;
            }
            if (tasks.dispatched(task.getId(), response.getMessage())!=1) throw new IllegalStateException("新执行任务关联保存未确认");
            JobControlHandler.startMonitoringTask(response.getMessage(), task.getTaskName());
        } catch (Exception e) {
            tasks.finish(task.getId(), "DISPATCHING", "UNKNOWN", "发布或关联异常，需人工核验，禁止自动重试");
            log.error("上级普通任务发布状态不确定: recordId={}", task.getId(), e);
        }
    }

    public CenterOrdinaryTaskEntity findJob(String jobId) {
        return enabled() && StringUtils.hasText(jobId) ? tasks.findJob(jobId) : null;
    }
}
