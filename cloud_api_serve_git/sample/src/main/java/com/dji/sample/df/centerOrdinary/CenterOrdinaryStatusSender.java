package com.dji.sample.df.centerOrdinary;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dji.sample.center.config.SwitchConfig;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.PatrolStatusItem;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.df.centerOrdinary.dao.CenterOrdinaryTaskMapper;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

/** Type41 status only; never republishes a flight or resends media. */
@Slf4j
@Component
public class CenterOrdinaryStatusSender {
    private final CenterOrdinaryTaskMapper tasks;
    private final PatrolHostSocketClient socket;
    private final SwitchConfig config;

    public CenterOrdinaryStatusSender(CenterOrdinaryTaskMapper tasks, PatrolHostSocketClient socket, SwitchConfig config) {
        this.tasks = tasks; this.socket = socket; this.config = config;
    }

    public void observed(CenterOrdinaryTaskEntity task, WaylineJobEntity job, WaylineJobDTO dto) {
        if (!"DISPATCHED".equals(task.getStatus())) return;
        String state;
        String description;
        Integer status = dto.getStatus();
        if (Integer.valueOf(1).equals(status)) { state = "5"; description = "已发布，待执行"; }
        else if (Integer.valueOf(2).equals(status)) { state = "2"; description = "正在执行普通采集"; }
        else if (Integer.valueOf(6).equals(status)) { state = "3"; description = "飞行已暂停"; }
        else if (Integer.valueOf(3).equals(status)) { state = "2"; description = "飞行结束，等待媒体稳定及选中点位结果上送"; }
        else return;
        int progress = dto.getProgress() == null ? 0 : Math.max(0, Math.min(99, dto.getProgress()));
        send(task, "flight:" + status, state, progress, description, job.getExecuteTime());
    }

    public void terminal(CenterOrdinaryTaskEntity task) {
        String status = task.getStatus();
        if (!Arrays.asList("COMPLETED", "FAILED", "CANCELLED", "EXPIRED", "UNKNOWN", "INCOMPLETE", "REPORT_UNKNOWN").contains(status)) return;
        String state = "COMPLETED".equals(status) ? "1" : "CANCELLED".equals(status) ? "4" : "6";
        send(task, status, state, "COMPLETED".equals(status) ? 100 : 0,
                status + ": " + Objects.toString(task.getMessage(), ""), null);
    }

    private void send(CenterOrdinaryTaskEntity task, String key, String state, int progress, String description, Long executeTime) {
        if (!("true".equalsIgnoreCase(config.getCenterOrdinaryTaskEnable()) || "1".equals(config.getCenterOrdinaryTaskEnable()))) return;
        long now = System.currentTimeMillis();
        try {
            if (tasks.reserveStatusReport(task.getId(), key, now, now - 30_000L) != 1) return;
            Set<String> requested = new HashSet<>(JSON.parseArray(task.getPointCodes(), String.class));
            Set<String> reported = new HashSet<>();
            if (task.getResultSnapshot() != null) {
                for (JSONObject result : JSON.parseArray(task.getResultSnapshot(), JSONObject.class)) {
                    if (Boolean.TRUE.equals(result.getBoolean("reported")) && result.getJSONObject("point") != null) {
                        String code = result.getJSONObject("point").getString("pointCode");
                        if (requested.contains(code)) reported.add(code);
                    }
                }
            }
            PatrolStatusItem item = new PatrolStatusItem();
            item.setTask_code(task.getTaskCode()); item.setTask_name(task.getTaskName());
            item.setTask_patrolled_id(Objects.toString(task.getJobId(), ""));
            item.setTask_state(state); item.setPlan_start_time(task.getFixedStartTime());
            item.setStart_time(executeTime == null || executeTime <= 0 ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(executeTime)));
            item.setTask_progress(progress + "%"); item.setTask_estimated_time("1".equals(state) ? "00:00:00" : "");
            item.setDescription(description + "；选中点位已上送 " + reported.size() + "/" + requested.size());
            PatrolHostCommand command = socket.getBaseCommand("41", "", task.getSubCode());
            command.setCode(task.getSubCode());
            command.addItem(item);
            if (socket.sendCommand(command, PatrolStatusItem.class)) tasks.statusReported(task.getId(), key, now);
        } catch (Exception e) {
            log.warn("上级普通任务状态发送未确认，稍后仅重试状态: recordId={}", task.getId(), e);
        }
    }
}
