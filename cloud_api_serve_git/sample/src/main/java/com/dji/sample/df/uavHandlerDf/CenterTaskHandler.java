package com.dji.sample.df.uavHandlerDf;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.alibaba.fastjson.JSONObject;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.center.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.cqDockDf.dao.CqDockTaskRecordMapper;
import com.dji.sample.df.cqDockDf.model.dto.CqApiResponse;
import com.dji.sample.df.cqDockDf.model.dto.CqAssignTaskRequest;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskRecordEntity;
import com.dji.sample.df.cqDockDf.service.CqDockApiService;
import com.dji.sample.df.cqDockDf.service.CqDockTaskStatusHandler;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sdk.common.HttpResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@Slf4j
public class CenterTaskHandler {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;
    @Autowired
    PubWaylineJobPlanDfService pubWaylineJobPlanDfService;
    @Autowired
    private IWaylineJobMapper waylineJobMapper;
    @Autowired
    UniPointMapper2 uniPointMapper2;
    @Autowired
    private CqDockApiService cqDockApiService;
    @Autowired
    private CqDockTaskRecordMapper cqDockTaskRecordMapper;
    @Autowired
    private CqDockTaskStatusHandler cqDockTaskStatusHandler;

    // 使用有序集合存储定时任务，score为执行时间戳
    private static final String TASK_SCHEDULE_ZSET = "task_schedule:zset";
    private static final String TASK_DETAIL_HASH = "task_schedule:hash";

    /**
     * 添加定时任务到Redis
     */
    public void addScheduledTask(Integer planType,String taskCode, String fixedStartTime,
                                 String deviceId, String taskName) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date scheduledTime = sdf.parse(fixedStartTime);
            long executeTimestamp = scheduledTime.getTime();

            // 1. 将任务加入有序集合，score为执行时间戳
            redisUtils.sSet(TASK_SCHEDULE_ZSET, taskCode + ":" + executeTimestamp);

            // 2. 存储任务详情到Hash
            Map<String, String> taskDetail = new HashMap<>();
            taskDetail.put("deviceId", deviceId);
            taskDetail.put("planType", String.valueOf(planType));
            taskDetail.put("taskCode", taskCode);
            taskDetail.put("taskName", taskName);
            taskDetail.put("fixedStartTime", fixedStartTime);
            taskDetail.put("executeTimestamp", String.valueOf(executeTimestamp));
            taskDetail.put("status", "waiting");

            redisUtils.add(TASK_DETAIL_HASH + ":" + taskCode, taskDetail);

            // 设置过期时间（执行时间+1天）
            long expireSeconds = (executeTimestamp - System.currentTimeMillis()) / 1000 + 24 * 60 * 60;
            if (expireSeconds > 0) {
                redisUtils.expire(TASK_DETAIL_HASH + ":" + taskCode, expireSeconds);
            }

