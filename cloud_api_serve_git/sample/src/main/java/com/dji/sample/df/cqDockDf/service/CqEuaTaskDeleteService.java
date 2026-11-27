package com.dji.sample.df.cqDockDf.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.dji.sample.df.cqDockDf.config.CqDockProperties;
import com.dji.sample.df.cqDockDf.controller.CqDockMockController;
import com.dji.sample.df.cqDockDf.dao.*;
import com.dji.sample.df.cqDockDf.model.entity.*;
import com.dji.sample.df.manageDf.dao.IWorkspaceMapperDf;
import com.dji.sample.df.manageDf.model.entity.WorkspaceEntity;
import com.dji.sample.wayline.dao.IWaylineFileMapper;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 仅清理平台执行的本地数据，不调用下级停止或删除接口。 */
@Slf4j
@Service
public class CqEuaTaskDeleteService {
    private final CqDockTaskRecordMapper records;
    private final CqDockTaskPictureMapper pictures;
    private final CqEuaPlanMapper plans;
    private final IWaylineFileMapper routes;
    private final IWorkspaceMapperDf workspaces;
    private final CqDockPictureReportService processing;
    private final CqDockTaskStatusHandler monitoring;
    private final CqEuaTaskReportService reports;
    private final ObjectProvider<CqDockMockController> mock;
    private final CqDockProperties config;
    private final TransactionTemplate transaction;

    public CqEuaTaskDeleteService(CqDockTaskRecordMapper records, CqDockTaskPictureMapper pictures,
            CqEuaPlanMapper plans, IWaylineFileMapper routes, IWorkspaceMapperDf workspaces, CqDockPictureReportService processing,
            CqDockTaskStatusHandler monitoring, CqEuaTaskReportService reports,
            ObjectProvider<CqDockMockController> mock, CqDockProperties config,
            PlatformTransactionManager transactionManager) {
        this.records = records; this.pictures = pictures; this.plans = plans; this.routes = routes;
        this.workspaces = workspaces;
        this.processing = processing; this.monitoring = monitoring; this.reports = reports;
        this.mock = mock; this.config = config; this.transaction = new TransactionTemplate(transactionManager);
    }

    public synchronized void delete(Long id, String workspace) {
        if (!StringUtils.hasText(workspace)) throw new IllegalArgumentException("请先登录工作空间");
        CqDockTaskRecordEntity initial = requirePlatform(id, workspace);
        Lock dispatch = processing.taskLock(null, initial.getBusinessId());
        dispatch.lock();
        try {
            // 下发可能刚返回并补写了EUA任务ID，必须重新取记录再获取图片处理锁。
            CqDockTaskRecordEntity row = requirePlatform(id, workspace);
            Lock task = processing.taskLock(row.getEuaTaskId(), row.getBusinessId());
            task.lock();
            try {
                synchronized (reports) {
                    transaction.executeWithoutResult(status -> {
                        requirePlatform(id, workspace);
                        List<Path> files = taskFiles(config.getPictureLocalPath(), row.getBusinessId(),
                                config.getMockScenario().getSourcePath(), config.getMockPicturePath());
                        monitoring.removeMonitoring(row.getBusinessId());
                        reports.deleteTaskFiles(row);
                        try {
                            for (Path file : files) Files.deleteIfExists(file);
                        } catch (IOException e) {
                            throw new IllegalStateException("任务图片清理失败，请重试删除", e);
                        }
                        pictures.delete(new LambdaQueryWrapper<CqDockTaskPictureEntity>()
                                .eq(CqDockTaskPictureEntity::getBusinessId, row.getBusinessId()));
                        if (records.deleteById(id) != 1) throw new IllegalStateException("任务记录删除失败");
                        restoreLatest(row);
                    });
                    CqDockMockController controller = mock.getIfAvailable();
                    if (controller != null) controller.forgetTask(row.getEuaTaskId());
                }
                log.info("EUA平台执行及关联本地数据已删除，不停止真实下级任务：recordId={}, planId={}", id, row.getPlanId());
            } finally { task.unlock(); }
        } finally { dispatch.unlock(); }
    }

