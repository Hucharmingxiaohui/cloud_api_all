package com.dji.sample.df.cqDockDf.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.cqDockDf.dao.CqDockTaskPictureMapper;
import com.dji.sample.df.cqDockDf.dao.CqDockTaskRecordMapper;
import com.dji.sample.df.cqDockDf.dao.CqEuaPlanMapper;
import com.dji.sample.df.cqDockDf.model.dto.CqDockTaskDispatchResult;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaBayOption;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaPlanAddRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaPlanPageRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaPlanUpdateRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaTaskPictureListRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaTaskRecordPageRequest;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskPictureEntity;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskRecordEntity;
import com.dji.sample.df.cqDockDf.model.entity.CqEuaPlanEntity;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import com.dji.sample.df.windDf.config.FjFileConfig;
import com.dji.sample.df.windDf.utils.FileNameUtils;
import com.dji.sample.df.cqDockDf.config.CqDockProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CqEuaPlanService {

    @Resource
    private FjFileConfig fjFileConfig;

    @Resource
    private CqDockProperties cqDockProperties;

    @Resource
    private CqEuaPlanMapper cqEuaPlanMapper;

    @Resource
    private UniPointMapper2 uniPointMapper2;

    @Resource
    private CqDockTaskDispatchService cqDockTaskDispatchService;

    @Resource
    private CqDockTaskRecordMapper cqDockTaskRecordMapper;

    @Resource
    private CqDockTaskPictureMapper cqDockTaskPictureMapper;

    public IPage<CqEuaPlanEntity> page(CqEuaPlanPageRequest request) {
        int page = request.getPage() == null || request.getPage() <= 0 ? 1 : request.getPage();
        int pageSize = request.getPageSize() == null || request.getPageSize() <= 0 ? 10 : request.getPageSize();
        LambdaQueryWrapper<CqEuaPlanEntity> wrapper = new LambdaQueryWrapper<CqEuaPlanEntity>()
                .like(StringUtils.hasText(request.getPlanName()), CqEuaPlanEntity::getPlanName, request.getPlanName())
                .like(StringUtils.hasText(request.getBayName()), CqEuaPlanEntity::getBayName, request.getBayName())
                .orderByDesc(CqEuaPlanEntity::getCreateTime)
                .orderByDesc(CqEuaPlanEntity::getId);
        IPage<CqEuaPlanEntity> result = cqEuaPlanMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<String> taskCodes = result.getRecords().stream().map(CqEuaPlanEntity::getLastTaskCode)
                .filter(StringUtils::hasText).distinct().collect(Collectors.toList());
        if (!taskCodes.isEmpty()) {
            Map<String, String> names = new LinkedHashMap<>();
            cqDockTaskRecordMapper.selectList(new LambdaQueryWrapper<CqDockTaskRecordEntity>()
                    .in(CqDockTaskRecordEntity::getBusinessId, taskCodes)
                    .orderByDesc(CqDockTaskRecordEntity::getId))
                    .forEach(record -> names.putIfAbsent(record.getBusinessId(), record.getTaskName()));
            result.getRecords().forEach(plan -> plan.setLastTaskName(names.get(plan.getLastTaskCode())));
        }
        return result;
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
        long timestamp = System.currentTimeMillis();
        String taskCode = "EUA_PLAN_" + plan.getId() + "_" + timestamp;
        String taskName = plan.getPlanName() + "_" + timestamp;
        String fixedStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        CqDockTaskDispatchResult result = cqDockTaskDispatchService.dispatchByBay(plan.getBayId(), taskCode, taskName, fixedStartTime, false);
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

    public IPage<CqDockTaskRecordEntity> taskRecordPage(CqEuaTaskRecordPageRequest request) {
        int page = request.getPage() == null || request.getPage() <= 0 ? 1 : request.getPage();
        int pageSize = request.getPageSize() == null || request.getPageSize() <= 0 ? 10 : request.getPageSize();
        String businessPrefix = request.getPlanId() == null ? null : "EUA_PLAN_" + request.getPlanId() + "_";
        LambdaQueryWrapper<CqDockTaskRecordEntity> wrapper = new LambdaQueryWrapper<CqDockTaskRecordEntity>()
                .likeRight(CqDockTaskRecordEntity::getBusinessId, "EUA_PLAN_")
                .likeRight(StringUtils.hasText(businessPrefix), CqDockTaskRecordEntity::getBusinessId, businessPrefix)
                .like(StringUtils.hasText(request.getTaskName()), CqDockTaskRecordEntity::getTaskName, request.getTaskName())
                .like(StringUtils.hasText(request.getEuaTaskId()), CqDockTaskRecordEntity::getEuaTaskId, request.getEuaTaskId())
                .orderByDesc(CqDockTaskRecordEntity::getCreateTime)
                .orderByDesc(CqDockTaskRecordEntity::getId);
        IPage<CqDockTaskRecordEntity> result = cqDockTaskRecordMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Map<Long, List<CqDockTaskRecordEntity>> byPlan = new LinkedHashMap<>();
        for (CqDockTaskRecordEntity record : result.getRecords()) {
            String code = record.getBusinessId();
            if (code != null && code.matches("EUA_PLAN_[0-9]+_[0-9]+")) {
                try {
                    Long planId = Long.valueOf(code.split("_")[2]);
                    byPlan.computeIfAbsent(planId, key -> new ArrayList<>()).add(record);
                } catch (NumberFormatException ignored) {
                    // 非法历史编码不关联计划。
                }
            }
        }
        if (!byPlan.isEmpty()) {
            cqEuaPlanMapper.selectBatchIds(byPlan.keySet()).forEach(plan ->
                    byPlan.get(plan.getId()).forEach(record -> record.setPlanName(plan.getPlanName())));
        }
        return result;
    }

    public List<CqDockTaskPictureEntity> taskPictureList(CqEuaTaskPictureListRequest request) {
        if (!StringUtils.hasText(request.getBusinessId()) && !StringUtils.hasText(request.getEuaTaskId())) {
            throw new IllegalArgumentException("任务编码或EUA任务ID不能为空");
        }
        LambdaQueryWrapper<CqDockTaskPictureEntity> wrapper = new LambdaQueryWrapper<CqDockTaskPictureEntity>()
                .eq(StringUtils.hasText(request.getBusinessId()), CqDockTaskPictureEntity::getBusinessId, request.getBusinessId())
                .eq(StringUtils.hasText(request.getEuaTaskId()), CqDockTaskPictureEntity::getEuaTaskId, request.getEuaTaskId())
                .orderByAsc(CqDockTaskPictureEntity::getPointId)
                .orderByAsc(CqDockTaskPictureEntity::getPictureId)
                .orderByAsc(CqDockTaskPictureEntity::getId);
        return cqDockTaskPictureMapper.selectList(wrapper);
    }

    public org.springframework.core.io.Resource taskPictureImage(Long id, boolean analysis) {
        CqDockTaskPictureEntity row = cqDockTaskPictureMapper.selectById(id);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片记录不存在");
        }
        String storedPath = analysis ? row.getAnalysisImageUrl() : row.getLocalPath();
        if (!StringUtils.hasText(storedPath) || !StringUtils.hasText(fjFileConfig.getRecfilePath())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片尚未生成或不可用");
        }
        try {
            Path root = Paths.get(fjFileConfig.getRecfilePath()).toRealPath();
            String value = storedPath.replace('\\', '/');
            Path path;
            if (value.startsWith("http://") || value.startsWith("https://")) {
                // 复用回调下载的结果图，不代理远程URL，避免预览触发服务端任意请求。
                String name = FileNameUtils.convertChineseToPinyinInitials(row.getPictureName());
                String analysisName = name.contains(".") ? name.replaceAll("(\\.[^.]+)$", "_analysis$1") : name + "_analysis.jpg";
                path = Paths.get(cqDockProperties.getPictureLocalPath(), row.getBusinessId(), "analysis", analysisName);
            } else {
                Path stored = Paths.get(value);
                if (stored.isAbsolute() && stored.normalize().startsWith(root)) {
                    path = stored;
                } else if (analysis) {
                    path = root.resolve(value.replaceFirst("^/+", ""));
                } else {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "原图不在图片存储目录");
                }
            }
            path = path.toRealPath();
            if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片路径无效");
            }
            return new FileSystemResource(path);
        } catch (java.io.IOException | java.nio.file.InvalidPathException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片文件不存在", e);
        }
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
