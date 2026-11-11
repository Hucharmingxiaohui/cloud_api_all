package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.cqDockDf.dao.CqDockTaskRecordMapper;
import com.dji.sample.df.cqDockDf.model.dto.CqApiResponse;
import com.dji.sample.df.cqDockDf.model.dto.CqAssignTaskRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqDockTaskDispatchResult;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskRecordEntity;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
public class CqDockTaskDispatchService {

    @Resource
    private UniPointMapper2 uniPointMapper2;

    @Resource
    private CqDockApiService cqDockApiService;

    @Resource
    private CqDockTaskRecordMapper cqDockTaskRecordMapper;

    @Resource
    private CqDockTaskStatusHandler cqDockTaskStatusHandler;

    @Resource
    private CqEuaRoutePointService cqEuaRoutePointService;

    public CqDockTaskDispatchResult dispatchPlanPoints(String snapshot, String subCode, String expectedRouteId,
                                                       String expectedRouteName, String taskCode,
                                                       String taskName, String fixedStartTime) {
        CqEuaRoutePointService.Selection selection = cqEuaRoutePointService.resolveSnapshot(
                snapshot, subCode, expectedRouteId, expectedRouteName);
        List<String> codes = cqEuaRoutePointService.parseCodes(selection.getPointCodes());
        return dispatchByPoints(String.join(",", codes), subCode, taskCode, taskName, fixedStartTime,
                false, expectedRouteId);
    }

    public CqDockTaskDispatchResult dispatchByBay(String bayId, String taskCode, String taskName,
                                                  String fixedStartTime, boolean reportToPatrol) {
        return dispatchByBay(bayId, taskCode, taskName, fixedStartTime, reportToPatrol, null);
    }

    public CqDockTaskDispatchResult dispatchByBay(String bayId, String taskCode, String taskName,
                                                  String fixedStartTime, boolean reportToPatrol, String subCode) {
        subCode = StringUtils.hasText(subCode) ? subCode.trim() : null;
        if (reportToPatrol && subCode == null) {
            return CqDockTaskDispatchResult.failed(taskCode, taskName, bayId, null, "上级EUA任务缺少站点Code(subCode)，拒绝下发");
        }
        log.info("开始执行EUA任务: taskCode={}, bayId={}, taskName={}, fixedStartTime={}, reportToPatrol={}",
                taskCode, bayId, taskName, fixedStartTime, reportToPatrol);
        String routeId = resolveRouteId(bayId);
        log.info("EUA任务匹配航线: taskCode={}, bayId={}, routeId={}", taskCode, bayId, routeId);
        if (!StringUtils.hasText(routeId)) {
            String msg = "未在df_uni_point中匹配到航线";
            saveTaskRecord(taskCode, taskName, bayId, null, null, null, msg, null, false, reportToPatrol, null, subCode);
            return CqDockTaskDispatchResult.failed(taskCode, taskName, bayId, null, msg);
        }

        CqAssignTaskRequest request = new CqAssignTaskRequest();
        request.setTaskName(taskName);
        request.setBusinessId(taskCode);
        request.setRouteId(routeId);
        log.info("EUA任务准备调用下级任务下发: taskCode={}, bayId={}, routeId={}, request={}",
                taskCode, bayId, routeId, JSONObject.toJSONString(request));

        CqDockTaskRecordEntity record = saveTaskRecord(taskCode, taskName, bayId, routeId, null, null,
                "待下发", null, false, reportToPatrol, null, subCode);
        CqApiResponse response = cqDockApiService.assignTask(request);
        boolean success = response != null && (Boolean.TRUE.equals(response.getSuccess()) || Objects.equals(response.getCode(), 200));
        String euaTaskId = extractEuaTaskId(response);
        String msg = response == null ? "EUA接口无响应" : response.getMsg();
        updateTaskRecord(record, response, success, euaTaskId, msg);

        CqDockTaskDispatchResult result = new CqDockTaskDispatchResult();
        result.setSuccess(success);
        result.setTaskCode(taskCode);
        result.setTaskName(taskName);
        result.setBayId(bayId);
        result.setRouteId(routeId);
        result.setEuaTaskId(euaTaskId);
        result.setResponseCode(response == null ? null : response.getCode());
        result.setResponseMsg(msg);

        if (success) {
            log.info("EUA任务下发成功: taskCode={}, bayId={}, routeId={}, euaTaskId={}, reportToPatrol={}",
                    taskCode, bayId, routeId, euaTaskId, reportToPatrol);
            cqDockTaskStatusHandler.startMonitoring(taskCode, taskName, euaTaskId, fixedStartTime, reportToPatrol);
        } else {
            log.error("EUA任务下发失败: taskCode={}, bayId={}, routeId={}, code={}, msg={}",
                    taskCode, bayId, routeId, response == null ? null : response.getCode(), msg);
        }
        return result;
    }

