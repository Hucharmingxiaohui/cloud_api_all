package com.dji.sample.df.constructionDf;

import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.service.IWaylineRedisService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

/** Offline executable regression checks; no DB, device or algorithm requests. */
public class ConstructionTaskServiceChecks {
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Function<String, Object> call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, method, args) -> call.apply(method.getName()));
    }
    private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
    private interface Action { void run() throws Exception; }
    private static void rejects(int status, Action action) throws Exception {
        try { action.run(); throw new AssertionError("expected rejection " + status); }
        catch (ResponseStatusException e) { check(e.getStatus().value() == status, e.toString()); }
    }
    public static void main(String[] args) throws Exception {
        for (Class<?> type : List.of(MediaFileEntity.class, WaylineJobEntity.class, PubWaylineJobPlanDfEntity.class)) {
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                    new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(), "test"), type);
        }
        Path temp = Files.createTempDirectory("construction-check-");
        Path source = temp.resolve("source.jpg"); byte[] original = {1, 2, 3, 4, 5}; Files.write(source, original);
        WaylineJobEntity job = new WaylineJobEntity(); job.setJobId("job/../1"); job.setWorkspaceId("workspace");
        job.setPlanId("plan"); job.setStatus(3); job.setMediaCount(1);
        PubWaylineJobPlanDfEntity plan = new PubWaylineJobPlanDfEntity(); plan.setPlanType(6);
        MediaFileEntity photo = new MediaFileEntity(); photo.setFileId("photo/../1"); photo.setFileName("DJI.jpg");
        IWaylineJobMapper jobs = proxy(IWaylineJobMapper.class, name -> job);
        PubWaylineJobPlanDfMapper plans = proxy(PubWaylineJobPlanDfMapper.class, name -> plan);
        IFileMapperDf media = proxy(IFileMapperDf.class, name -> List.of(photo));
        IFileService files = proxy(IFileService.class, name -> { try { return source.toUri().toURL(); } catch (Exception e) { throw new RuntimeException(e); } });
        IWaylineRedisService redis = proxy(IWaylineRedisService.class, name -> Optional.empty());
        ConstructionAnalysisGateway[] configured = {null};
        ObjectProvider<ConstructionAnalysisGateway> provider = proxy(ObjectProvider.class, name -> configured[0]);
        ConstructionTaskService service = new ConstructionTaskService(jobs, plans, media, files, redis, provider, temp.resolve("data").toString());
        rejects(503, () -> service.submit(job, "request-001"));
        check(!service.detail(job).get("analysis_configured").equals(true), "unconfigured gateway must be explicit");
        plan.setPlanType(4); rejects(409, () -> service.requireJob("workspace", "job")); plan.setPlanType(6);
        service.archive(job);
        Path saved = service.photo(job, photo.getFileId());
        check(saved.startsWith(temp.resolve("data")), "paths must stay under local root");
        check(Arrays.equals(original, Files.readAllBytes(saved)), "original bytes must be preserved");
        Files.write(saved, new byte[]{9}); service.archive(job);
        check(Arrays.equals(original, Files.readAllBytes(saved)), "checksum detects and repairs damaged originals");
        rejects(404, () -> service.photo(job, "another-task-file"));
        int[] submissions = {0}; boolean[] timeout = {false};
        configured[0] = new ConstructionAnalysisGateway() {
            public boolean checkConnection() { return true; }
            public String submit(String batch, String id, List<Path> originals) throws Exception {
                submissions[0]++; if (timeout[0]) throw new java.io.IOException("ambiguous response"); return "remote";
            }
            public Map<String, Object> query(String batch, String remote) { return Map.of("status", "SUCCEEDED", "result", Map.of("detections", List.of())); }
        };
        job.setMediaCount(2); rejects(409, () -> service.submit(job, "request-001")); job.setMediaCount(1);
        Map<String, Object> batch = service.submit(job, "request-001");
        check("QUEUED".equals(batch.get("status")), "accepted batch queued");
        check(service.submit(job, "request-001").get("batch_id").equals(batch.get("batch_id")), "same request reuses batch");
        rejects(409, () -> service.submit(job, "request-002")); check(submissions[0] == 1, "never duplicate active work");
        service.refresh(job);
        ConstructionTaskService restarted = new ConstructionTaskService(jobs, plans, media, files, redis, provider, temp.resolve("data").toString());
        check(((Map<?, ?>) restarted.detail(job).get("analysis")).get("status").equals("SUCCEEDED"), "result survives service restart");
        timeout[0] = true;
        check("UNKNOWN".equals(restarted.submit(job, "request-002").get("status")), "timeout is not failure or success");
        rejects(409, () -> restarted.submit(job, "request-003"));
        restarted.refresh(job); check(submissions[0] == 2, "refresh resolves uncertainty without resubmitting");
        check(restarted.submit(job, "request-001").get("batch_id").equals(batch.get("batch_id")), "historical retry returns original batch");
        System.out.println("Construction backend checks passed: type isolation, disabled adapter, raw archive, checksum, path confinement, incomplete media, idempotency, timeout recovery, restart persistence.");
    }
}
