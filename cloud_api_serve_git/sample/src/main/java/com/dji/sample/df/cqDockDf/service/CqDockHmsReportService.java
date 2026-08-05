package com.dji.sample.df.cqDockDf.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.config.SwitchConfig;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.v2022.command.upload.UavDeviceAlarmItem;
import com.dji.sample.center.v2022.handler.CenterMsgPushHandler;
import com.dji.sample.df.cqDockDf.dao.CqDockHmsMapper;
import com.dji.sample.df.cqDockDf.model.entity.CqDockHmsEntity;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class CqDockHmsReportService {

    @Autowired
    private CqDockHmsMapper cqDockHmsMapper;
    @Autowired
    private CenterMsgPushHandler centerMsgPushHandler;
    @Autowired
    private SwitchConfig switchConfig;
    @Autowired
    private IDeviceMapper deviceMapper;

    public void reportIfTcpEnabled(Long hmsId) {
        if (!isTcpEnabled()) {
            log.info("[cq-dock][push/hms] 上级TCP开关未开启，告警仅入库: hmsId={}", hmsId);
            return;
        }
        CqDockHmsEntity entity = cqDockHmsMapper.selectById(hmsId);
        if (entity == null) {
            return;
        }
        report(entity);
    }

    @Scheduled(fixedDelay = 30000)
    public void retryFailedReports() {
        if (!isTcpEnabled()) {
            return;
        }
        List<CqDockHmsEntity> list = cqDockHmsMapper.selectList(new LambdaQueryWrapper<CqDockHmsEntity>()
                .and(w -> w.isNull(CqDockHmsEntity::getReportStatus).or().ne(CqDockHmsEntity::getReportStatus, 1))
                .and(w -> w.isNull(CqDockHmsEntity::getRetryCount).or().lt(CqDockHmsEntity::getRetryCount, 10))
                .orderByAsc(CqDockHmsEntity::getCreateTime)
                .last("limit 20"));
        for (CqDockHmsEntity entity : list) {
            report(entity);
        }
    }

    private void report(CqDockHmsEntity entity) {
        Date now = new Date();
        try {
            UavDeviceAlarmItem item = buildAlarmItem(entity);
            boolean success = centerMsgPushHandler.pushDeviceAlarm(Collections.singletonList(item));
            entity.setReportStatus(success ? 1 : 2);
            entity.setReportMsg(success ? "success" : "send failed or center not connected");
            entity.setReportTime(now);
            entity.setRetryCount(entity.getRetryCount() == null ? 1 : entity.getRetryCount() + 1);
            entity.setUpdateTime(now);
            cqDockHmsMapper.updateById(entity);
            log.info("[cq-dock][push/hms] 上级告警上送{}: hmsId={}, messageId={}, retryCount={}",
                    success ? "成功" : "失败", entity.getId(), entity.getMessageId(), entity.getRetryCount());
        } catch (Exception e) {
            entity.setReportStatus(2);
            entity.setReportMsg(limit(e.getMessage(), 500));
            entity.setReportTime(now);
            entity.setRetryCount(entity.getRetryCount() == null ? 1 : entity.getRetryCount() + 1);
            entity.setUpdateTime(now);
            cqDockHmsMapper.updateById(entity);
            log.error("[cq-dock][push/hms] 上级告警上送异常: hmsId={}, err={}", entity.getId(), e.getMessage(), e);
        }
    }

    private UavDeviceAlarmItem buildAlarmItem(CqDockHmsEntity entity) {
        String deviceSn = firstText(entity.getUavSn(), entity.getDockSn(), entity.getTopicDockSn());
        DeviceEntity device = findDevice(deviceSn);
        UavDeviceAlarmItem item = new UavDeviceAlarmItem();
        item.setPatroldevice_name(device == null || !StringUtils.hasText(device.getDeviceName()) ? deviceSn : device.getDeviceName());
        item.setPatroldevice_code(deviceSn);
        item.setTime(DateUtils.parseDateToStr(entity.getCreateTime() == null ? new Date() : entity.getCreateTime()));
        item.setContent(buildContent(entity));
        return item;
    }

    private DeviceEntity findDevice(String deviceSn) {
        if (!StringUtils.hasText(deviceSn)) {
            return null;
        }
        return deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>()
                .eq(DeviceEntity::getDeviceSn, deviceSn)
                .last("limit 1"));
    }

    private String buildContent(CqDockHmsEntity entity) {
        String message = StringUtils.hasText(entity.getMessage()) ? entity.getMessage() : "健康告警";
        String alarmCode = StringUtils.hasText(entity.getAlarmCode()) ? entity.getAlarmCode() : "";
        String level = entity.getLevel() == null ? "" : String.valueOf(entity.getLevel());
        if (!StringUtils.hasText(alarmCode) && !StringUtils.hasText(level)) {
            return message;
        }
        return String.format("%s，告警码:%s，等级:%s", message, alarmCode, level);
    }

    private boolean isTcpEnabled() {
        return "true".equalsIgnoreCase(switchConfig.getCenterNormalTcpEnable());
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String limit(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}
