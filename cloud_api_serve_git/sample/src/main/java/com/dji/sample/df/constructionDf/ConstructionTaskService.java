package com.dji.sample.df.constructionDf;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.service.IWaylineRedisService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/** Local originals and analysis journal belong to the existing job, never a second flight task. */
@Service
public class ConstructionTaskService {
    @org.springframework.beans.factory.annotation.Autowired private ConstructionOverallService overall;
    private final IWaylineJobMapper jobs;
    private final PubWaylineJobPlanDfMapper plans;
    private final IFileMapperDf media;
    private final IFileService files;
    private final IWaylineRedisService redis;
    private final ObjectProvider<ConstructionAnalysisGateway> gateways;
    private final ObjectMapper json = new ObjectMapper();
    private final Path root;

    public ConstructionTaskService(IWaylineJobMapper jobs, PubWaylineJobPlanDfMapper plans,
            IFileMapperDf media, IFileService files, IWaylineRedisService redis,
            ObjectProvider<ConstructionAnalysisGateway> gateways,
            @Value("${construction.storage-root:./data/construction}") String root) {
        this.jobs = jobs; this.plans = plans; this.media = media; this.files = files;
        this.redis = redis; this.gateways = gateways;
        this.root = Paths.get(root).toAbsolutePath().normalize();
    }

