package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSONObject;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.center.config.CenterNormalConfig;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.PatrolStatusItem;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.df.cqDockDf.model.dto.CqApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
public class CqDockTaskStatusHandler {

    private static final String MONITOR_SET = "eua_task_status_monitor:set";
    private static final String MONITOR_HASH = "eua_task_status_monitor:hash";

    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private CqDockApiService cqDockApiService;
    @Autowired
    private CqDockPictureReportService cqDockPictureReportService;
    @Autowired
    private PatrolHostSocketClient patrolHostSocketClient;
    @Autowired
    private CenterNormalConfig centerConfig;

    public void startMonitoring(String taskCode, String taskName, String euaTaskId, String fixedStartTime) {
        if (!StringUtils.hasText(euaTaskId)) {
            log.warn("EUA任务下发成功但未返回taskId，无法启动状态查询: taskCode={}", taskCode);
            return;
        }
        Map<String, String> detail = new HashMap<>();
        detail.put("taskName", taskName);
        detail.put("euaTaskId", euaTaskId);
        detail.put("fixedStartTime", fixedStartTime);
        detail.put("lastState", "");
        detail.put("lastProgress", "");
        redisUtils.sSet(MONITOR_SET, taskCode);
        redisUtils.add(MONITOR_HASH + ":" + taskCode, detail);
        redisUtils.expire(MONITOR_HASH + ":" + taskCode, 2 * 60 * 60L);
        log.info("开始监控EUA任务状态: taskCode={}, euaTaskId={}", taskCode, euaTaskId);
    }

    @Scheduled(fixedDelay = 5000)
    public void scanTaskStatus() {
        try {
            Set<Object> taskCodes = redisUtils.members(MONITOR_SET);
            if (taskCodes == null || taskCodes.isEmpty()) {
                return;
            }
            for (Object taskCodeObj : taskCodes) {
                if (taskCodeObj != null) {
                    processTaskStatus(taskCodeObj.toString());
                }
            }
        } catch (Exception e) {
            log.error("扫描EUA任务状态失败", e);
        }
    }

    private void processTaskStatus(String taskCode) {
        Map<Object, Object> raw = redisUtils.getHashEntries(MONITOR_HASH + ":" + taskCode);
        if (raw == null || raw.isEmpty()) {
            redisUtils.remove(MONITOR_SET, taskCode);
            return;
        }

        Map<String, String> detail = toStringMap(raw);
        String euaTaskId = detail.get("euaTaskId");
        try {
            CqApiResponse response = cqDockApiService.taskStatus(euaTaskId);
            if (response == null || !(Boolean.TRUE.equals(response.getSuccess()) || Objects.equals(response.getCode(), 200))) {
                log.warn("EUA任务状态查询失败: taskCode={}, euaTaskId={}, msg={}", taskCode, euaTaskId, response == null ? null : response.getMsg());
                return;
            }

            JSONObject data = response.getData();
            String euaStatus = extractStatus(data);
            String mappedState = mapEuaState(euaStatus);
            int progress = extractProgress(data, mappedState);
            sendTaskStatus(taskCode, detail.get("taskName"), euaTaskId, mappedState, progress,
                    detail.get("fixedStartTime"), euaStatus);
            detail.put("lastState", mappedState);
            detail.put("lastProgress", String.valueOf(progress));
            redisUtils.add(MONITOR_HASH + ":" + taskCode, detail);

            if (isTerminalEuaStatus(euaStatus)) {
                if ("7".equals(euaStatus)) {
                    // EUA约定：7表示任务完成且图片上传完成/总数已齐，此时再执行HTTP查图、落库、本地保存、FTP上传和上级图片上报。
//                  todo 后续需让EUA平台规定7为图片上传结束标志
                    cqDockPictureReportService.fetchSaveAndReport(taskCode, detail.get("taskName"), euaTaskId);
                }
                redisUtils.remove(MONITOR_SET, taskCode);
                redisUtils.delete(MONITOR_HASH + ":" + taskCode);
                log.info("EUA任务已结束，停止状态监控: taskCode={}, euaTaskId={}, euaStatus={}, state={}",
                        taskCode, euaTaskId, euaStatus, mappedState);
            }
        } catch (Exception e) {
            log.error("处理EUA任务状态失败: taskCode={}, euaTaskId={}", taskCode, euaTaskId, e);
        }
    }