    private CqDockTaskRecordEntity requirePlatform(Long id, String workspace) {
        CqDockTaskRecordEntity row = id == null ? null : records.selectById(id);
        if (row == null) throw new IllegalArgumentException("任务记录不存在");
        if (!"PLATFORM".equals(row.getTaskSource()) || !Objects.equals(row.getReportToPatrol(), 0)) {
            throw new IllegalArgumentException("仅允许删除平台创建的EUA执行记录，巡视任务禁止删除");
        }
        if (row.getBusinessId() == null || !row.getBusinessId().matches("EUA_PLAN_[0-9]+_[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("平台任务编码无效，禁止清理");
        }
        if (records.selectCount(new LambdaQueryWrapper<CqDockTaskRecordEntity>()
                .eq(CqDockTaskRecordEntity::getBusinessId, row.getBusinessId())) != 1
                || (StringUtils.hasText(row.getEuaTaskId()) && records.selectCount(new LambdaQueryWrapper<CqDockTaskRecordEntity>()
                .eq(CqDockTaskRecordEntity::getEuaTaskId, row.getEuaTaskId())) != 1)) {
            throw new IllegalArgumentException("任务标识不唯一，禁止删除共享数据");
        }
        CqEuaPlanEntity plan = row.getPlanId() == null ? null : plans.selectById(row.getPlanId());
        if (plan != null && !"PLATFORM".equals(plan.getPlanSource())) throw new IllegalArgumentException("巡视计划执行禁止删除");
        // 校验失败的执行可能尚未冻结航线，只允许回退到关联平台计划的服务端航线。
        String routeId = StringUtils.hasText(row.getRouteId()) ? row.getRouteId() : plan == null ? null : plan.getRouteId();
        List<WaylineFileEntity> linkedRoutes = StringUtils.hasText(routeId)
                ? routes.selectList(new LambdaQueryWrapper<WaylineFileEntity>()
                .eq(WaylineFileEntity::getWaylineId, routeId).last("LIMIT 2")) : Collections.emptyList();
        if (!linkedRoutes.isEmpty()) {
            if (linkedRoutes.size() != 1 || !Objects.equals(workspace, linkedRoutes.get(0).getWorkspaceId())) {
                throw new IllegalArgumentException("任务航线归属不唯一或无工作空间权限");
            }
        } else {
            // EUA航线不要求存在DJI航线表；沿用CenterPatrolService的唯一工作空间策略，不猜测多租户归属。
            List<WorkspaceEntity> available = workspaces.selectList(new QueryWrapper<WorkspaceEntity>().last("LIMIT 2"));
            if (available.size() != 1 || !StringUtils.hasText(available.get(0).getWorkspaceId())) {
                throw new IllegalArgumentException("EUA工作空间不存在或不唯一，禁止猜测任务归属");
            }
            if (!Objects.equals(workspace, available.get(0).getWorkspaceId())) {
                throw new IllegalArgumentException("无EUA任务工作空间权限");
            }
            log.info("EUA删除权限已按服务端唯一工作空间确认：recordId={}, workspaceId={}", id, workspace);
        }
        return row;
    }

    private void restoreLatest(CqDockTaskRecordEntity removed) {
        List<CqEuaPlanEntity> linked = plans.selectList(new LambdaQueryWrapper<CqEuaPlanEntity>()
                .eq(CqEuaPlanEntity::getLastTaskCode, removed.getBusinessId()).last("FOR UPDATE"));
        for (CqEuaPlanEntity plan : linked) {
            if (!"PLATFORM".equals(plan.getPlanSource())) throw new IllegalStateException("任务关联非平台计划，已回滚删除");
            List<CqDockTaskRecordEntity> remaining = records.selectList(new LambdaQueryWrapper<CqDockTaskRecordEntity>()
                    .eq(CqDockTaskRecordEntity::getTaskSource, "PLATFORM")
                    .and(w -> w.eq(CqDockTaskRecordEntity::getPlanId, plan.getId()).or()
                            .isNull(CqDockTaskRecordEntity::getPlanId))
                    .orderByDesc(CqDockTaskRecordEntity::getCreateTime).orderByDesc(CqDockTaskRecordEntity::getId));
            // SQL LIKE中的下划线是通配符，历史无planId记录须按完整编码前缀匹配。
            CqDockTaskRecordEntity previous = remaining.stream().filter(record -> Objects.equals(record.getPlanId(), plan.getId())
                    || (record.getPlanId() == null && record.getBusinessId() != null
                    && record.getBusinessId().startsWith("EUA_PLAN_" + plan.getId() + "_"))).findFirst().orElse(null);
            int updated = plans.update(null, new LambdaUpdateWrapper<CqEuaPlanEntity>()
                    .eq(CqEuaPlanEntity::getId, plan.getId()).eq(CqEuaPlanEntity::getLastTaskCode, removed.getBusinessId())
                    .set(CqEuaPlanEntity::getLastTaskCode, previous == null ? null : previous.getBusinessId())
                    .set(CqEuaPlanEntity::getLastEuaTaskId, previous == null ? null : previous.getEuaTaskId())
                    .set(CqEuaPlanEntity::getLastExecuteTime, previous == null ? null : previous.getCreateTime())
                    .set(CqEuaPlanEntity::getLastExecuteStatus, previous == null ? null : Objects.equals(previous.getSuccess(), 1) ? 2 : 0)
                    .set(CqEuaPlanEntity::getLastExecuteMsg, previous == null ? null : previous.getResponseMsg())
                    .set(CqEuaPlanEntity::getUpdateTime, new Date()));
            if (updated != 1) throw new IllegalStateException("计划最新执行恢复失败，请重试");
        }
    }

    static List<Path> taskFiles(String configuredRoot, String business, String... protectedRoots) {
        if (!StringUtils.hasText(configuredRoot) || !business.matches("EUA_PLAN_[0-9]+_[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("任务图片目录配置或任务编码无效");
        }
        try {
            Path root = Paths.get(configuredRoot).toAbsolutePath().normalize();
            if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return Collections.emptyList();
            if (Files.isSymbolicLink(root) || !root.equals(root.toRealPath())) throw new IOException("图片根目录包含链接");
            Path directory = root.resolve(business).normalize();
            for (String protectedRoot : protectedRoots) {
                if (!StringUtils.hasText(protectedRoot)) continue;
                Path source = Paths.get(protectedRoot).toAbsolutePath().normalize();
                if (Files.exists(source)) source = source.toRealPath();
                if (directory.startsWith(source) || source.startsWith(directory)) throw new IOException("禁止删除共享素材目录");
            }
            if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return Collections.emptyList();
            try (Stream<Path> walk = Files.walk(directory)) {
                List<Path> paths = walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
                for (Path path : paths) {
                    if (Files.isSymbolicLink(path) || !path.toRealPath().startsWith(directory)) throw new IOException("任务目录包含越界链接");
                }
                return paths;
            }
        } catch (IOException | InvalidPathException e) {
            throw new IllegalStateException("任务图片目录不安全或不可读取，禁止删除", e);
        }
    }
}