            log.info("添加定时任务: {}, 任务类型: {}, 执行时间: {}", taskCode, planType, fixedStartTime);

        } catch (Exception e) {
            log.error("添加定时任务失败", e);
        }
    }

    /**
     * 定时扫描并执行任务（每分钟执行一次）
     */
    @Scheduled(fixedRate = 60000)
    public void scanAndExecuteTasks() {
        try {
            // 获取当前时间
            long currentTime = System.currentTimeMillis();

            // 3. 获取所有任务（这里只能获取全部，因为没有keys方法）
            // 注意：这可能效率不高，如果任务多的话
            Set<Object> allTasks = redisUtils.members(TASK_SCHEDULE_ZSET);
            if (allTasks == null || allTasks.isEmpty()) {
                return;
            }

            // 遍历所有任务
            for (Object taskObj : allTasks) {
                try {
                    processScheduledTask(taskObj, currentTime);
                } catch (Exception e) {
                    log.error("处理定时任务失败: {}", taskObj, e);
                }
            }

        } catch (Exception e) {
            log.error("定时扫描任务失败", e);
        }
    }

    /**
     * 处理单个定时任务：判断是否到点、加载详情并触发执行
     */
    private void processScheduledTask(Object taskObj, long currentTime) {
        if (taskObj == null) return;

        String taskInfo = taskObj.toString();
        String[] parts = taskInfo.split(":");
        if (parts.length < 2) return;

        String taskCode = parts[0];
        long executeTimestamp = Long.parseLong(parts[1]);

        // 检查是否到执行时间
        if (currentTime < executeTimestamp) {
            return;
        }

        // 获取任务详情
        Map<Object, Object> detailMap = redisUtils.getHashEntries(TASK_DETAIL_HASH + ":" + taskCode);
        if (detailMap == null || detailMap.isEmpty()) {
            // 删除无效任务
            redisUtils.remove(TASK_SCHEDULE_ZSET, taskInfo);
            return;
        }

        // 转换为String Map
        Map<String, String> taskDetail = convertToStringMap(detailMap);

        String status = taskDetail.get("status");
        if (!"waiting".equals(status)) {
            return;
        }

        log.info("执行定时任务: {}", taskCode);

        // 更新状态为执行中
        taskDetail.put("status", "executing");
        taskDetail.put("actualStartTime", String.valueOf(currentTime));
        redisUtils.add(TASK_DETAIL_HASH + ":" + taskCode, taskDetail);

        // 这里需要调用你的执行逻辑,需要区分风机任务和普通任务
        // executeTaskLogic(taskCode, taskDetail);
        String singleDeviceId = taskDetail.get("deviceId");
        String taskName = taskDetail.get("taskName");
        String planType = taskDetail.get("planType");
        String fixedStartTime = taskDetail.get("fixedStartTime");
        int result = executeTask(planType, singleDeviceId, taskCode, taskName, fixedStartTime);
//      执行成功了才加入监控；EUA任务由下级平台执行，不复用本地航线任务监控。
        if (result == 0) {
            if ("5".equals(planType)) {
                log.info("EUA定时任务已完成下发，无需启动本地任务监控: {}", taskCode);
            } else {
                redisUtils.set("isCenterTask","1");

                WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, redisUtils.get("jobId").toString())
                );
                // 1. 启动状态监控（反而要加监控覆盖掉默认的状态监控）
                JobControlHandler.startMonitoringTask(taskCode, taskName);
            }
        }

        // 从有序集合中移除已执行任务
        redisUtils.remove(TASK_SCHEDULE_ZSET, taskInfo);
    }

    /**
     * 将Redis Hash中的Object键值对转换为String Map
     */
    private Map<String, String> convertToStringMap(Map<Object, Object> detailMap) {
        Map<String, String> taskDetail = new HashMap<>();
        for (Map.Entry<Object, Object> entry : detailMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                taskDetail.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return taskDetail;
    }

    /**
     * 执行任务
     */
    private int executeTask(String planType,String singleDeviceId,String taskCode,String taskName, String fixedStartTime) {
        try {
//          分风机任务和普通任务，0普通1风机，0传间隔id 1传设备id
//          间隔航线多对一可以，一对多不可以
            if("0".equals(planType)){
                return executeNormalTask(singleDeviceId, taskName);
            }else if ("1".equals(planType)){
                return executeFanTask(singleDeviceId, taskCode, taskName);
            }else if ("5".equals(planType)){
                return executeCqDockTask(singleDeviceId, taskCode, taskName, fixedStartTime);
            }
            return -1;
        } catch (Exception e) {
            log.error("任务执行异常", e);
            return -1;
        }
    }

    /**
     * 执行普通任务（planType=0，传入间隔id）
     */
    private int executeNormalTask(String singleDeviceId, String taskName) throws SQLException {
        UniPoint uniPoint = uniPointMapper2.selectOne(
                new QueryWrapper<UniPoint>()
                        .eq("bay_id", singleDeviceId)
                        .orderByDesc("id")
                        .last("LIMIT 1")
        );
        String waylineId = uniPoint.getWaylineId();
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanType, 0)
                .eq(PubWaylineJobPlanDfEntity::getFileId, waylineId)
                .eq(PubWaylineJobPlanDfEntity::getTaskType,0)
                .orderByDesc(PubWaylineJobPlanDfEntity::getCreateTime)
                .last("LIMIT 1"));
        pubWaylineJobPlanDfEntity.setName(taskName);
        return dispatchExpressPlan(pubWaylineJobPlanDfEntity);
    }

    /**
     * 执行风机任务（planType=1，传入设备id）
     */
    private int executeFanTask(String singleDeviceId, String taskCode, String taskName) throws SQLException {
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanType, 1)
                .eq(PubWaylineJobPlanDfEntity::getFanId, singleDeviceId)
                .eq(PubWaylineJobPlanDfEntity::getTaskType,0)
                .orderByDesc(PubWaylineJobPlanDfEntity::getCreateTime)
                .last("LIMIT 1"));
        String fanName = pubWaylineJobPlanDfEntity.getFanName();
        fanName = fanName.replace("#", "");
        pubWaylineJobPlanDfEntity.setName(fanName+"-"+taskName);
        redisUtils.set("taskCode",taskCode);
        return dispatchExpressPlan(pubWaylineJobPlanDfEntity);
    }

    /**
     * 执行重庆EUA任务（planType=5，传入间隔id），由定时扫描到点后调用下级下发接口。
     */
    private int executeCqDockTask(String bayId, String taskCode, String taskName, String fixedStartTime) {
        String routeId = resolveCqDockRouteId(bayId);
        if (!StringUtils.hasText(routeId)) {
            log.error("EUA定时任务执行失败，未在df_uni_point中匹配到航线: taskCode={}, bayId={}", taskCode, bayId);
            saveCqDockTaskRecord(taskCode, taskName, bayId, null, null, null, "未在df_uni_point中匹配到航线", null, false);
            return -1;
        }

        CqAssignTaskRequest request = new CqAssignTaskRequest();
        request.setTaskName(taskName);
        request.setBusinessId(taskCode);
        request.setRouteId(routeId);
        // todo EUA接口当前只需要任务名称、业务ID和航线ID，deviceIdList根据实际情况补充，到定时时间后再真正调用下级下发
        CqApiResponse response = cqDockApiService.assignTask(request);
        boolean success = response != null && (Boolean.TRUE.equals(response.getSuccess()) || Objects.equals(response.getCode(), 200));
        String euaTaskId = extractEuaTaskId(response);
        saveCqDockTaskRecord(taskCode, taskName, bayId, routeId, euaTaskId,
                response == null ? null : response.getCode(),
                response == null ? "EUA接口无响应" : response.getMsg(),
                response == null ? null : response.getRawBody(), success);
        if (success) {
            log.info("EUA定时任务下发成功: taskCode={}, bayId={}, routeId={}, taskId={}", taskCode, bayId, routeId, euaTaskId);
            cqDockTaskStatusHandler.startMonitoring(taskCode, taskName, euaTaskId, fixedStartTime);
            return 0;
        }
        log.error("EUA定时任务下发失败: taskCode={}, bayId={}, routeId={}, code={}, msg={}",
                taskCode, bayId, routeId, response == null ? null : response.getCode(),
                response == null ? "EUA接口无响应" : response.getMsg());
        return -1;
    }

    /**
     * 通过上级间隔ID查询df_uni_point，按一个间隔对应一个航线的规则获取EUA routeId。
     */
    private String resolveCqDockRouteId(String bayId) {
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
        JSONObject data = response.getData();
        return data.getString("taskId");
    }

    /**
     * 保存上级业务ID与EUA任务ID映射，后续任务状态、结果查询通过该记录关联。
     */
    private void saveCqDockTaskRecord(String taskCode, String taskName, String bayId, String routeId,
                                      String euaTaskId, Integer responseCode, String responseMsg,
                                      String rawResponse, boolean success) {
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
        Date now = new Date();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        cqDockTaskRecordMapper.insert(record);
    }

    /**
     * 构造下发凭证并调用上级任务下发接口
     */
    private int dispatchExpressPlan(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) throws SQLException {
        CustomClaim customClaim = new CustomClaim();
        customClaim.setWorkspaceId("e3dea0f5-37f2-4d79-ae58-490af3228069");
        customClaim.setUsername("adminPC");
        HttpResultResponse httpResultResponse = pubWaylineJobPlanDfService.expressPlan(customClaim, pubWaylineJobPlanDfEntity);
        if (httpResultResponse.getCode() == 0) {
            log.info("成功执行上级任务------");
        }else {
            log.info("执行上级任务失败------");
        }
        return httpResultResponse.getCode();
    }

    /**
     * 清理过期任务（可定期调用）
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupExpiredTasks() {
        try {
            long oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;

            Set<Object> allTasks = redisUtils.members(TASK_SCHEDULE_ZSET);
            if (allTasks == null) return;

            int cleaned = 0;
            for (Object taskObj : allTasks) {
                try {
                    if (taskObj == null) continue;

                    String taskInfo = taskObj.toString();
                    String[] parts = taskInfo.split(":");
                    if (parts.length < 2) continue;

                    String taskCode = parts[0];
                    long executeTimestamp = Long.parseLong(parts[1]);

                    // 删除一周前的任务
                    if (executeTimestamp < oneWeekAgo) {
                        redisUtils.remove(TASK_SCHEDULE_ZSET, taskInfo);
                        redisUtils.delete(TASK_DETAIL_HASH + ":" + taskCode);
                        cleaned++;
                    }
                } catch (Exception e) {
                    // 忽略单个任务清理失败
                }
            }

            if (cleaned > 0) {
                log.info("清理了 {} 个过期定时任务", cleaned);
            }

        } catch (Exception e) {
            log.error("清理过期任务失败", e);
        }
    }
}
