package com.dji.sample.df.cqDockDf.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.cqDockDf.dao.CqEuaPlanMapper;
import com.dji.sample.df.cqDockDf.model.dto.CqDockTaskDispatchResult;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaBayOption;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaPlanAddRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaPlanPageRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaPlanUpdateRequest;
import com.dji.sample.df.cqDockDf.model.entity.CqEuaPlanEntity;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CqEuaPlanService {

    @Resource
    private CqEuaPlanMapper cqEuaPlanMapper;

    @Resource
    private UniPointMapper2 uniPointMapper2;

    @Resource
    private CqDockTaskDispatchService cqDockTaskDispatchService;

    public IPage<CqEuaPlanEntity> page(CqEuaPlanPageRequest request) {
        int page = request.getPage() == null || request.getPage() <= 0 ? 1 : request.getPage();
        int pageSize = request.getPageSize() == null || request.getPageSize() <= 0 ? 10 : request.getPageSize();
        LambdaQueryWrapper<CqEuaPlanEntity> wrapper = new LambdaQueryWrapper<CqEuaPlanEntity>()
                .like(StringUtils.hasText(request.getPlanName()), CqEuaPlanEntity::getPlanName, request.getPlanName())
                .like(StringUtils.hasText(request.getBayName()), CqEuaPlanEntity::getBayName, request.getBayName())
                .orderByDesc(CqEuaPlanEntity::getCreateTime)
                .orderByDesc(CqEuaPlanEntity::getId);
        return cqEuaPlanMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public CqEuaPlanEntity add(CqEuaPlanAddRequest request) {
        validatePlan(request.getPlanName(), request.getBayId());
        CqEuaBayOption option = findBayOption(request.getBayId());
        if (option == null) {
            throw new IllegalArgumentException("未找到可用间隔或间隔未匹配航线");
        }
        Date now = new Date();
        CqEuaPlanEntity entity = new CqEuaPlanEntity();
        entity.setPlanName(request.getPlanName().trim());
        entity.setBayId(option.getBayId());
        entity.setBayName(option.getBayName());
        entity.setRouteId(option.getRouteId());
        entity.setRemark(request.getRemark());
        entity.setEnabled(1);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        cqEuaPlanMapper.insert(entity);
        return entity;
    }

    public CqEuaPlanEntity update(CqEuaPlanUpdateRequest request) {
        if (request.getId() == null) {
            throw new IllegalArgumentException("计划ID不能为空");
        }
        CqEuaPlanEntity entity = cqEuaPlanMapper.selectById(request.getId());
        if (entity == null) {
            throw new IllegalArgumentException("计划不存在");
        }
        validatePlan(request.getPlanName(), request.getBayId());
        CqEuaBayOption option = findBayOption(request.getBayId());
        if (option == null) {
            throw new IllegalArgumentException("未找到可用间隔或间隔未匹配航线");
        }
        entity.setPlanName(request.getPlanName().trim());
        entity.setBayId(option.getBayId());
        entity.setBayName(option.getBayName());
        entity.setRouteId(option.getRouteId());
        entity.setRemark(request.getRemark());
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        entity.setUpdateTime(new Date());
        cqEuaPlanMapper.updateById(entity);
        return entity;
    }

    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("计划ID不能为空");
        }
        cqEuaPlanMapper.deleteById(id);
    }

    public CqDockTaskDispatchResult execute(Long planId) {
        if (planId == null) {
            throw new IllegalArgumentException("计划ID不能为空");
        }
        CqEuaPlanEntity plan = cqEuaPlanMapper.selectById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("计划不存在");
        }
        if (plan.getEnabled() != null && plan.getEnabled() == 0) {
            throw new IllegalArgumentException("计划已停用");
        }
        String taskCode = "EUA_PLAN_" + plan.getId() + "_" + System.currentTimeMillis();
        String fixedStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        CqDockTaskDispatchResult result = cqDockTaskDispatchService.dispatchByBay(plan.getBayId(), taskCode, plan.getPlanName(), fixedStartTime, false);
        plan.setLastTaskCode(taskCode);
        plan.setLastEuaTaskId(result.getEuaTaskId());
        plan.setLastExecuteTime(new Date());
        plan.setLastExecuteStatus(result.isSuccess() ? 2 : 0);
        plan.setLastExecuteMsg(result.getResponseMsg());
        plan.setRouteId(result.getRouteId() == null ? plan.getRouteId() : result.getRouteId());
        plan.setUpdateTime(new Date());
        cqEuaPlanMapper.updateById(plan);
        return result;
    }

    public List<CqEuaBayOption> bayOptions() {
        List<UniPoint> points = uniPointMapper2.selectList(new LambdaQueryWrapper<UniPoint>()
                .isNotNull(UniPoint::getBayId)
                .isNotNull(UniPoint::getWaylineId)
                .orderByAsc(UniPoint::getBayName)
                .orderByAsc(UniPoint::getBayId));
        Map<String, CqEuaBayOption> optionMap = new LinkedHashMap<>();
        for (UniPoint point : points) {
            if (!StringUtils.hasText(point.getBayId()) || !StringUtils.hasText(point.getWaylineId())) {
                continue;
            }
            optionMap.computeIfAbsent(point.getBayId(), bayId -> {
                CqEuaBayOption option = new CqEuaBayOption();
                option.setBayId(bayId);
                option.setBayName(StringUtils.hasText(point.getBayName()) ? point.getBayName() : bayId);
                option.setRouteId(point.getWaylineId());
                return option;
            });
        }
        return new ArrayList<>(optionMap.values());
    }

    private CqEuaBayOption findBayOption(String bayId) {
        if (!StringUtils.hasText(bayId)) {
            return null;
        }
        return bayOptions().stream()
                .filter(option -> bayId.equals(option.getBayId()))
                .findFirst()
                .orElse(null);
    }

    private void validatePlan(String planName, String bayId) {
        if (!StringUtils.hasText(planName)) {
            throw new IllegalArgumentException("计划名称不能为空");
        }
        if (!StringUtils.hasText(bayId)) {
            throw new IllegalArgumentException("间隔不能为空");
        }
    }
}