    public String resolveRouteId(String bayId) {
        if (!StringUtils.hasText(bayId)) {
            return null;
        }
        return uniPointMapper2.selectList(new LambdaQueryWrapper<UniPoint>()
                        .eq(UniPoint::getBayId, bayId)
                        .isNotNull(UniPoint::getWaylineId))
                .stream()
                .map(UniPoint::getWaylineId)
                .filter(StringUtils::hasText)
                .distinct()
                .findFirst()
                .orElse(null);
    }

    public String validatePointDispatch(String pointCodes, String subCode) {
        if (!StringUtils.hasText(subCode)) {
            throw new IllegalArgumentException("上级EUA任务缺少站点Code(subCode)，拒绝下发");
        }
        List<String> codes = normalizePointCodes(pointCodes);
        resolvePointRoute(codes, subCode.trim());
        return String.join(",", codes);
    }

    public CqDockTaskDispatchResult dispatchByPoints(String pointCodes, String subCode, String taskCode,
                                                    String taskName, String fixedStartTime, boolean reportToPatrol) {
        return dispatchByPoints(pointCodes, subCode, taskCode, taskName, fixedStartTime, reportToPatrol, null);
    }

    private CqDockTaskDispatchResult dispatchByPoints(String pointCodes, String subCode, String taskCode,
                                                    String taskName, String fixedStartTime, boolean reportToPatrol,
                                                    String expectedRouteId) {
        subCode = StringUtils.hasText(subCode) ? subCode.trim() : null;
        if (reportToPatrol && subCode == null) {
            return CqDockTaskDispatchResult.failed(taskCode, taskName, null, null, "上级EUA任务缺少站点Code(subCode)，拒绝下发");
        }
        List<String> codes = normalizePointCodes(pointCodes);
        String snapshot = JSONObject.toJSONString(codes);
        String routeId;
        try {
            routeId = resolvePointRoute(codes, subCode);
            if (expectedRouteId != null) {
                if (!expectedRouteId.equals(routeId)) throw new IllegalArgumentException("计划航线ID已变化，请重新选择点位并保存");
            }
        } catch (IllegalArgumentException e) {
            saveTaskRecord(taskCode, taskName, null, null, null, null, e.getMessage(), null, false, reportToPatrol, snapshot, subCode);
            return CqDockTaskDispatchResult.failed(taskCode, taskName, null, null, e.getMessage());
        }
        CqAssignTaskRequest request = new CqAssignTaskRequest();
        request.setTaskName(taskName);
        request.setBusinessId(taskCode);
        request.setRouteId(routeId);
        CqDockTaskRecordEntity record = saveTaskRecord(taskCode, taskName, null, routeId, null, null,
                "待下发", null, false, reportToPatrol, snapshot, subCode);
        CqApiResponse response;
        try {
            response = cqDockApiService.assignTask(request);
        } catch (Exception e) {
            log.error("EUA点位任务下发异常: taskCode={}, pointCodes={}", taskCode, snapshot, e);
            updateTaskRecord(record, null, false, null, e.getMessage());
            return CqDockTaskDispatchResult.failed(taskCode, taskName, null, routeId, e.getMessage());
        }
        boolean success = response != null && (Boolean.TRUE.equals(response.getSuccess()) || Objects.equals(response.getCode(), 200));
        String euaTaskId = extractEuaTaskId(response);
        String msg = response == null ? "EUA接口无响应" : response.getMsg();
        updateTaskRecord(record, response, success, euaTaskId, msg);
        CqDockTaskDispatchResult result = new CqDockTaskDispatchResult();
        result.setSuccess(success);
        result.setTaskCode(taskCode);
        result.setTaskName(taskName);
        result.setRouteId(routeId);
        result.setEuaTaskId(euaTaskId);
        result.setResponseCode(response == null ? null : response.getCode());
        result.setResponseMsg(msg);
        if (success) {
            cqDockTaskStatusHandler.startMonitoring(taskCode, taskName, euaTaskId, fixedStartTime, reportToPatrol);
        }
        return result;
    }

