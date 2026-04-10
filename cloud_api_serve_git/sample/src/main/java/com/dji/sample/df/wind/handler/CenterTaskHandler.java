package com.dji.sample.df.wind.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.center.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.common.model.CustomClaim;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

            log.info("添加定时任务: {}, 执行时间: {}", taskCode, fixedStartTime);

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
                    if (taskObj == null) continue;

                    String taskInfo = taskObj.toString();
                    String[] parts = taskInfo.split(":");
                    if (parts.length < 2) continue;

                    String taskCode = parts[0];
                    long executeTimestamp = Long.parseLong(parts[1]);

                    // 检查是否到执行时间
                    if (currentTime >= executeTimestamp) {
                        // 获取任务详情
                        Map<Object, Object> detailMap = redisUtils.getHashEntries(TASK_DETAIL_HASH + ":" + taskCode);
                        if (detailMap == null || detailMap.isEmpty()) {
                            // 删除无效任务
                            redisUtils.remove(TASK_SCHEDULE_ZSET, taskInfo);
                            continue;
                        }

                        // 转换为String Map
                        Map<String, String> taskDetail = new HashMap<>();
                        for (Map.Entry<Object, Object> entry : detailMap.entrySet()) {
                            if (entry.getKey() != null && entry.getValue() != null) {
                                taskDetail.put(entry.getKey().toString(), entry.getValue().toString());
                            }
                        }

                        String status = taskDetail.get("status");
                        if ("waiting".equals(status)) {
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
                            int result = executeTask(planType, singleDeviceId, taskCode, taskName);
//                          执行成功了才加入监控
                            if (result == 0) {
                                redisUtils.set("isCenterTask","1");

                                WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                                        .eq(WaylineJobEntity::getJobId, redisUtils.get("jobId").toString())
                                );
                                // 1. 启动状态监控（反而要加监控覆盖掉默认的状态监控）
                                JobControlHandler.startMonitoringTask(taskCode, taskName);
                            }

                            // 从有序集合中移除已执行任务
                            redisUtils.remove(TASK_SCHEDULE_ZSET, taskInfo);
                        }
                    }

                } catch (Exception e) {
                    log.error("处理定时任务失败: {}", taskObj, e);
                }
            }

        } catch (Exception e) {
            log.error("定时扫描任务失败", e);
        }
    }

    /**
     * 执行任务
     */
    private int executeTask(String planType,String singleDeviceId,String taskCode,String taskName) {
        try {
//          分风机任务和普通任务，0普通1风机，0传间隔id 1传设备id
//          间隔航线多对一可以，一对多不可以
            if("0".equals(planType)){
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
                CustomClaim customClaim = new CustomClaim();
                customClaim.setWorkspaceId("e3dea0f5-37f2-4d79-ae58-490af3228069");
                customClaim.setUsername("adminPC");
                pubWaylineJobPlanDfEntity.setName(taskName);
                HttpResultResponse httpResultResponse = pubWaylineJobPlanDfService.expressPlan(customClaim, pubWaylineJobPlanDfEntity);
                if (httpResultResponse.getCode() == 0) {
                    log.info("成功执行上级任务------");
                }else {
                    log.info("执行上级任务失败------");
                }
                return httpResultResponse.getCode();
            }else if ("1".equals(planType)){
                PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                        .eq(PubWaylineJobPlanDfEntity::getPlanType, 1)
                        .eq(PubWaylineJobPlanDfEntity::getFanId, singleDeviceId)
                        .eq(PubWaylineJobPlanDfEntity::getTaskType,0)
                        .orderByDesc(PubWaylineJobPlanDfEntity::getCreateTime)
                        .last("LIMIT 1"));
                CustomClaim customClaim = new CustomClaim();
                customClaim.setWorkspaceId("e3dea0f5-37f2-4d79-ae58-490af3228069");
                customClaim.setUsername("adminPC");
                String fanName = pubWaylineJobPlanDfEntity.getFanName();
                fanName = fanName.replace("#", "");
                pubWaylineJobPlanDfEntity.setName(fanName+"-"+taskName);
                redisUtils.set("taskCode",taskCode);
                HttpResultResponse httpResultResponse = pubWaylineJobPlanDfService.expressPlan(customClaim, pubWaylineJobPlanDfEntity);
                if (httpResultResponse.getCode() == 0) {
                    log.info("成功执行上级任务------");
                }else {
                    log.info("执行上级任务失败------");
                }
                return httpResultResponse.getCode();
            }
            return -1;
        } catch (Exception e) {
            log.error("任务执行异常", e);
            return -1;
        }
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
