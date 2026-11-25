package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.cqDockDf.dao.CqEuaPlanMapper;
import com.dji.sample.df.cqDockDf.model.dto.CqDockTaskDispatchResult;
import com.dji.sample.df.cqDockDf.model.entity.CqEuaPlanEntity;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class CqEuaPatrolPlanService {
    @Resource private CqEuaPlanMapper cqEuaPlanMapper;
    @Resource private CqEuaPlanService cqEuaPlanService;
    @Resource private CqDockTaskDispatchService cqDockTaskDispatchService;
    @Resource private UniPointMapper2 uniPointMapper2;

    public CqDockTaskDispatchResult createAndExecute(String pointCodes, String subCode, String taskCode,
            String taskName, String fixedStartTime) {
        return cqEuaPlanService.executePatrolPlan(freeze(pointCodes, subCode, taskCode, taskName), fixedStartTime);
    }

    public CqEuaPlanEntity freeze(String pointCodes, String subCode, String taskCode, String taskName) {
        if (!StringUtils.hasText(subCode) || !StringUtils.hasText(taskCode)
                || taskCode.length() > 128 || subCode.trim().length() > 128) {
            throw new IllegalArgumentException("巡视任务缺少有效站点或原始任务编码");
        }
        subCode = subCode.trim();
        CqEuaPlanEntity plan = find(subCode, taskCode);
        if (plan == null) {
            String normalized = cqDockTaskDispatchService.validatePointDispatch(pointCodes, subCode);
            List<String> codes = Arrays.asList(normalized.split(","));
            List<UniPoint> points = uniPointMapper2.selectList(new LambdaQueryWrapper<UniPoint>()
                    .eq(UniPoint::getSubCode, subCode).in(UniPoint::getPointCode, codes));
            if (points.isEmpty()) throw new IllegalArgumentException("巡视任务点位已变化");
            String route = points.get(0).getWaylineId().trim();
            // Route display binding is optional for upper tasks; dispatch revalidates the real route ID.
            String routeName = points.stream().map(UniPoint::getEuaRouteName)
                    .filter(StringUtils::hasText).findFirst().orElse(null);
            String name = StringUtils.hasText(taskName) ? taskName.trim() : taskCode;
            if (!name.startsWith("巡视任务-")) name = "巡视任务-" + name;
            if (name.length() > 128) name = name.substring(0, 128);
            plan = new CqEuaPlanEntity();
            plan.setPlanSource("PATROL");
            plan.setPatrolTaskCode(taskCode);
            plan.setPlanName(name);
            plan.setSubCode(subCode);
            plan.setRouteId(route);
            plan.setRouteName(routeName);
            plan.setPointCodes(JSON.toJSONString(codes));
            plan.setEnabled(1);
            plan.setCreateTime(new Date());
            plan.setUpdateTime(new Date());
            // Persist and commit the dedupe identity before calling the external assignment API.
            try {
                if (cqEuaPlanMapper.insert(plan) != 1) throw new IllegalStateException("巡视计划保存失败");
            } catch (DuplicateKeyException e) {
                plan = find(subCode, taskCode);
                if (plan == null) throw e;
            }
        }
        String normalized = cqDockTaskDispatchService.validatePointDispatch(pointCodes, subCode);
        if (!JSON.parseArray(plan.getPointCodes(), String.class).equals(Arrays.asList(normalized.split(","))))
            throw new IllegalArgumentException("同一巡视计划禁止覆盖已冻结点位");
        if (!StringUtils.hasText(plan.getRouteId()) || !"PATROL".equals(plan.getPlanSource()))
            throw new IllegalArgumentException("巡视计划来源或航线无效");
        return plan;
    }

    private CqEuaPlanEntity find(String subCode, String taskCode) {
        return cqEuaPlanMapper.selectOne(new LambdaQueryWrapper<CqEuaPlanEntity>()
                .eq(CqEuaPlanEntity::getSubCode, subCode)
                .eq(CqEuaPlanEntity::getPatrolTaskCode, taskCode)
                .eq(CqEuaPlanEntity::getPlanSource, "PATROL"));
    }
}
