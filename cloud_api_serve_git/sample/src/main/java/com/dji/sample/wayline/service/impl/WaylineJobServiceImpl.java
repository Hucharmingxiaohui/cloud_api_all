package com.dji.sample.wayline.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.component.mqtt.model.EventsReceiver;
import com.dji.sample.component.redis.RedisConst;
import com.dji.sample.component.redis.RedisOpsUtils;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.windDf.dao.FanWaylinePointsMapper;
import com.dji.sample.df.windDf.model.entity.FanWaylinePoints;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.manage.service.IDeviceService;
import com.dji.sample.media.model.MediaFileCountDTO;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.model.enums.WaylineJobStatusEnum;
import com.dji.sample.wayline.model.param.CreateJobParam;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sample.wayline.service.IWaylineJobService;
import com.dji.sample.wayline.service.IWaylineRedisService;
import com.dji.sdk.cloudapi.device.DockModeCodeEnum;
import com.dji.sdk.cloudapi.device.DroneModeCodeEnum;
import com.dji.sdk.cloudapi.device.OsdDock;
import com.dji.sdk.cloudapi.device.OsdDockDrone;
import com.dji.sdk.cloudapi.wayline.*;
import com.dji.sdk.common.Pagination;
import com.dji.sdk.common.PaginationData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sean
 * @version 1.1
 * @date 2022/6/1
 */
@Service
@Transactional
@Slf4j
public class WaylineJobServiceImpl implements IWaylineJobService {

    private static final String WAYLINE_DOCK_FALLBACK_PREFIX = "wayline:dock_fallback:";

    private static final int DOCK_FALLBACK_PROGRESS_THRESHOLD = 90;

    private static final long DOCK_FALLBACK_CONFIRM_MILLIS = 30_000L;

    @Autowired
    private IWaylineJobMapper mapper;

    @Autowired
    private IWaylineFileService waylineFileService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IFileService fileService;

    @Autowired
    private IDeviceRedisService deviceRedisService;

    @Autowired
    private IWaylineRedisService waylineRedisService;

    @Autowired
    PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;

    @Resource
    RedisUtils redisUtils;

    @Autowired
    FanWaylinePointsMapper fanWaylinePointsMapper;

    private Optional<WaylineJobDTO> insertWaylineJob(WaylineJobEntity jobEntity) {
        int id = mapper.insert(jobEntity);
        if (id <= 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.entity2Dto(jobEntity));
    }

    @Override
    public Optional<WaylineJobDTO> createWaylineJob(CreateJobParam param, String workspaceId, String username, Long beginTime, Long endTime) {
        if (Objects.isNull(param)) {
            return Optional.empty();
        }
//      获取计划的总点位数
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanId, param.getPlanId()));
//        String[] split = pubWaylineJobPlanDfEntity.getWaylinePointPos().split(",");
        // Immediate tasks, allocating time on the backend.
        WaylineJobEntity jobEntity = WaylineJobEntity.builder()
                .name(param.getName())
                .dockSn(param.getDockSn())
                .fileId(param.getFileId())
                .username(username)
                .workspaceId(workspaceId)
                .jobId(UUID.randomUUID().toString())
                .beginTime(beginTime)
                .endTime(endTime)
                .status(WaylineJobStatusEnum.PENDING.getVal())
                .taskType(param.getTaskType().getType())
                .waylineType(param.getWaylineType().getValue())
                .outOfControlAction(param.getOutOfControlAction().getAction())
                .rthAltitude(param.getRthAltitude())
                .mediaCount(0)
                .planId(param.getPlanId())
                .fanName(param.getFanName())
//                .allPointCnt(split.length)
                .build();
