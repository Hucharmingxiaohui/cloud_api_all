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

    private String extractEuaTaskId(CqApiResponse response) {
        if (response == null || response.getData() == null) {
            return null;
        }
        return response.getData().getString("taskId");
    }

    private void saveTaskRecord(String taskCode, String taskName, String bayId, String routeId,
                                String euaTaskId, Integer responseCode, String responseMsg,
                                String rawResponse, boolean success, boolean reportToPatrol) {
        CqDockTaskRecordEntity record = new CqDockTaskRecordEntity();
        record.setBusinessId(taskCode);
        record.setTaskName(taskName);
        record.setBayId(bayId);
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
        cqDockTaskRecordMapper.insert(record);
    }
}
