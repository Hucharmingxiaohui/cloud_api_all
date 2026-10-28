package com.dji.sample.df.constructionDf;

import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;

/** Result-only transfer: no flight jobs, credentials, executable plans or device bindings are imported. */
@Service
@SuppressWarnings("unchecked")
public class ConstructionTransferService {
    private final ConstructionTaskService tasks;
    private final Path root;
    private final long maxBytes;
    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    public ConstructionTransferService(ConstructionTaskService tasks,
            @Value("${construction.transfer-root:./data/construction-transfer}") String root,
            @Value("${construction.transfer-max-bytes:10737418240}") long maxBytes) {
        this.tasks=tasks; this.root=Paths.get(root).toAbsolutePath().normalize(); this.maxBytes=maxBytes;
    }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
    private static String key(String value) { return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString(); }
    private static String hash(byte[] bytes) throws Exception { return hex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static String hex(byte[] bytes) { StringBuilder s=new StringBuilder(); for(byte b:bytes)s.append(String.format("%02x",b&255));return s.toString(); }
    private Path workspace(String w) throws IOException {
        Path dir=root.resolve(key(w));
        for(String sub:List.of("inbox","imported","failed","exports"))Files.createDirectories(dir.resolve(sub));
        return dir;
    }
    public synchronized String deployment() throws IOException {
        Files.createDirectories(root); Path file=root.resolve("deployment.id");
        if(!Files.exists(file))try { Files.writeString(file,UUID.randomUUID().toString(),StandardOpenOption.CREATE_NEW); } catch(FileAlreadyExistsException ignored) { }
        String id=Files.readString(file).trim(); UUID.fromString(id); return id;
    }
    private Map<String,Object> read(Path path) throws IOException { return json.readValue(path.toFile(),new TypeReference<Map<String,Object>>(){}); }
    private void write(Path path,Object value) throws IOException { json.writeValue(path.toFile(),value); }
    private static String string(Map<String,Object> map,String key) {
        Object v=map.get(key);if(!(v instanceof String)||((String)v).isBlank()||((String)v).length()>500)throw bad("数据包字段无效："+key);return (String)v;
    }
    private static Map<String,Object> map(Object v) { if(!(v instanceof Map))throw bad("数据包对象缺失");return (Map<String,Object>)v; }
    private static List<Map<String,Object>> list(Object v) {
        if(!(v instanceof List)||((List<?>)v).size()>20000)throw bad("数据包列表无效或超过20000项");
        for(Object item:(List<?>)v)map(item);return (List<Map<String,Object>>)v;
    }
    private static long number(Object v) { if(!(v instanceof Number)||((Number)v).doubleValue()!=((Number)v).longValue())throw bad("数据包数值无效");return ((Number)v).longValue(); }
    private String identity(Map<String,Object> doc) {
        Map<String,Object> origin=map(doc.get("origin"));
        return string(origin,"deployment")+"\n"+string(origin,"workspace")+"\n"+string(origin,"job_id");
    }
    private void validate(Map<String,Object> doc) {
        if(number(doc.get("version"))!=1)throw bad("不支持的数据包版本"); identity(doc);
        Map<String,Object> task=map(doc.get("task")); string(task,"name");
        try { json.convertValue(task,WaylineJobEntity.class); }
        catch(IllegalArgumentException e) {throw bad("任务字段格式无效");}
        Map<String,Object> origin=map(doc.get("origin"));
        if(!Objects.equals(task.get("jobId"),origin.get("job_id"))||!Objects.equals(task.get("workspaceId"),origin.get("workspace")))throw bad("任务与来源标识不一致");
        List<Map<String,Object>> photos=list(doc.get("photos"));
        if(photos.isEmpty()||number(task.get("status"))!=3||number(task.get("mediaCount"))!=photos.size())throw bad("任务未完成或照片数量不完整");
        Map<String,Object> snapshot=map(doc.get("snapshot"));
        Set<String> areas=new HashSet<>();
        for(Map<String,Object> binding:list(snapshot.get("bindings"))) {
            if(!areas.add(string(binding,"area_id")))throw bad("工作面编号重复");string(binding,"area_name");
            if(!(binding.get("waypoints") instanceof List))throw bad("工作面航点列表无效");
            for(Object n:(List<?>)binding.get("waypoints"))if(number(n)<1)throw bad("航点编号无效");
        }
        if(areas.isEmpty())throw bad("缺少工作面快照");
        for(Map<String,Object> point:list(snapshot.get("points"))) {
            if(number(point.get("index"))<1||!(point.get("photo_suffixes") instanceof List))throw bad("拍照航点快照无效");
            for(Object suffix:(List<?>)point.get("photo_suffixes"))if(!(suffix instanceof String))throw bad("拍照后缀无效");
        }
        Set<String> ids=new HashSet<>(),paths=new HashSet<>();long total=0;
        for(Map<String,Object> photo:photos) {
            String id=string(photo,"id"),name=string(photo,"name"),entry=string(photo,"entry");
            String ext=name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
            if(!Set.of("jpg","jpeg","png","tif","tiff","dng","bmp","webp","heic","heif").contains(ext)
                    ||!entry.equals("originals/"+key(id)+"."+ext)||!ids.add(id)||!paths.add(entry))throw bad("照片编号、格式或归档路径无效");
            if(!string(photo,"sha256").matches("[a-f0-9]{64}"))throw bad("照片完整性记录无效");
            long size=number(photo.get("size"));if(size<1||size>maxBytes||total>maxBytes-size)throw bad("数据包超过接收容量限制");total+=size;
        }
        Map<String,Object> journal=map(doc.get("associations"));
        if(!journal.isEmpty()) {
            if(number(journal.get("revision"))<0)throw bad("照片归属版本无效");
            for(Map.Entry<String,Object> e:map(journal.get("photos")).entrySet()) {
                if(!ids.contains(e.getKey()))throw bad("归属记录包含其他任务照片");
                ConstructionPhotoAssociation.decision(snapshot,map(e.getValue()),"transfer-validation");
            }
        }
    }
    Path pack(String w,Map<String,Object> document,Map<String,Path> originals) throws Exception {
        validate(document); Path folder=workspace(w).resolve("exports");
        String name="inspection-"+key(identity(document))+"-"+hash(json.writeValueAsBytes(document)).substring(0,12)+".zip";
        Path target=folder.resolve(name),temp=Files.createTempFile(folder,"export-",".part");
        try {
            try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(temp))) {
                zip.putNextEntry(new ZipEntry("manifest.json"));zip.write(json.writeValueAsBytes(document));zip.closeEntry();
                for(Map.Entry<String,Path> file:originals.entrySet()) {zip.putNextEntry(new ZipEntry(file.getKey()));Files.copy(file.getValue(),zip);zip.closeEntry();}
            }
            Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);return target;
        } finally {Files.deleteIfExists(temp);}
    }
    public Map<String,Object> status(String w) throws Exception {
        Path folder=workspace(w);List<Map<String,Object>> rows=new ArrayList<>();
        Path local=tasks.storageRoot().resolve(key(w));
        if(Files.isDirectory(local))try(var dirs=Files.list(local)) {
            for(Path dir:dirs.filter(p->p.getFileName().toString().matches("[a-f0-9-]{36}")&&Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS)).collect(Collectors.toList())) {
                Path marker=dir.resolve("transfer.json");if(!Files.isRegularFile(marker,LinkOption.NOFOLLOW_LINKS))continue;
                Map<String,Object> doc=read(marker),task=map(doc.get("task"));
                Map<String,Object> row=new LinkedHashMap<>();row.put("job_id",doc.get("local_job_id"));row.put("name",task.get("name"));
                row.put("dock_sn",task.get("dockSn"));row.put("execute_time",task.get("executeTime"));row.put("end_time",task.get("endTime"));
                row.put("photo_count",list(doc.get("photos")).size());row.put("origin",doc.get("origin"));row.put("imported_at",doc.get("imported_at"));rows.add(row);
            }
        }
        rows.sort(Comparator.comparingLong((Map<String,Object> r)->((Number)r.get("imported_at")).longValue()).reversed());
        return Map.of("deployment",deployment(),"inbox",folder.resolve("inbox").toString(),"exports",folder.resolve("exports").toString(),"tasks",rows);
    }
    public Map<String,Object> scan(String w) throws Exception {
        Path folder=workspace(w);List<Map<String,Object>> receipts=new ArrayList<>();
        try(FileChannel channel=FileChannel.open(folder.resolve("scan.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE)) {
            try(FileLock lock=channel.tryLock()) {
                if(lock==null)throw bad("正在接收离线数据，请稍后刷新");
                List<Path> files;try(var stream=Files.list(folder.resolve("inbox"))) {files=stream.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)&&p.getFileName().toString().endsWith(".zip")).sorted().limit(100).collect(Collectors.toList());}
                for(Path file:files) {
                    Map<String,Object> receipt=new LinkedHashMap<>();receipt.put("file",file.getFileName().toString());
                    try { receipt.putAll(receive(w,file)); }
                    catch(Exception e) {receipt.put("status","FAILED");receipt.put("message",e instanceof ResponseStatusException?((ResponseStatusException)e).getReason():"数据包无效或本地读写失败："+e.getMessage());}
                    boolean failed="FAILED".equals(receipt.get("status"));
                    Path destination=folder.resolve(failed?"failed":"imported").resolve(UUID.randomUUID()+"-"+file.getFileName());
                    try { Files.move(file,destination,StandardCopyOption.ATOMIC_MOVE);write(destination.resolveSibling(destination.getFileName()+".receipt.json"),receipt); }
                    catch(IOException e) {receipt.put("message",String.valueOf(receipt.getOrDefault("message",""))+"；接收包未能移入归档目录，重试不会重复创建任务");}
                    receipts.add(receipt);
                }
            } catch(OverlappingFileLockException e) {throw bad("正在接收离线数据，请稍后刷新");}
        }
        Map<String,Object> result=new LinkedHashMap<>(status(w));result.put("receipts",receipts);return result;
    }
    Map<String,Object> receive(String w,Path file) throws Exception {
        if(Files.size(file)>maxBytes+16777216L)throw bad("数据包超过接收容量限制");
        try(ZipFile zip=new ZipFile(file.toFile())) {
            if(zip.size()>20001)throw bad("数据包文件过多");
            ZipEntry manifest=zip.getEntry("manifest.json");if(manifest==null)throw bad("数据包缺少manifest.json");
            byte[] data;try(InputStream in=zip.getInputStream(manifest)){data=in.readNBytes(16777217);}
            if(data.length>16777216)throw bad("任务描述文件过大");
            Map<String,Object> doc=json.readValue(data,new TypeReference<Map<String,Object>>(){});validate(doc);
            String digest=hash(json.writeValueAsBytes(doc));String id="offline-"+key(identity(doc));
            if(deployment().equals(map(doc.get("origin")).get("deployment")))return Map.of("status","SKIPPED","message","本机来源任务，无需重复导入");
            Map<String,Object> existing=tasks.imported(w,id);
            if(!existing.isEmpty()) {
                if(!digest.equals(existing.get("import_digest")))throw bad("同一来源任务已有不同版本，保留本机数据，请人工核对");
                return Map.of("status","SKIPPED","job_id",id,"message","该任务已导入，本机分析与归属记录保持不变");
            }
            Set<String> expected=new HashSet<>();expected.add("manifest.json");for(Map<String,Object> p:list(doc.get("photos")))expected.add(string(p,"entry"));
            Set<String> seen=new HashSet<>();Enumeration<? extends ZipEntry> all=zip.entries();
            while(all.hasMoreElements()){ZipEntry entry=all.nextElement();if(entry.isDirectory()||!expected.contains(entry.getName())||!seen.add(entry.getName()))throw bad("数据包包含重复或未声明文件");}
            if(!seen.equals(expected))throw bad("数据包缺少原图");
            Path parent=tasks.storageRoot().resolve(key(w));Files.createDirectories(parent);Path stage=Files.createTempDirectory(parent,"receiving-");
            try {
                Map<String,Object> media=new LinkedHashMap<>();
                for(Map<String,Object> photo:list(doc.get("photos"))) {
                    String entry=string(photo,"entry");Path target=stage.resolve(entry.substring("originals/".length()));
                    MessageDigest sha=MessageDigest.getInstance("SHA-256");long size=0,limit=number(photo.get("size"));
                    try(InputStream in=zip.getInputStream(zip.getEntry(entry));OutputStream out=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW)) {
                        byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){size+=n;if(size>limit)throw bad("原图大小与清单不一致");sha.update(buffer,0,n);out.write(buffer,0,n);}
                    }
                    String checksum=hex(sha.digest());if(size!=limit||!checksum.equals(photo.get("sha256")))throw bad("原图损坏或拷贝不完整");media.put(string(photo,"id"),checksum);
                }
                write(stage.resolve("media.json"),media);write(stage.resolve("associations.json"),doc.get("associations"));
                doc.put("local_job_id",id);doc.put("import_digest",digest);doc.put("imported_at",System.currentTimeMillis());
                write(stage.resolve("transfer.json"),doc);
                Path destination=parent.resolve(key(id));if(Files.exists(destination))throw bad("离线任务目标目录已存在，请检查本地记录");
                Files.move(stage,destination,StandardCopyOption.ATOMIC_MOVE);
                return Map.of("status","IMPORTED","job_id",id,"message","已接收，可查看结果并手动提交分析");
            } finally {
                if(Files.exists(stage))try(var paths=Files.walk(stage)){for(Path p:paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList()))Files.deleteIfExists(p);}
            }
        }
    }
}