//        redisUtils.set("jobId",jobEntity.getJobId());
        return insertWaylineJob(jobEntity);
    }

    @Override
    public Optional<WaylineJobDTO> createWaylineJob2(CreateJobParam param, String workspaceId, String username, Long beginTime, Long endTime) {
        if (Objects.isNull(param)) {
            return Optional.empty();
        }
//      获取计划的总点位数
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanId, param.getPlanId()));
        // Immediate tasks, allocating time on the backend.

        WaylineJobEntity jobEntity = WaylineJobEntity.builder()
                .name(param.getName())
                .dockSn(param.getDockSn())
                .fileId(param.getFileId())
                .username(username)
                .workspaceId(workspaceId)
                .jobId(param.getPlanId())
                .beginTime(beginTime)
                .endTime(endTime)
                .status(WaylineJobStatusEnum.PENDING.getVal())
                .taskType(param.getTaskType().getType())
                .waylineType(param.getWaylineType().getValue())
                .outOfControlAction(param.getOutOfControlAction().getAction())
                .rthAltitude(param.getRthAltitude())
                .mediaCount(0)
                .planId(param.getPlanId())
//                .allPointCnt(split.length)
                .build();

        return insertWaylineJob(jobEntity);
    }

    @Override
    public Optional<WaylineJobDTO> createWaylineJobByParent(String workspaceId, String parentId) {
        Optional<WaylineJobDTO> parentJobOpt = this.getJobByJobId(workspaceId, parentId);
        if (parentJobOpt.isEmpty()) {
            return Optional.empty();
        }
        WaylineJobEntity jobEntity = this.dto2Entity(parentJobOpt.get());
        jobEntity.setJobId(UUID.randomUUID().toString());
        jobEntity.setErrorCode(null);
        jobEntity.setCompletedTime(null);
        jobEntity.setExecuteTime(null);
        jobEntity.setStatus(WaylineJobStatusEnum.PENDING.getVal());
        jobEntity.setParentId(parentId);

        return this.insertWaylineJob(jobEntity);
    }

    public List<WaylineJobDTO> getJobsByConditions(String workspaceId, Collection<String> jobIds, WaylineJobStatusEnum status) {
        return mapper.selectList(
                new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getWorkspaceId, workspaceId)
                        .eq(Objects.nonNull(status), WaylineJobEntity::getStatus, status.getVal())
                        .and(!CollectionUtils.isEmpty(jobIds),
                                wrapper -> jobIds.forEach(id -> wrapper.eq(WaylineJobEntity::getJobId, id).or())))
                .stream()
                .map(this::entity2Dto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<WaylineJobDTO> getJobByJobId(String workspaceId, String jobId) {
        WaylineJobEntity jobEntity = mapper.selectOne(
                new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getWorkspaceId, workspaceId)
                        .eq(WaylineJobEntity::getJobId, jobId));
        return Optional.ofNullable(entity2Dto(jobEntity));
    }

    @Override
    public Boolean updateJob(WaylineJobDTO dto) {
        return mapper.update(this.dto2Entity(dto),
                new LambdaUpdateWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, dto.getJobId())) > 0;
    }

    @Override
    public PaginationData<WaylineJobDTO> getJobsByWorkspaceId(String workspaceId, long page, long pageSize, Map map) {
        // 构建查询条件
        LambdaQueryWrapper<WaylineJobEntity> queryWrapper = new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getWorkspaceId, workspaceId);
        // 动态添加 name 条件（模糊查询）
        if (map != null && map.containsKey("name")) {
            String name = map.get("name").toString();
            if (com.dji.sample.center.utils.StringUtils.isNotBlank(name)) {
                queryWrapper.like(WaylineJobEntity::getName, name);
            }
        }
        // 动态添加 taskType 条件（精确匹配）
        if (map != null && map.containsKey("taskType")) {
            String taskType = map.get("taskType").toString();
            if (com.dji.sample.center.utils.StringUtils.isNotBlank(taskType)) {
                queryWrapper.eq(WaylineJobEntity::getTaskType, taskType);
            }
        }
        // 动态添加 planType 条件：通过任务关联的计划 plan_id 过滤
        if (map != null && map.containsKey("planType")) {
            String planType = map.get("planType").toString();
            if (com.dji.sample.center.utils.StringUtils.isNotBlank(planType)) {
                List<String> planIds = pubWaylineJobPlanDfMapper.selectList(
                                new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                                        .eq(PubWaylineJobPlanDfEntity::getPlanType, Integer.valueOf(planType))
                                        .select(PubWaylineJobPlanDfEntity::getPlanId))
                        .stream()
                        .map(PubWaylineJobPlanDfEntity::getPlanId)
                        .filter(com.dji.sample.center.utils.StringUtils::isNotBlank)
                        .collect(Collectors.toList());
                if (planIds.isEmpty()) {
                    queryWrapper.eq(WaylineJobEntity::getPlanId, "__NO_MATCH_PLAN_ID__");
                } else {
                    queryWrapper.in(WaylineJobEntity::getPlanId, planIds);
                }
            }
        }
        // 排序
        queryWrapper.orderByDesc(WaylineJobEntity::getId);
        // 执行分页查询
        Page<WaylineJobEntity> pageData = mapper.selectPage(
                new Page<WaylineJobEntity>(page, pageSize),
                queryWrapper);

        List<WaylineJobDTO> records = pageData.getRecords()
                .stream()
                .map(this::entity2Dto)
                .collect(Collectors.toList());

        return new PaginationData<WaylineJobDTO>(records,
                new Pagination(pageData.getCurrent(), pageData.getSize(), pageData.getTotal()));
    }

    private WaylineJobEntity dto2Entity(WaylineJobDTO dto) {
        WaylineJobEntity.WaylineJobEntityBuilder builder = WaylineJobEntity.builder();
        if (dto == null) {
            return builder.build();
        }
        if (Objects.nonNull(dto.getBeginTime())) {
            builder.beginTime(dto.getBeginTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (Objects.nonNull(dto.getEndTime())) {
            builder.endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (Objects.nonNull(dto.getExecuteTime())) {
            builder.executeTime(dto.getExecuteTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (Objects.nonNull(dto.getCompletedTime())) {
            builder.completedTime(dto.getCompletedTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        return builder.status(dto.getStatus())
                .mediaCount(dto.getMediaCount())
                .name(dto.getJobName())
                .errorCode(dto.getCode())
                .jobId(dto.getJobId())
                .fileId(dto.getFileId())
                .dockSn(dto.getDockSn())
                .workspaceId(dto.getWorkspaceId())
                .taskType(Optional.ofNullable(dto.getTaskType()).map(TaskTypeEnum::getType).orElse(null))
                .waylineType(Optional.ofNullable(dto.getWaylineType()).map(WaylineTypeEnum::getValue).orElse(null))
                .username(dto.getUsername())
                .rthAltitude(dto.getRthAltitude())
                .outOfControlAction(Optional.ofNullable(dto.getOutOfControlAction())
                        .map(OutOfControlActionEnum::getAction).orElse(null))
                .parentId(dto.getParentId())
                .build();
    }

    public WaylineJobStatusEnum getWaylineState(String dockSn) {
        Optional<DeviceDTO> dockOpt = deviceRedisService.getDeviceOnline(dockSn);
        if (dockOpt.isEmpty() || !StringUtils.hasText(dockOpt.get().getChildDeviceSn())) {
            return WaylineJobStatusEnum.UNKNOWN;
        }
        Optional<OsdDock> dockOsdOpt = deviceRedisService.getDeviceOsd(dockSn, OsdDock.class);
        Optional<OsdDockDrone> deviceOsdOpt = deviceRedisService.getDeviceOsd(dockOpt.get().getChildDeviceSn(), OsdDockDrone.class);
        if (dockOsdOpt.isEmpty() || deviceOsdOpt.isEmpty() || DockModeCodeEnum.WORKING != dockOsdOpt.get().getModeCode()) {
            return WaylineJobStatusEnum.UNKNOWN;
        }

        OsdDockDrone osdDevice = deviceOsdOpt.get();
        if (DroneModeCodeEnum.WAYLINE == osdDevice.getModeCode()
                || DroneModeCodeEnum.MANUAL == osdDevice.getModeCode()
                || DroneModeCodeEnum.TAKEOFF_AUTO == osdDevice.getModeCode()) {
            if (StringUtils.hasText(waylineRedisService.getPausedWaylineJobId(dockSn))) {
                return WaylineJobStatusEnum.PAUSED;
            }
            if (waylineRedisService.getRunningWaylineJob(dockSn).isPresent()) {
                return WaylineJobStatusEnum.IN_PROGRESS;
            }
        }
        return WaylineJobStatusEnum.UNKNOWN;
    }

    public WaylineJobDTO entity2Dto(WaylineJobEntity entity) {
        if (entity == null) {
            return null;
        }
//      此次任务进度大于90，无人机回巢30秒以上也判断为任务结束，兜底
        applyDockedSuccessFallback(entity);

//      如果是不停机巡检任务，已上传图片要加上
        int videoPointNum=0;
        FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
                .eq(FanWaylinePoints::getJobId, entity.getJobId())
                .orderByDesc(FanWaylinePoints::getId)
                .last("LIMIT 1"));
        if(fanWaylinePoints!=null){
            Integer jobType = fanWaylinePoints.getJobType();
            if (jobType == 1 && fanWaylinePoints.getVideoFanPoints()!=null) {
//                videoPoints  可能是null
                JSONArray videoPoints = JSON.parseArray(fanWaylinePoints.getVideoFanPoints());
                videoPointNum = videoPoints.size();
            }
        }

        WaylineJobDTO.WaylineJobDTOBuilder builder = WaylineJobDTO.builder()
                .jobId(entity.getJobId())
                .jobName(entity.getName())
                .fileId(entity.getFileId())
                .fileName(waylineFileService.getWaylineByWaylineId(entity.getWorkspaceId(), entity.getFileId())
                        .orElse(new GetWaylineListResponse()).getName())
                .dockSn(entity.getDockSn())
                .dockName(deviceService.getDeviceBySn(entity.getDockSn())
                        .orElse(DeviceDTO.builder().build()).getNickname())
                .username(entity.getUsername())
                .workspaceId(entity.getWorkspaceId())
                .status(WaylineJobStatusEnum.IN_PROGRESS.getVal() == entity.getStatus() &&
                        entity.getJobId().equals(waylineRedisService.getPausedWaylineJobId(entity.getDockSn())) ?
                                WaylineJobStatusEnum.PAUSED.getVal() : entity.getStatus())
                .code(entity.getErrorCode())
                .beginTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(entity.getBeginTime()), ZoneId.systemDefault()))
                .endTime(Objects.nonNull(entity.getEndTime()) ?
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(entity.getEndTime()), ZoneId.systemDefault()) : null)
                .executeTime(Objects.nonNull(entity.getExecuteTime()) ?
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(entity.getExecuteTime()), ZoneId.systemDefault()) : null)
                .completedTime(WaylineJobStatusEnum.find(entity.getStatus()).getEnd() ?
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(entity.getUpdateTime()), ZoneId.systemDefault()) : null)
                .taskType(TaskTypeEnum.find(entity.getTaskType()))
                .waylineType(WaylineTypeEnum.find(entity.getWaylineType()))
                .rthAltitude(entity.getRthAltitude())
                .outOfControlAction(OutOfControlActionEnum.find(entity.getOutOfControlAction()))
                .mediaCount(entity.getMediaCount())
                .savedCount(videoPointNum);

        if (Objects.nonNull(entity.getEndTime())) {
            builder.endTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(entity.getEndTime()), ZoneId.systemDefault()));
        }
        if (WaylineJobStatusEnum.IN_PROGRESS.getVal() == entity.getStatus()) {
            builder.progress(waylineRedisService.getRunningWaylineJob(entity.getDockSn())
                    .map(EventsReceiver::getOutput)
                    .map(FlighttaskProgress::getProgress)
                    .map(FlighttaskProgressData::getPercent)
                    .orElse(null));
        }

        if (entity.getMediaCount() == 0) {
            return builder.build();
        }

        // sync the number of media files
        String key = RedisConst.MEDIA_HIGHEST_PRIORITY_PREFIX + entity.getDockSn();
        String countKey = RedisConst.MEDIA_FILE_PREFIX + entity.getDockSn();
        Object mediaFileCount = RedisOpsUtils.hashGet(countKey, entity.getJobId());


        if (Objects.nonNull(mediaFileCount)) {
            builder.uploadedCount(((MediaFileCountDTO) mediaFileCount).getUploadedCount()+videoPointNum)
                    .uploading(RedisOpsUtils.checkExist(key) && entity.getJobId().equals(((MediaFileCountDTO)RedisOpsUtils.get(key)).getJobId()));
            return builder.build();
        }

        int uploadedSize = fileService.getFilesByWorkspaceAndJobId(entity.getWorkspaceId(), entity.getJobId()).size();
        // All media for this job have been uploaded.
        if (uploadedSize >= entity.getMediaCount()) {
            return builder.uploadedCount(uploadedSize).build();
        }
        RedisOpsUtils.hashSet(countKey, entity.getJobId(),
                MediaFileCountDTO.builder()
                        .jobId(entity.getJobId())
                        .mediaCount(entity.getMediaCount())
                        .uploadedCount(uploadedSize).build());
        return builder.build();
    }

    private void applyDockedSuccessFallback(WaylineJobEntity entity) {
        if (WaylineJobStatusEnum.IN_PROGRESS.getVal() != entity.getStatus()
                || !StringUtils.hasText(entity.getDockSn())
                || !StringUtils.hasText(entity.getJobId())) {
            return;
        }

        String fallbackKey = WAYLINE_DOCK_FALLBACK_PREFIX + entity.getDockSn() + RedisConst.DELIMITER + entity.getJobId();
        Optional<EventsReceiver<FlighttaskProgress>> runningJobOpt = waylineRedisService.getRunningWaylineJob(entity.getDockSn());
        Integer progress = runningJobOpt
                .map(EventsReceiver::getOutput)
                .map(FlighttaskProgress::getProgress)
                .map(FlighttaskProgressData::getPercent)
                .orElse(null);
        Optional<OsdDock> dockOsdOpt = deviceRedisService.getDeviceOsd(entity.getDockSn(), OsdDock.class);

        if (Objects.isNull(progress)
                || progress < DOCK_FALLBACK_PROGRESS_THRESHOLD
                || dockOsdOpt.isEmpty()
                || !Boolean.TRUE.equals(dockOsdOpt.get().getDroneInDock())) {
            redisUtils.delete(fallbackKey);
            return;
        }

        long now = System.currentTimeMillis();
        Object firstDockedValue = redisUtils.get(fallbackKey);
        if (Objects.isNull(firstDockedValue)) {
            redisUtils.set(fallbackKey, String.valueOf(now));
            return;
        }

        long firstDockedTime;
        try {
            firstDockedTime = Long.parseLong(String.valueOf(firstDockedValue));
        } catch (NumberFormatException e) {
            redisUtils.set(fallbackKey, String.valueOf(now));
            return;
        }

        if (now - firstDockedTime < DOCK_FALLBACK_CONFIRM_MILLIS) {
            return;
        }

        boolean updated = this.updateJob(WaylineJobDTO.builder()
                .jobId(entity.getJobId())
                .status(WaylineJobStatusEnum.SUCCESS.getVal())
                .completedTime(LocalDateTime.now())
                .build());
        if (updated) {
            entity.setStatus(WaylineJobStatusEnum.SUCCESS.getVal());
            entity.setCompletedTime(now);
            entity.setUpdateTime(now);
            waylineRedisService.delRunningWaylineJob(entity.getDockSn());
            redisUtils.delete(fallbackKey);
            log.warn("Wayline job marked success by docked fallback. jobId={}, dockSn={}, progress={}, dockedMillis={}",
                    entity.getJobId(), entity.getDockSn(), progress, now - firstDockedTime);
        }
    }
}
