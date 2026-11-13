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
        if (jobId.startsWith("offline-")) {
            try {
                Map<String,Object> record = imported(workspace, jobId);
                if (record.isEmpty()) throw error(HttpStatus.NOT_FOUND, "离线任务不存在或不属于当前工作空间");
                Map<String,Object> task = (Map<String,Object>)record.get("task");
                WaylineJobEntity job = json.convertValue(task, WaylineJobEntity.class);
                job.setId(null); job.setJobId(jobId); job.setWorkspaceId(workspace); job.setPlanId(null);
                job.setStatus(3); return job;
            } catch (java.io.IOException e) { throw error(HttpStatus.INTERNAL_SERVER_ERROR, "离线任务记录无法读取"); }
            catch (ResponseStatusException e) { throw e; }
            catch (Exception e) { throw error(HttpStatus.INTERNAL_SERVER_ERROR, "离线任务记录无效"); }
        }
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

    // Check the task row directly: a removed plan or missing photos must not be
    // mistaken for a deleted task, and database failures must stop the import.
    boolean sourceTaskExists(String workspace, String jobId) {
        return jobs.selectCount(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId, jobId)
                .eq(WaylineJobEntity::getWorkspaceId, workspace)) > 0;
    }

    private List<MediaFileEntity> media(WaylineJobEntity job) {
        if (job.getJobId().startsWith("offline-")) {
            try {
                List<Map<String,Object>> records = (List<Map<String,Object>>) imported(job.getWorkspaceId(),job.getJobId()).get("photos");
                return records.stream().map(p -> MediaFileEntity.builder().fileId((String)p.get("id"))
                        .fileName((String)p.get("name")).subFileType(p.get("sub_file_type") instanceof Number ? ((Number)p.get("sub_file_type")).intValue() : null).drone((String)p.get("drone")).payload((String)p.get("payload")).workspaceId(job.getWorkspaceId()).jobId(job.getJobId()).build()).collect(Collectors.toList());
            } catch (Exception e) { throw error(HttpStatus.INTERNAL_SERVER_ERROR, "离线照片记录无法读取"); }
        }
        return media.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getJobId, job.getJobId())
                .eq(MediaFileEntity::getWorkspaceId, job.getWorkspaceId()).orderByAsc(MediaFileEntity::getId));
    }

    Path dir(WaylineJobEntity job) {
        // UUID-derived components prevent any user-supplied path from escaping the storage root.
        return root.resolve(key(job.getWorkspaceId())).resolve(key(job.getJobId()));
    }
    private String key(String value) { return UUID.nameUUIDFromBytes(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString(); }
    Map<String,Object> imported(String workspace, String jobId) throws Exception {
        if (!jobId.matches("offline-[a-f0-9-]{36}")) return Map.of();
        return read(root.resolve(key(workspace)).resolve(key(jobId)).resolve("transfer.json"));
    }
    private Map<String,Object> snapshot(WaylineJobEntity job) throws Exception {
        if (job.getJobId().startsWith("offline-")) return (Map<String,Object>)imported(job.getWorkspaceId(),job.getJobId()).get("snapshot");
        return overall == null ? Map.of() : overall.jobSnapshot(job.getWorkspaceId(),job.getJobId());
    }
    Path storageRoot() { return root; }
    private Path legacyOriginal(WaylineJobEntity job, String fileId) {
        return dir(job).resolve(key(fileId) + ".original");
    }

    private Path original(WaylineJobEntity job, MediaFileEntity file) {
        String name = Optional.ofNullable(file.getFileName()).orElse("");
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!Set.of("jpg", "jpeg", "png", "tif", "tiff", "dng", "bmp", "webp", "heic", "heif").contains(extension))
            throw error(HttpStatus.CONFLICT, "原照片缺少受支持的图片扩展名，请核对媒体文件名：" + name);
        return dir(job).resolve(key(file.getFileId()) + "." + extension);
    }

    private Path readableOriginal(WaylineJobEntity job, MediaFileEntity file) {
        Path path = original(job, file);
        return Files.isRegularFile(path) ? path : legacyOriginal(job, file.getFileId());
    }

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
            Path target = original(job, file);
            Object saved = manifest.get(file.getFileId());
            if (saved != null && Files.isRegularFile(target) && saved.equals(checksum(target))) continue;
            if (job.getJobId().startsWith("offline-")) throw error(HttpStatus.CONFLICT, "离线原图缺失或损坏，请恢复本地归档文件");
            Path temp = Files.createTempFile(dir(job), "photo-", ".part");
            try {
                Path legacy = legacyOriginal(job, file.getFileId());
                if (saved != null && Files.isRegularFile(legacy) && saved.equals(checksum(legacy))) {
                    // Keep the legacy file for any already submitted batch that still references it.
                    Files.copy(legacy, temp, StandardCopyOption.REPLACE_EXISTING);
                } else {
                URLConnection connection = files.getObjectUrl(job.getWorkspaceId(), file.getFileId()).openConnection();
                connection.setConnectTimeout(10000); connection.setReadTimeout(30000);
                try (InputStream in = connection.getInputStream()) { Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING); }
                }
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
        Map<String,Object> bindingSnapshot=snapshot(job);
        Map<String,Object> associationJournal=read(dir(job).resolve("associations.json"));
        Map<String,Object> decisions=decisions(associationJournal);
        for (MediaFileEntity file : entries) {
            Map<String, Object> photo = new LinkedHashMap<>();
            photo.put("file_id", file.getFileId()); photo.put("name", file.getFileName());
            photo.putAll(ConstructionPhotoAssociation.resolve(bindingSnapshot,file.getFileId(),file.getFileName(),decisions));
            photo.put("archived", manifest.containsKey(file.getFileId()) && Files.isRegularFile(readableOriginal(job, file)));
            photos.add(photo);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("job_id", job.getJobId()); result.put("name", job.getName());
        result.put("plan_type", ConstructionType.PLAN_TYPE); result.put("photos", photos);
        result.put("overall_route",bindingSnapshot);
        result.put("expected_count", job.getMediaCount()); result.put("received_count", entries.size());
        result.put("media_complete", complete(job, entries));
        ConstructionAnalysisGateway configured=gateways.getIfAvailable();
        result.put("analysis_configured", configured instanceof ConstructionNativeAnalysisGateway?((ConstructionNativeAnalysisGateway)configured).configured():configured!=null);
        Map<String,Object> analysis=read(dir(job).resolve("analysis.json"));
        result.put("analysis",analysis);
        if (job.getJobId().startsWith("offline-")) result.put("transfer_source", imported(job.getWorkspaceId(),job.getJobId()).get("origin"));
        result.put("association_revision",revision(associationJournal));
        result.put("unresolved_count",bindingSnapshot.isEmpty()?0:photos.stream().filter(p->!p.containsKey("area_id")&&!Boolean.TRUE.equals(p.get("excluded"))).count());
        result.put("analysis_stale",!analysis.isEmpty()&&revision(associationJournal)!=((Number)analysis.getOrDefault("association_revision",0)).intValue());
        return result;
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> decisions(Map<String,Object> journal) { return (Map<String,Object>)journal.getOrDefault("photos",new LinkedHashMap<String,Object>()); }
    private int revision(Map<String,Object> journal) { return ((Number)journal.getOrDefault("revision",0)).intValue(); }
    public void assignPhotos(WaylineJobEntity job,Map<String,Object> body,String user) throws Exception {
        locked(job,()->{
            if(gateways.getIfAvailable() instanceof ConstructionNativeAnalysisGateway)
                for(Map<String,Object> active:nativeGateway().batches(job))
                    if(!List.of("SUCCEEDED","FAILED").contains(String.valueOf(active.get("status"))))
                        throw error(HttpStatus.CONFLICT,"分析批次尚未结束或提交结果待确认，暂不能修改照片归属");
            Map<String,Object> batch=read(dir(job).resolve("analysis.json"));
            if(!batch.isEmpty()&&!List.of("SUCCEEDED","FAILED").contains(String.valueOf(batch.get("status"))))throw error(HttpStatus.CONFLICT,"分析批次尚未结束，暂不能修改照片归属");
            Map<String,Object> journal=read(dir(job).resolve("associations.json"));
            if(!(body.get("expected_revision") instanceof Number)||((Number)body.get("expected_revision")).doubleValue()!=revision(journal))throw error(HttpStatus.CONFLICT,"照片归属已更新，请刷新后重试");
            Object raw=body.get("file_ids");
            if(!(raw instanceof List)||((List<?>)raw).isEmpty()||((List<?>)raw).size()>500)throw error(HttpStatus.BAD_REQUEST,"请选择1–500张照片");
            Set<String> ids=new HashSet<>();for(Object id:(List<?>)raw){if(!(id instanceof String)||!ids.add((String)id))throw error(HttpStatus.BAD_REQUEST,"照片标识无效或重复");}
            Set<String> actual=media(job).stream().map(MediaFileEntity::getFileId).collect(Collectors.toSet());
            if(!actual.containsAll(ids))throw error(HttpStatus.BAD_REQUEST,"照片不属于本次任务");
            Map<String,Object> snapshot=snapshot(job);
            Map<String,Object> decision=ConstructionPhotoAssociation.decision(snapshot,body,user);
            Map<String,Object> values=new LinkedHashMap<>(decisions(journal));for(String id:ids)values.put(id,new LinkedHashMap<>(decision));
            journal.put("revision",revision(journal)+1);journal.put("photos",values);write(dir(job).resolve("associations.json"),journal);return null;
        });
    }
    private boolean complete(WaylineJobEntity job, List<MediaFileEntity> entries) {
        return Objects.equals(job.getStatus(), 3) && job.getMediaCount() != null && job.getMediaCount() > 0
                && entries.size() == job.getMediaCount() && (job.getJobId().startsWith("offline-") || !redis.getWaylineJobMediaBase(job.getJobId()).isPresent());
    }

    public Path photo(WaylineJobEntity job, String fileId) throws Exception {
        MediaFileEntity file = media(job).stream().filter(item -> Objects.equals(item.getFileId(), fileId))
                .findFirst().orElseThrow(() -> error(HttpStatus.NOT_FOUND, "照片不属于当前任务"));
        Path path = readableOriginal(job, file);
        if (!Files.isRegularFile(path) || !read(dir(job).resolve("media.json")).containsKey(fileId))
            throw error(HttpStatus.NOT_FOUND, "照片尚未归档，请先保存本地原图");
        return path;
    }

    private ConstructionAnalysisGateway gateway() {
        ConstructionAnalysisGateway gateway = gateways.getIfAvailable();
        if (gateway == null) throw error(HttpStatus.SERVICE_UNAVAILABLE, "工地算法接口尚未配置，未提交分析");
        return gateway;
    }
    public boolean checkConnection(WaylineJobEntity job)throws Exception {return gateway() instanceof ConstructionNativeAnalysisGateway?nativeGateway().checkConnection(job):checkConnection();}
    public Map<String,Object> checkCapabilities(WaylineJobEntity job)throws Exception {
        if(gateway() instanceof ConstructionNativeAnalysisGateway)return nativeGateway().checkCapabilities(job);
        boolean ready=checkConnection();return Map.of("connected",ready,"message",ready?"分析主机连接正常":"分析主机连接失败");
    }
    public boolean checkConnection() throws Exception { return gateway().checkConnection(); }

    public Map<String, Object> submit(WaylineJobEntity job, String requestId) throws Exception {
        if (requestId == null || !requestId.matches("[a-zA-Z0-9_-]{8,100}"))
            throw error(HttpStatus.BAD_REQUEST, "请提供有效的幂等请求标识");
        if(gateway() instanceof ConstructionNativeAnalysisGateway) return locked(job,()->{
            for(Map<String,Object> old:batches(job))if(requestId.equals(old.get("request_id")))return old;
            Map<String,Object> legacy=read(dir(job).resolve("analysis.json"));
            if(!legacy.isEmpty()&&!Boolean.TRUE.equals(legacy.get("native_adapter"))&&!List.of("SUCCEEDED","FAILED").contains(legacy.get("status")))
                throw error(HttpStatus.CONFLICT,"旧分析批次结果待确认，请先人工核对，不会自动重新提交");
            List<MediaFileEntity> entries=media(job);
            if(!complete(job,entries))throw error(HttpStatus.CONFLICT,"飞行未完成或媒体未收齐，不能提交分析");
            return remember(job,((ConstructionNativeAnalysisGateway)gateway()).submit(job,requestId));
        });
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
            Map<String,Object> associationJournal=read(dir(job).resolve("associations.json"));
            Map<String,Object> bindingSnapshot=snapshot(job);
            Map<String,Object> frozenAssociations=new LinkedHashMap<>();List<MediaFileEntity> analyzed=new ArrayList<>();
            for(MediaFileEntity file:entries){
                Map<String,Object> association=ConstructionPhotoAssociation.resolve(bindingSnapshot,file.getFileId(),file.getFileName(),decisions(associationJournal));
                if(!bindingSnapshot.isEmpty()&&!association.containsKey("area_id")&&!Boolean.TRUE.equals(association.get("excluded")))throw error(HttpStatus.CONFLICT,"存在待确认工作面的照片，请先确认归属或填写排除原因");
                frozenAssociations.put(file.getFileId(),association);
                if(!Boolean.TRUE.equals(association.get("excluded")))analyzed.add(file);
            }
            if(analyzed.isEmpty())throw error(HttpStatus.CONFLICT,"没有可分析的照片");
            archiveFiles(job, entries);
            Map<String, Object> batch = new LinkedHashMap<>();
            String batchId = UUID.randomUUID().toString();
            batch.put("batch_id", batchId); batch.put("request_id", requestId); batch.put("job_id", job.getJobId());
            batch.put("status", "SUBMITTING"); batch.put("created_at", System.currentTimeMillis());
            batch.put("media_manifest", read(dir(job).resolve("media.json")));
            batch.put("association_revision",revision(associationJournal));batch.put("photo_associations",frozenAssociations);batch.put("association_decisions",decisions(associationJournal));batch.put("overall_route",bindingSnapshot);
            write(current, batch); write(history, batch);
            try {
                List<Path> paths = analyzed.stream().map(file -> original(job, file)).collect(Collectors.toList());
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
        if(gateway() instanceof ConstructionNativeAnalysisGateway){
            List<Map<String,Object>> all=batches(job);
            if(all.isEmpty())return read(dir(job).resolve("analysis.json"));
            return batch(job,(String)all.get(0).get("batch_id"));
        }
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

    private ConstructionNativeAnalysisGateway nativeGateway(){
        if(!(gateway() instanceof ConstructionNativeAnalysisGateway))throw error(HttpStatus.SERVICE_UNAVAILABLE,"原生分析适配器未配置");
        return (ConstructionNativeAnalysisGateway)gateway();
    }
    private Map<String,Object> remember(WaylineJobEntity job,Map<String,Object> batch)throws Exception {
        batch.put("native_adapter",true);
        write(dir(job).resolve("analysis.json"),batch);return batch;
    }
    public List<Map<String,Object>> batches(WaylineJobEntity job)throws Exception{
        List<Map<String,Object>> result=new ArrayList<>(nativeGateway().batches(job));
        Set<Object> ids=result.stream().map(b->b.get("batch_id")).collect(Collectors.toSet());
        if(Files.isDirectory(dir(job)))try(DirectoryStream<Path> stream=Files.newDirectoryStream(dir(job),"batch-*.json")){
            for(Path p:stream){Map<String,Object> b=read(p);if(!b.isEmpty()&&ids.add(b.get("batch_id")))result.add(b);}
        }
        result.sort(Comparator.comparingLong((Map<String,Object> b)->b.get("created_at") instanceof Number?((Number)b.get("created_at")).longValue():0).reversed());return result;
    }
    public Map<String,Object> batch(WaylineJobEntity job,String id)throws Exception {
        for(Map<String,Object> b:batches(job))if(id.equals(b.get("batch_id"))&&!b.containsKey("report_ready"))return b;
        Map<String,Object> value=nativeGateway().batch(job,id);
        Map<String,Object> current=read(dir(job).resolve("analysis.json"));
        if(Objects.equals(current.get("batch_id"),id))remember(job,value);
        return value;
    }
    public String currentBatch(WaylineJobEntity job)throws Exception {
        Map<String,Object> b=read(dir(job).resolve("analysis.json"));
        if(!(b.get("batch_id") instanceof String))throw error(HttpStatus.NOT_FOUND,"暂无分析批次");return (String)b.get("batch_id");
    }
    public Map<String,Object> generateReport(WaylineJobEntity job,String id)throws Exception {
        Map<String,Object> b=nativeGateway().generate(job,id);
        return locked(job,()->{
            Map<String,Object> current=read(dir(job).resolve("analysis.json"));
            if(id.equals(current.get("batch_id")))remember(job,b);return b;
        });
    }
    public Path report(WaylineJobEntity job,String id)throws Exception{return nativeGateway().report(job,id);}
    public Path resultPhoto(WaylineJobEntity job,String id,String file)throws Exception{return nativeGateway().evidence(job,id,file);}

    Map<String,Object> analysisSnapshot(WaylineJobEntity job)throws Exception {
        Map<String,Object> frozen=new LinkedHashMap<>();
        frozen.put("overall_route",snapshot(job));frozen.put("media_manifest",read(dir(job).resolve("media.json")));
        Map<String,Object> journal=read(dir(job).resolve("associations.json"));
        frozen.put("association_revision",revision(journal));frozen.put("association_decisions",decisions(journal));
        return frozen;
    }
    Path analysisOriginal(WaylineJobEntity job,String fileId)throws Exception {
        Path path=photo(job,fileId);
        if(!Objects.equals(read(dir(job).resolve("media.json")).get(fileId),checksum(path)))
            throw error(HttpStatus.CONFLICT,"原图已改变或损坏，请核对归档，不能使用旧元数据提交");
        return path;
    }
    Map<String,Path> analysisOriginals(WaylineJobEntity job)throws Exception {
        Map<String,Object> manifest=read(dir(job).resolve("media.json"));Map<String,Path> out=new LinkedHashMap<>();
        for(MediaFileEntity file:media(job)){
            Path path=photo(job,file.getFileId());
            if(!Objects.equals(manifest.get(file.getFileId()),checksum(path)))throw error(HttpStatus.CONFLICT,"原图已改变或损坏，请核对归档，不能使用旧元数据提交");
            out.put(file.getFileId(),path);
        }return out;
    }
    @SuppressWarnings("unchecked")
    Map<String,Map<String,Object>> analysisMetadata(WaylineJobEntity job)throws Exception {
        Map<String,Object> snap=snapshot(job), config=(Map<String,Object>)snap.getOrDefault("analysis_config",Map.of());
        if(!(config.get("schema_version") instanceof Number)||((Number)config.get("schema_version")).doubleValue()!=1)
            throw error(HttpStatus.CONFLICT,"任务缺少受支持的冻结检测配置版本，不能读取当前规则代替历史快照");
        List<Map<String,Object>> points=(List<Map<String,Object>>)config.getOrDefault("points",List.of());
        Map<String,Object> journal=read(dir(job).resolve("associations.json"));Map<String,Map<String,Object>> out=new LinkedHashMap<>();
        for(MediaFileEntity file:media(job)){
            Map<String,Object> association=ConstructionPhotoAssociation.resolve(snap,file.getFileId(),file.getFileName(),decisions(journal));
            Map<String,Object> p=new LinkedHashMap<>();p.put("id",file.getFileId());p.put("fileName",file.getFileName());p.put("areaId",association.get("area_id"));p.put("excluded",Boolean.TRUE.equals(association.get("excluded")));
            p.put("ready",false);p.put("checks",List.of());p.put("type","unknown");p.put("metadataSource","frozen-task-snapshot");
            if(Boolean.TRUE.equals(p.get("excluded"))){out.put(file.getFileId(),p);continue;}
            List<Map<String,Object>> matches=new ArrayList<>();
            for(Map<String,Object> point:points)for(String suffix:(List<String>)point.getOrDefault("photo_suffixes",List.of()))
                if(file.getFileName()!=null&&java.util.regex.Pattern.compile("(?:^|_)"+java.util.regex.Pattern.quote(suffix)+"(?=_|\\.|$)").matcher(file.getFileName()).find()){matches.add(point);break;}
            if(matches.size()==1&&Objects.equals(matches.get(0).get("area_id"),association.get("area_id"))){
                Map<String,Object> point=matches.get(0);Map<String,Object> policy=ConstructionAnalysisPolicy.validate(point.get("effective"));
                String name=file.getFileName();boolean thermal=java.util.regex.Pattern.compile("(?i)(?:^|_)T(?=_|\\.|$)").matcher(name).find(),visible=java.util.regex.Pattern.compile("(?i)(?:^|_)V(?=_|\\.|$)").matcher(name).find();
                if(thermal!=visible){
                    Map<String,Object> ir=(Map<String,Object>)policy.get("infrared");p.put("type",thermal?"infrared":"visible");p.put("checks",thermal?List.of():policy.get("visible_checks"));p.put("ready",true);
                    p.put("waypoint_index",point.get("index"));p.put("policy_source",point.get("source"));p.put("infrared_temperature_enabled",thermal&&Boolean.TRUE.equals(ir.get("enabled")));
                    if(thermal&&Boolean.TRUE.equals(ir.get("enabled")))p.put("infrared_policy",new LinkedHashMap<>(ir));
                    p.put("infrared_temperature",Map.of("status",thermal&&Boolean.TRUE.equals(ir.get("enabled"))?"queued":"not_requested","message",thermal&&Boolean.TRUE.equals(ir.get("enabled"))?"等待平台本地测温":"本照片未请求红外测温"));
                }
            }
            if(!Boolean.TRUE.equals(p.get("excluded"))&&!Boolean.TRUE.equals(p.get("ready")))throw error(HttpStatus.CONFLICT,"任务缺少有效冻结检测配置、唯一拍摄航点或明确_T/_V镜头标记："+file.getFileName());
            p.put("originalUrl","/api/construction/tasks/"+job.getJobId()+"/media/"+file.getFileId());out.put(file.getFileId(),p);
        }return out;
    }

    /** Export under the same lock as archive/analysis so ownership cannot change mid-package. */
    public Path exportTransfer(WaylineJobEntity job, ConstructionTransferService transfer) throws Exception {
        return locked(job, () -> {
            List<MediaFileEntity> entries = media(job);
            if (!complete(job, entries)) throw error(HttpStatus.CONFLICT,"仅支持飞行完成且照片收齐的任务导出");
            archiveFiles(job, entries);
            Map<String,Object> document = new LinkedHashMap<>();
            Map<String,Object> old = imported(job.getWorkspaceId(),job.getJobId());
            document.put("version",1);
            document.put("origin", old.isEmpty() ? Map.of("deployment",transfer.deployment(),"workspace",job.getWorkspaceId(),"job_id",job.getJobId()) : old.get("origin"));
            document.put("task", old.isEmpty() ? json.convertValue(job,Map.class) : old.get("task"));
            Map<String,Object> route = snapshot(job);
            if (route.isEmpty()) throw error(HttpStatus.CONFLICT,"任务缺少工作面执行快照，不能导出不完整的巡检结果");
            document.put("snapshot",route);
            document.put("associations",read(dir(job).resolve("associations.json")));
            List<Map<String,Object>> photos = new ArrayList<>(); Map<String,Path> originals = new LinkedHashMap<>();
            for (MediaFileEntity file : entries) {
                Path path = original(job,file); String entry = "originals/" + path.getFileName();
                Map<String,Object> photo=new LinkedHashMap<>();
                photo.put("id",file.getFileId());photo.put("name",file.getFileName());photo.put("entry",entry);
                photo.put("sha256",checksum(path));photo.put("size",Files.size(path));
                photo.put("sub_file_type",file.getSubFileType());photo.put("drone",file.getDrone());photo.put("payload",file.getPayload());
                photos.add(photo);
                originals.put(entry,path);
            }
            document.put("photos",photos);
            return transfer.pack(job.getWorkspaceId(),document,originals);
        });
    }
}