    private Map<String, String> toStringMap(Map<Object, Object> raw) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : raw.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return result;
    }

    private String extractStatus(JSONObject data) {
        if (data == null) {
            return null;
        }
        String status = data.getString("status");
        return status;
    }

    private int extractProgress(JSONObject data, String mappedState) {
        if ("1".equals(mappedState)) {
            return 100;
        }
        if ("2".equals(mappedState)) {
            return 10;
        }
        if ("3".equals(mappedState)|| "4".equals(mappedState)|| "5".equals(mappedState)|| "6".equals(mappedState)) {
            return 0;
        }
        return 0;
    }

    private String mapEuaState(String status) {
        if (!StringUtils.hasText(status)) {
            return "2";
        }
        String value = status.trim();
        if ("0".equals(value)) {
            return "5";
        }
        if ("7".equals(value)) {
            return "1";
        }
        if ("1".equals(value)) {
            return "2";
        }
        if ("2".equals(value)) {
            return "2";
        }
        if ("3".equals(value)) {
            return "6";
        }
        if ("4".equals(value)) {
            return "5";
        }
        if ("5".equals(value)) {
            return "3";
        }
        if ("6".equals(value)) {
            return "4";
        }
        return "2";
    }

    private boolean isTerminalEuaStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String value = status.trim();
        return "7".equals(value) || "3".equals(value) || "6".equals(value);
    }

    private void sendTaskStatus(String taskCode, String taskName, String euaTaskId, String mappedState,
                                int progress, String fixedStartTime, String euaStatus) {
        PatrolStatusItem item = new PatrolStatusItem();
        item.setTask_patrolled_id(euaTaskId);
        item.setTask_name(taskName);
        item.setTask_code(taskCode);
        item.setTask_state(mappedState);
        item.setStart_time(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        item.setPlan_start_time(fixedStartTime);
        item.setTask_progress(progress+"%");
        item.setTask_estimated_time(calculateEstimatedTime(progress));
        item.setDescription(buildDescription(mappedState, progress, euaStatus));

        List<PatrolStatusItem> items = new ArrayList<>();
        items.add(item);
        PatrolHostCommand commandData = new PatrolHostCommand();
        commandData.addItems(items);
        commandData.setSendCode(centerConfig.getStationCode());
        commandData.setReceiveCode(centerConfig.getServerCode());
        commandData.setType("41");
        boolean sendSuccess = patrolHostSocketClient.sendCommand(commandData, PatrolStatusItem.class);
        if (sendSuccess) {
            log.info("上报EUA任务状态成功: taskCode={}, euaTaskId={}, euaStatus={}, state={}, progress={}%",
                    taskCode, euaTaskId, euaStatus, mappedState, progress);
        } else {
            log.warn("上报EUA任务状态失败，TCP可能未连接: taskCode={}, euaTaskId={}, euaStatus={}, state={}, progress={}%",
                    taskCode, euaTaskId, euaStatus, mappedState, progress);
        }
    }

    private String buildDescription(String mappedState, int progress, String euaStatus) {
        if ("1".equals(mappedState)) {
            return "完成任务巡检";
        }
        if ("3".equals(mappedState)) {
            return "巡检任务已暂停";
        }
        if ("4".equals(mappedState)) {
            return "巡检任务已终止";
        }
        if ("5".equals(mappedState)) {
            if ("4".equals(euaStatus)) {
                return "巡检任务已下发到机场，待执行";
            }
            return "巡检任务待执行";
        }
        if ("6".equals(mappedState)) {
            return "巡检任务失败";
        }
        return "正在进行巡视，已完成" + progress + "%的巡视任务";
    }

    private String calculateEstimatedTime(int progress) {
        if (progress >= 100) {
            return "00:00:00";
        }
        int remainingSeconds = (int) ((100 - progress) / 100.0 * 25 * 60);
        return String.format("%02d:%02d:%02d", remainingSeconds / 3600,
                (remainingSeconds % 3600) / 60, remainingSeconds % 60);
    }
}