    private List<String> normalizePointCodes(String pointCodes) {
        Set<String> codes = new LinkedHashSet<>();
        if (pointCodes != null) {
            for (String code : pointCodes.split(",")) {
                if (StringUtils.hasText(code)) {
                    codes.add(code.trim());
                }
            }
        }
        return new ArrayList<>(codes);
    }

    private String resolvePointRoute(List<String> codes, String subCode) {
        if (codes.isEmpty()) {
            throw new IllegalArgumentException("EUA任务点位不能为空");
        }
        List<UniPoint> points = uniPointMapper2.selectList(new LambdaQueryWrapper<UniPoint>()
                .in(UniPoint::getPointCode, codes)
                .eq(StringUtils.hasText(subCode), UniPoint::getSubCode, subCode));
        Map<String, UniPoint> mappings = new HashMap<>();
        for (UniPoint point : points) {
            String code = point.getPointCode();
            if (mappings.putIfAbsent(code, point) != null) {
                throw new IllegalArgumentException("EUA点位映射不唯一: " + code);
            }
        }
        String route = null;
        for (String code : codes) {
            UniPoint point = mappings.get(code);
            if (point == null) {
                throw new IllegalArgumentException("EUA点位不存在: " + code);
            }
            if (!StringUtils.hasText(point.getWaylineId())) {
                throw new IllegalArgumentException("EUA点位未配置航线: " + code);
            }
            String pointRoute = point.getWaylineId().trim();
            if (route != null && !route.equals(pointRoute)) {
                throw new IllegalArgumentException("EUA任务所有点位必须属于同一航线");
            }
            route = pointRoute;
        }
        return route;
    }

    private String extractEuaTaskId(CqApiResponse response) {
        if (response == null || response.getData() == null) {
            return null;
        }
        return response.getData().getString("taskId");
    }

    private CqDockTaskRecordEntity saveTaskRecord(String taskCode, String taskName, String bayId, String routeId,
                                String euaTaskId, Integer responseCode, String responseMsg,
                                String rawResponse, boolean success, boolean reportToPatrol, String pointCodes, String subCode) {
        CqDockTaskRecordEntity record = new CqDockTaskRecordEntity();
        record.setBusinessId(taskCode);
        record.setSubCode(subCode);
        record.setTaskName(taskName);
        record.setBayId(bayId);
        record.setPointCodes(pointCodes);
        record.setRouteId(routeId);
        record.setEuaTaskId(euaTaskId);
        record.setResponseCode(responseCode);
        record.setResponseMsg(responseMsg);
        record.setSuccess(success ? 1 : 0);
        record.setRawResponse(rawResponse);
        record.setReportToPatrol(reportToPatrol ? 1 : 0);
        Date now = new Date();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        int inserted = cqDockTaskRecordMapper.insert(record);
        if (inserted != 1) {
            throw new IllegalStateException("EUA任务记录保存失败");
        }
        return record;
    }

    private void updateTaskRecord(CqDockTaskRecordEntity accepted, CqApiResponse response, boolean success,
                                  String euaTaskId, String message) {
        CqDockTaskRecordEntity update = new CqDockTaskRecordEntity();
        update.setId(accepted.getId());
        update.setEuaTaskId(euaTaskId);
        update.setResponseCode(response == null ? null : response.getCode());
        update.setResponseMsg(message);
        update.setRawResponse(response == null ? null : response.getRawBody());
        update.setSuccess(success ? 1 : 0);
        update.setUpdateTime(new Date());
        if (cqDockTaskRecordMapper.updateById(update) != 1) {
            throw new IllegalStateException("EUA任务响应记录保存失败");
        }
    }
}