    public WaylineJobEntity requireJob(String workspace, String jobId) {
        WaylineJobEntity job = jobs.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId, jobId).eq(WaylineJobEntity::getWorkspaceId, workspace));
        if (job == null) throw error(HttpStatus.NOT_FOUND, "任务不存在或不属于当前工作空间");
        PubWaylineJobPlanDfEntity plan = plans.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanId, job.getPlanId())
                .eq(PubWaylineJobPlanDfEntity::getWorkspaceId, workspace));
        if (plan == null || !ConstructionType.matches(plan.getPlanType()))
            throw error(HttpStatus.CONFLICT, "该接口仅处理工地巡检任务");
        return job;
    }

    private List<MediaFileEntity> media(WaylineJobEntity job) {
        return media.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getJobId, job.getJobId())
                .eq(MediaFileEntity::getWorkspaceId, job.getWorkspaceId()).orderByAsc(MediaFileEntity::getId));
    }

    private Path dir(WaylineJobEntity job) {
        // UUID-derived components prevent any user-supplied path from escaping the storage root.
        return root.resolve(key(job.getWorkspaceId())).resolve(key(job.getJobId()));
    }
    private String key(String value) { return UUID.nameUUIDFromBytes(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(); }
    private Path original(WaylineJobEntity job, String fileId) { return dir(job).resolve(key(fileId) + ".original"); }

    private Map<String, Object> read(Path path) throws Exception {
        return Files.exists(path) ? json.readValue(path.toFile(), new TypeReference<Map<String, Object>>() { }) : new LinkedHashMap<>();
    }
    private void write(Path path, Map<String, Object> value) throws Exception {
        Path temp = Files.createTempFile(path.getParent(), "journal-", ".tmp");
        try {
            json.writeValue(temp.toFile(), value);
            try (FileChannel ch = FileChannel.open(temp, StandardOpenOption.WRITE)) { ch.force(true); }
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
    private String checksum(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[65536]; int n;
            while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) out.append(String.format("%02x", b & 255));
        return out.toString();
    }
    private interface Work<T> { T run() throws Exception; }
    private <T> T locked(WaylineJobEntity job, Work<T> work) throws Exception {
        Files.createDirectories(dir(job));
        try (FileChannel channel = FileChannel.open(dir(job).resolve("operation.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            try (FileLock lock = channel.tryLock()) {
                if (lock == null) throw error(HttpStatus.CONFLICT, "任务正在归档或提交，请稍后刷新");
                return work.run();
            } catch (OverlappingFileLockException e) { throw error(HttpStatus.CONFLICT, "任务正在处理，请勿重复提交"); }
        }
    }

    private void archiveFiles(WaylineJobEntity job, List<MediaFileEntity> entries) throws Exception {
        Path manifestPath = dir(job).resolve("media.json");
        Map<String, Object> manifest = read(manifestPath);
        for (MediaFileEntity file : entries) {
            Path target = original(job, file.getFileId());
            Object saved = manifest.get(file.getFileId());
            if (saved != null && Files.isRegularFile(target) && saved.equals(checksum(target))) continue;
            Path temp = Files.createTempFile(dir(job), "photo-", ".part");
            try {
                URLConnection connection = files.getObjectUrl(job.getWorkspaceId(), file.getFileId()).openConnection();
                connection.setConnectTimeout(10000); connection.setReadTimeout(30000);
                try (InputStream in = connection.getInputStream()) { Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING); }
                if (Files.size(temp) == 0) throw new IllegalStateException("收到空图片文件");
                String hash = checksum(temp);
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                manifest.put(file.getFileId(), hash);
                write(manifestPath, manifest);
            } finally { Files.deleteIfExists(temp); }
        }
    }

    public void archive(WaylineJobEntity job) throws Exception {
        locked(job, () -> { archiveFiles(job, media(job)); return null; });
    }

    public Map<String, Object> detail(WaylineJobEntity job) throws Exception {
        List<MediaFileEntity> entries = media(job);
        Map<String, Object> manifest = read(dir(job).resolve("media.json"));
        List<Map<String, Object>> photos = new ArrayList<>();
        Map<String,Object> bindingSnapshot=overall==null?Map.of():overall.jobSnapshot(job.getWorkspaceId(),job.getJobId());
        for (MediaFileEntity file : entries) {
            Map<String, Object> photo = new LinkedHashMap<>();
            photo.put("file_id", file.getFileId()); photo.put("name", file.getFileName());
            photo.putAll(ConstructionOverallService.matchPhoto(bindingSnapshot,file.getFileName()));
            photo.put("archived", manifest.containsKey(file.getFileId()) && Files.isRegularFile(original(job, file.getFileId())));
            photos.add(photo);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("job_id", job.getJobId()); result.put("name", job.getName());
        result.put("plan_type", ConstructionType.PLAN_TYPE); result.put("photos", photos);
        result.put("overall_route",bindingSnapshot);
        result.put("expected_count", job.getMediaCount()); result.put("received_count", entries.size());
        result.put("media_complete", complete(job, entries));
        result.put("analysis_configured", gateways.getIfAvailable() != null);
        result.put("analysis", read(dir(job).resolve("analysis.json")));
        return result;
    }
    private boolean complete(WaylineJobEntity job, List<MediaFileEntity> entries) {
        return Objects.equals(job.getStatus(), 3) && job.getMediaCount() != null && job.getMediaCount() > 0
                && entries.size() == job.getMediaCount() && !redis.getWaylineJobMediaBase(job.getJobId()).isPresent();
    }

    public Path photo(WaylineJobEntity job, String fileId) throws Exception {
        if (media(job).stream().noneMatch(file -> Objects.equals(file.getFileId(), fileId)))
            throw error(HttpStatus.NOT_FOUND, "照片不属于当前任务");
        Path path = original(job, fileId);
        if (!Files.isRegularFile(path) || !read(dir(job).resolve("media.json")).containsKey(fileId))
            throw error(HttpStatus.NOT_FOUND, "照片尚未归档，请先保存本地原图");
        return path;
    }

    private ConstructionAnalysisGateway gateway() {
        ConstructionAnalysisGateway gateway = gateways.getIfAvailable();
        if (gateway == null) throw error(HttpStatus.SERVICE_UNAVAILABLE, "工地算法接口尚未配置，未提交分析");
        return gateway;
    }
    public boolean checkConnection() throws Exception { return gateway().checkConnection(); }

    public Map<String, Object> submit(WaylineJobEntity job, String requestId) throws Exception {
        if (requestId == null || !requestId.matches("[a-zA-Z0-9_-]{8,100}"))
            throw error(HttpStatus.BAD_REQUEST, "请提供有效的幂等请求标识");
        return locked(job, () -> {
            Path history = dir(job).resolve("batch-" + key(requestId) + ".json");
            if (Files.exists(history)) return read(history);
            Path current = dir(job).resolve("analysis.json");
            Map<String, Object> previous = read(current);
            if (requestId.equals(previous.get("request_id"))) return previous;
            if (!previous.isEmpty() && !Arrays.asList("SUCCEEDED", "FAILED").contains(previous.get("status")))
                throw error(HttpStatus.CONFLICT, "已有分析批次，请刷新其状态，勿重复下发");
            ConstructionAnalysisGateway gateway = gateway();
            if (!gateway.checkConnection()) throw error(HttpStatus.SERVICE_UNAVAILABLE, "分析主机不可达，未提交分析");
            List<MediaFileEntity> entries = media(job);
            if (!complete(job, entries)) throw error(HttpStatus.CONFLICT, "飞行未完成或媒体未收齐，不能提交分析");
            archiveFiles(job, entries);
            Map<String, Object> batch = new LinkedHashMap<>();
            String batchId = UUID.randomUUID().toString();
            batch.put("batch_id", batchId); batch.put("request_id", requestId); batch.put("job_id", job.getJobId());
            batch.put("status", "SUBMITTING"); batch.put("created_at", System.currentTimeMillis());
            batch.put("media_manifest", read(dir(job).resolve("media.json")));
            write(current, batch); write(history, batch);
            try {
                List<Path> paths = entries.stream().map(file -> original(job, file.getFileId())).collect(Collectors.toList());
                String remoteId = gateway.submit(batchId, job.getJobId(), paths);
                if (remoteId == null || remoteId.trim().isEmpty()) throw new IllegalStateException("分析主机未返回批次标识");
                batch.put("remote_id", remoteId); batch.put("status", "QUEUED");
            } catch (Exception e) {
                batch.put("status", "UNKNOWN"); batch.put("message", "提交结果待确认，请恢复连接后刷新状态，不要重复下发");
            }
            write(current, batch); write(history, batch);
            return batch;
        });
    }

    public Map<String, Object> refresh(WaylineJobEntity job) throws Exception {
        return locked(job, () -> {
            Path current = dir(job).resolve("analysis.json");
            Map<String, Object> batch = read(current);
            if (batch.isEmpty() || Arrays.asList("SUCCEEDED", "FAILED").contains(batch.get("status"))) return batch;
            Map<String, Object> response = gateway().query((String) batch.get("batch_id"), (String) batch.get("remote_id"));
            if (response == null || !Arrays.asList("QUEUED", "RUNNING", "SUCCEEDED", "FAILED").contains(response.get("status")))
                throw error(HttpStatus.BAD_GATEWAY, "分析主机返回状态无效，本地批次保持原状");
            if ("SUCCEEDED".equals(response.get("status")) && !(response.get("result") instanceof Map))
                throw error(HttpStatus.BAD_GATEWAY, "分析结果未完整返回，不能标记成功");
            batch.put("status", response.get("status")); batch.put("result", response.get("result"));
            batch.put("message", response.get("message")); batch.put("updated_at", System.currentTimeMillis());
            write(current, batch);
            write(dir(job).resolve("batch-" + key((String) batch.get("request_id")) + ".json"), batch);
            return batch;
        });
    }
    private static ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
}
