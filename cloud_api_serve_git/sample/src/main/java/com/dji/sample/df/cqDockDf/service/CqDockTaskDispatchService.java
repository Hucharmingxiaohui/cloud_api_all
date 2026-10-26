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

    public CqDockTaskDispatchResult dispatchByBay(String bayId, String taskCode, String taskName,
                                                  String fixedStartTime, boolean reportToPatrol) {
        log.info("开始执行EUA任务: taskCode={}, bayId={}, taskName={}, fixedStartTime={}, reportToPatrol={}",
                taskCode, bayId, taskName, fixedStartTime, reportToPatrol);
        String routeId = resolveRouteId(bayId);
        log.info("EUA任务匹配航线: taskCode={}, bayId={}, routeId={}", taskCode, bayId, routeId);
        if (!StringUtils.hasText(routeId)) {
            String msg = "未在df_uni_point中匹配到航线";
            saveTaskRecord(taskCode, taskName, bayId, null, null, null, msg, null, false, reportToPatrol);
            return CqDockTaskDispatchResult.failed(taskCode, taskName, bayId, null, msg);
        }

        CqAssignTaskRequest request = new CqAssignTaskRequest();
        request.setTaskName(taskName);
        request.setBusinessId(taskCode);
        request.setRouteId(routeId);
        log.info("EUA任务准备调用下级任务下发: taskCode={}, bayId={}, routeId={}, request={}",
                taskCode, bayId, routeId, JSONObject.toJSONString(request));

        CqApiResponse response = cqDockApiService.assignTask(request);
        boolean success = response != null && (Boolean.TRUE.equals(response.getSuccess()) || Objects.equals(response.getCode(), 200));
        String euaTaskId = extractEuaTaskId(response);
        String msg = response == null ? "EUA接口无响应" : response.getMsg();
        saveTaskRecord(taskCode, taskName, bayId, routeId, euaTaskId,
                response == null ? null : response.getCode(), msg,
                response == null ? null : response.getRawBody(), success, reportToPatrol);

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
        List<String> codes = normalizePointCodes(pointCodes);
        resolvePointRoute(codes, subCode);
        return String.join(",", codes);
    }

    public CqDockTaskDispatchResult dispatchByPoints(String pointCodes, String subCode, String taskCode,
                                                    String taskName, String fixedStartTime, boolean reportToPatrol) {
        List<String> codes = normalizePointCodes(pointCodes);
        String snapshot = JSONObject.toJSONString(codes);
        String routeId;
        try {
            routeId = resolvePointRoute(codes, subCode);
        } catch (IllegalArgumentException e) {
            savePointTaskRecord(taskCode, taskName, null, null, null, e.getMessage(), null, false, reportToPatrol, snapshot);
            return CqDockTaskDispatchResult.failed(taskCode, taskName, null, null, e.getMessage());
        }
        CqAssignTaskRequest request = new CqAssignTaskRequest();
        request.setTaskName(taskName);
        request.setBusinessId(taskCode);
        request.setRouteId(routeId);
        CqApiResponse response;
        try {
            response = cqDockApiService.assignTask(request);
        } catch (Exception e) {
            log.error("EUA点位任务下发异常: taskCode={}, pointCodes={}", taskCode, snapshot, e);
            savePointTaskRecord(taskCode, taskName, routeId, null, null, e.getMessage(), null, false, reportToPatrol, snapshot);
            return CqDockTaskDispatchResult.failed(taskCode, taskName, null, routeId, e.getMessage());
        }
        boolean success = response != null && (Boolean.TRUE.equals(response.getSuccess()) || Objects.equals(response.getCode(), 200));
        String euaTaskId = extractEuaTaskId(response);
        String msg = response == null ? "EUA接口无响应" : response.getMsg();
        savePointTaskRecord(taskCode, taskName, routeId, euaTaskId, response == null ? null : response.getCode(),
                msg, response == null ? null : response.getRawBody(), success, reportToPatrol, snapshot);
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

    private void saveTaskRecord(String taskCode, String taskName, String bayId, String routeId,
                                String euaTaskId, Integer responseCode, String responseMsg,
                                String rawResponse, boolean success, boolean reportToPatrol) {
        saveTaskRecord(taskCode, taskName, bayId, routeId, euaTaskId, responseCode, responseMsg,
                rawResponse, success, reportToPatrol, null);
    }

    private void savePointTaskRecord(String taskCode, String taskName, String routeId, String euaTaskId,
                                     Integer responseCode, String responseMsg, String rawResponse, boolean success,
                                     boolean reportToPatrol, String snapshot) {
        saveTaskRecord(taskCode, taskName, null, routeId, euaTaskId, responseCode, responseMsg,
                rawResponse, success, reportToPatrol, snapshot);
    }

    private void saveTaskRecord(String taskCode, String taskName, String bayId, String routeId,
                                String euaTaskId, Integer responseCode, String responseMsg,
                                String rawResponse, boolean success, boolean reportToPatrol, String pointCodes) {
        CqDockTaskRecordEntity record = new CqDockTaskRecordEntity();
        record.setBusinessId(taskCode);
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
        if (pointCodes != null && inserted != 1) {
            throw new IllegalStateException("EUA任务记录保存失败");
        }
    }
}
