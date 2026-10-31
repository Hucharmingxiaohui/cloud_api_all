package com.dji.sample.df.constructionAnalysisDf;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import static com.dji.sample.df.constructionAnalysisDf.AnalysisStore.error;

public class AnalysisService implements AutoCloseable {
    public static final List<String> CHECKS=List.of("missing_helmet","phone_use","smoking","person_fallen","missing_safety_clothing","person_count","fire_smoke","adverse_weather","animal_intrusion","barrier_crossing","person_under_lifting_load","missing_hook_latch");
    private static final Set<String> TERMINAL=Set.of("SUCCEEDED","FAILED");
    private static final Set<String> CHECK_STATUS=Set.of("success","unavailable","not_applicable","unassessable","failed");
    final AnalysisStore store;
    private final AnalysisMetadata metadata;
    private final AnalysisClient client;
    final int maxPhotos;
    final long maxFile,maxTotal;
    private final Set<String> pending=ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String,ReentrantLock> taskLocks=new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8),r->{Thread t=new Thread(r,"construction-analysis");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    public AnalysisService(AnalysisStore store,AnalysisMetadata metadata,AnalysisClient client,int maxPhotos,long maxFile,long maxTotal) {
        if(maxPhotos<1||maxFile<1||maxTotal<maxFile||maxTotal>256L*1024*1024)throw new IllegalArgumentException("Invalid analysis limits");
        this.store=store;this.metadata=metadata;this.client=client;this.maxPhotos=maxPhotos;this.maxFile=maxFile;this.maxTotal=maxTotal;
    }
    private void authorize(String w,String t)throws IOException {AnalysisStore.id(w);AnalysisStore.id(t);metadata.resolve(w,t);store.scan(t);store.claim(w,t);}
    private ReentrantLock taskLock(String w,String t){return taskLocks.computeIfAbsent(AnalysisStore.id(w)+":"+AnalysisStore.id(t),k->new ReentrantLock());}
    @FunctionalInterface private interface IoOperation<T>{T run()throws IOException;}
    private <T>T guarded(String w,String t,IoOperation<T> operation)throws IOException {
        ReentrantLock lock=taskLock(w,t);
        if(!lock.tryLock())throw error(HttpStatus.CONFLICT,"Task operation in progress; retry shortly");
        try{return operation.run();}finally{lock.unlock();}
    }
    public Map<String,Object> task(String w,String t)throws IOException {return guarded(w,t,()->taskLocked(w,t));}
    public Map<String,Object> savePhotos(String w,String t,List<Map<String,Object>> requested)throws IOException {return guarded(w,t,()->savePhotosLocked(w,t,requested));}
    public List<Map<String,Object>> batches(String w,String t)throws IOException {return guarded(w,t,()->{authorize(w,t);return batchesInternal(w,t);});}
    public Map<String,Object> submit(String w,String t,String requestId)throws IOException {return guarded(w,t,()->submitLocked(w,t,requestId));}
    public Map<String,Object> refresh(String w,String t,String batch)throws IOException {return guarded(w,t,()->refreshLocked(w,t,batch));}
    public Path original(String w,String t,String image)throws IOException {return guarded(w,t,()->originalLocked(w,t,image));}
    public Path result(String w,String t,String batch,String image)throws IOException {return guarded(w,t,()->resultLocked(w,t,batch,image));}
    public Path report(String w,String t,String batch)throws IOException {return guarded(w,t,()->reportLocked(w,t,batch));}
    public Map<String,Object> generateReport(String w,String t,String batch)throws Exception {
        ReentrantLock lock=taskLock(w,t);
        if(!lock.tryLock())throw error(HttpStatus.CONFLICT,"Task operation in progress; retry shortly");
        try{return generateReportLocked(w,t,batch);}finally{lock.unlock();}
    }
    private Path journal(String w,String t)throws IOException{return store.file(store.task(w,t),"metadata.json");}
    @SuppressWarnings("unchecked") static List<Map<String,Object>> photos(Map<String,Object> b){return (List<Map<String,Object>>)b.getOrDefault("photos",List.of());}
    private Map<String,Object> taskLocked(String w,String t)throws IOException {
        AnalysisStore.id(w);Map<String,Map<String,Object>> nativePhotos=metadata.resolve(w,AnalysisStore.id(t));
        Map<String,Path> scanned=store.scan(t);store.claim(w,t);Map<String,Object> saved=store.read(journal(w,t));
        Map<String,Map<String,Object>> manual=new HashMap<>();for(Map<String,Object> p:photos(saved))manual.put((String)p.get("id"),p);
        List<Map<String,Object>> out=new ArrayList<>();
        for(Map.Entry<String,Path> e:scanned.entrySet()) {
            Map<String,Object> p=new LinkedHashMap<>();p.put("id",e.getKey());p.put("fileName",e.getValue().getFileName().toString());
            p.put("areaId",null);p.put("type","unknown");p.put("checks",CHECKS);p.put("excluded",false);p.put("metadataSource","unknown");p.put("ready",false);
            if(nativePhotos.containsKey(e.getKey()))p.putAll(nativePhotos.get(e.getKey()));
            Map<String,Object> m=manual.get(e.getKey());
            if(m!=null&&Objects.equals(m.get("fileName"),p.get("fileName")))p.putAll(m);
            p.put("originalUrl",base(t)+"/photos/"+e.getKey()+"/original");out.add(p);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("taskId",t);result.put("photos",out);result.put("batches",batchesInternal(w,t));return result;
    }
    private Map<String,Object> savePhotosLocked(String w,String t,List<Map<String,Object>> requested)throws IOException {
        Map<String,Object> current=task(w,t);Map<String,Map<String,Object>> actual=photos(current).stream().collect(Collectors.toMap(p->(String)p.get("id"),p->p));
        if(requested==null||requested.size()!=actual.size()||requested.size()>maxPhotos)throw error(HttpStatus.BAD_REQUEST,"Provide metadata for every scanned photo exactly once");
        List<Map<String,Object>> saved=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Map<String,Object> raw:requested) {
            if(raw==null||!(raw.get("id") instanceof String))throw error(HttpStatus.BAD_REQUEST,"Invalid photo metadata");
            String id=AnalysisStore.id((String)raw.get("id"));
            if(!seen.add(id)||!actual.containsKey(id))throw error(HttpStatus.BAD_REQUEST,"Unknown or duplicate photo identifier");
            if(!raw.containsKey("areaId")||!(raw.get("excluded") instanceof Boolean)||!(raw.get("type") instanceof String)
                    ||!Set.of("visible","infrared","unknown").contains(raw.get("type")))throw error(HttpStatus.BAD_REQUEST,"Explicit areaId, type, checks and excluded are required");
            if(raw.get("areaId")!=null){if(!(raw.get("areaId") instanceof String))throw error(HttpStatus.BAD_REQUEST,"Invalid area identifier");AnalysisStore.id((String)raw.get("areaId"));}
            validateChecks(raw.get("checks"));
            Map<String,Object> p=new LinkedHashMap<>();for(String key:List.of("id","areaId","type","checks","excluded"))p.put(key,raw.get(key));
            p.put("fileName",actual.get(id).get("fileName"));p.put("metadataSource","manual");p.put("ready",true);saved.add(p);
        }
        store.write(journal(w,t),Map.of("photos",saved,"updatedAt",Instant.now().toString()));return task(w,t);
    }
    static void validateChecks(Object raw) {
        if(!(raw instanceof List)||((List<?>)raw).size()>12)throw error(HttpStatus.BAD_REQUEST,"Invalid checks list");
        Set<Object> seen=new HashSet<>();for(Object c:(List<?>)raw)if(!(c instanceof String)||!CHECKS.contains(c)||!seen.add(c))throw error(HttpStatus.BAD_REQUEST,"Unknown or duplicate check code");
    }
    private String key(String w,String t,String b){return w+":"+t+":"+b;}
    private void persist(String w,String t,Map<String,Object> b)throws IOException {store.write(store.file(store.batch(w,t,(String)b.get("batchId")),"batch.json"),b);}
    private List<Map<String,Object>> batchesInternal(String w,String t)throws IOException {
        List<Map<String,Object>> all=store.batches(w,t);
        for(Map<String,Object> b:all)if("SUBMITTING".equals(b.get("status"))&&!pending.contains(key(w,t,(String)b.get("batchId")))) {
            b.put("status","UNKNOWN");b.put("message","Submission interrupted; remote acceptance unknown. Do not resubmit.");persist(w,t,b);
        }
        return all;
    }
    private Map<String,Object> submitLocked(String w,String t,String requestId)throws IOException {
        authorize(w,t);AnalysisStore.id(requestId);
        for(Map<String,Object> b:batchesInternal(w,t))if(requestId.equals(b.get("requestId")))return b;
        for(Map<String,Object> b:batchesInternal(w,t))if(!TERMINAL.contains(b.get("status")))throw error(HttpStatus.CONFLICT,"An active or uncertain batch already exists");
        if(!client.configured())throw error(HttpStatus.SERVICE_UNAVAILABLE,"Algorithm service is not configured");
        Map<String,Object> task=task(w,t);List<Map<String,Object>> selected=new ArrayList<>();long total=0;
        Map<String,Path> files=store.scan(t);
        for(Map<String,Object> p:photos(task)) {
            if(Boolean.TRUE.equals(p.get("excluded")))continue;
            if(!Boolean.TRUE.equals(p.get("ready")))throw error(HttpStatus.CONFLICT,"Confirm metadata for every included photo");
            if(((List<?>)p.get("checks")).isEmpty())continue;
            long size=Files.size(files.get(p.get("id")));total+=size;
            if(size==0||size>maxFile||total>maxTotal||selected.size()>=maxPhotos)throw error(HttpStatus.PAYLOAD_TOO_LARGE,"Complete task exceeds configured image limits; it cannot be split");
            Map<String,Object> frozen=new LinkedHashMap<>(p);frozen.put("sha256",digest(store.bytes(files.get(p.get("id")),maxFile)));frozen.put("status","QUEUED");frozen.put("message",null);frozen.put("result",null);frozen.put("imageUrl",null);selected.add(frozen);
        }
        if(selected.isEmpty())throw error(HttpStatus.CONFLICT,"No photos selected for analysis");
        if(executor.getQueue().remainingCapacity()==0)throw error(HttpStatus.SERVICE_UNAVAILABLE,"Analysis submission queue is full");
        Map<String,Object> b=new LinkedHashMap<>();String batch=UUID.randomUUID().toString();
        b.put("batchId",batch);b.put("requestId",requestId);b.put("taskId",t);b.put("remoteId",null);b.put("status","SUBMITTING");b.put("finished",0);b.put("total",selected.size());b.put("message",null);b.put("createdAt",Instant.now().toString());b.put("areas",List.of());b.put("photos",selected);b.put("reportReady",false);
        persist(w,t,b);String key=key(w,t,batch);pending.add(key);
        try{executor.execute(()->send(w,t,batch));}catch(RejectedExecutionException e){pending.remove(key);b.put("status","FAILED");b.put("message","Submission queue unavailable; no request sent");persist(w,t,b);}
        return b;
    }
    private void send(String w,String t,String batch) {
        ReentrantLock lock=taskLock(w,t);lock.lock();
        try{sendLocked(w,t,batch);}finally{lock.unlock();}
    }
    private void sendLocked(String w,String t,String batch) {
        Map<String,Object> b=null;boolean sending=false;
        try {
            b=load(w,t,batch);List<Map<String,Object>> images=new ArrayList<>();Map<String,Path> files=store.scan(t);long total=0;
            for(Map<String,Object> p:photos(b)) {
                Path file=files.get(p.get("id"));if(file==null||!file.getFileName().toString().equals(p.get("fileName")))throw error(HttpStatus.CONFLICT,"Original file changed after batch freeze");
                byte[] bytes=store.bytes(file,maxFile);total+=bytes.length;if(total>maxTotal)throw error(HttpStatus.PAYLOAD_TOO_LARGE,"Task exceeds configured total limit");
                if(!digest(bytes).equals(p.get("sha256")))throw error(HttpStatus.CONFLICT,"Original bytes changed after batch freeze");
                Map<String,Object> image=new LinkedHashMap<>();image.put("id",p.get("id"));image.put("area_id",p.get("areaId"));image.put("type",p.get("type"));image.put("checks",p.get("checks"));image.put("data",Base64.getEncoder().encodeToString(bytes));images.add(image);
            }
            // The algorithm idempotency key includes workspace/task via the globally unique local batch ID.
            sending=true;Map<String,Object> response=client.submit(Map.of("request_id",batch,"images",images));
            Object remote=response.get("task_id");if(!(remote instanceof String))throw error(HttpStatus.BAD_GATEWAY,"Algorithm did not return task_id");
            b.put("remoteId",AnalysisStore.id((String)remote));b.put("status","QUEUED");
        }catch(Exception e) {
            if(b!=null){boolean rejected=e instanceof ResponseStatusException&&((ResponseStatusException)e).getStatus()==HttpStatus.PAYLOAD_TOO_LARGE;
                b.put("status",sending&&!rejected?"UNKNOWN":"FAILED");b.put("message",sending&&!rejected?"Remote acceptance is uncertain; do not resubmit":"Submission rejected before processing");}
        }finally {
            try{if(b!=null)persist(w,t,b);}catch(IOException e){/* Durable SUBMITTING journal recovers as UNKNOWN. */}
            pending.remove(key(w,t,batch));
        }
    }
    private Map<String,Object> load(String w,String t,String batch)throws IOException {
        Map<String,Object> b=store.read(store.file(store.batch(w,t,batch),"batch.json"));
        if(b.isEmpty())throw error(HttpStatus.NOT_FOUND,"Batch not found");
        if(!t.equals(b.get("taskId"))||!batch.equals(b.get("batchId")))throw error(HttpStatus.INTERNAL_SERVER_ERROR,"Invalid batch journal");return b;
    }
    private Map<String,Object> refreshLocked(String w,String t,String batch)throws IOException {
        authorize(w,t);batchesInternal(w,t);Map<String,Object> b=load(w,t,batch);
        if(TERMINAL.contains(b.get("status"))||b.get("remoteId")==null)return b;
        try {
            Map<String,Object> remote=client.poll((String)b.get("remoteId"));String status=String.valueOf(remote.get("status"));
            if(!Set.of("queued","running","completed","failed").contains(status))throw error(HttpStatus.BAD_GATEWAY,"Invalid algorithm status");
            int total=photos(b).size();if(!integer(remote.get("total"))||((Number)remote.get("total")).intValue()!=total||!integer(remote.get("finished"))||((Number)remote.get("finished")).intValue()>total)throw error(HttpStatus.BAD_GATEWAY,"Invalid algorithm progress");
            b.put("finished",remote.get("finished"));
            if(status.equals("completed")) {
                if(((Number)remote.get("finished")).intValue()!=total)throw error(HttpStatus.BAD_GATEWAY,"Completed batch has incomplete progress");
                b.put("areas",validateAreas(b,remote.get("areas")));boolean complete=true;
                for(Map<String,Object> p:photos(b)) {
                    if(Set.of("SUCCEEDED","PARTIAL").contains(p.get("status")))continue;
                    try{complete=fetch(w,t,b,p)&&complete;}catch(Exception e){p.put("status","RETRIEVAL_PENDING");p.put("message","Result retrieval or validation incomplete; retry refresh on the same remote task");complete=false;}
                    persist(w,t,b);
                }
                if(!consistentPersonAreas(b)){complete=false;b.put("validationMessage","Algorithm area success contradicts incomplete/failed person-count input; area result is not trustworthy");}
                boolean partial=photos(b).stream().anyMatch(p->"PARTIAL".equals(p.get("status")))||((List<Map<String,Object>>)b.get("areas")).stream().anyMatch(a->!Set.of("success","not_requested").contains(a.get("status")));
                b.put("status",complete?"SUCCEEDED":"RUNNING");b.put("message",complete?(partial?"All protocol results retrieved; partial checks or evidence unavailable. NOT an all-clear":"All protocol results retrieved; inspect detections and area status. Not an all-clear") :"Result retrieval or protocol validation pending; saved evidence retained. Retry refresh on the same remote task");
            }else {b.put("status",status.equals("failed")?"FAILED":status.toUpperCase(Locale.ROOT));b.put("message",status.equals("failed")?"Algorithm task failed":null);}
        }catch(Exception e){b.put("message","Status refresh failed; retained remote task and saved evidence; retry refresh");}
        persist(w,t,b);return b;
    }
    private List<Map<String,Object>> validateAreas(Map<String,Object> batch,Object raw) {
        if(!(raw instanceof List))throw error(HttpStatus.BAD_GATEWAY,"Missing area results");
        Set<Object> expected=photos(batch).stream().map(p->p.get("areaId")).filter(Objects::nonNull).collect(Collectors.toSet());Set<Object> seen=new HashSet<>();List<Map<String,Object>> result=new ArrayList<>();
        for(Object item:(List<?>)raw) {
            if(!(item instanceof Map))throw error(HttpStatus.BAD_GATEWAY,"Invalid area result");
            Map<String,Object> area=new LinkedHashMap<>((Map<String,Object>)item);Object id=area.get("area_id"),status=area.get("status");
            if(!expected.contains(id)||!seen.add(id)||(!CHECK_STATUS.contains(status)&&!"not_requested".equals(status)))throw error(HttpStatus.BAD_GATEWAY,"Invalid area identity/status");
            boolean requested=photos(batch).stream().anyMatch(p->Objects.equals(id,p.get("areaId"))&&((List<?>)p.get("checks")).contains("person_count"));
            if(!requested&&!"not_requested".equals(status)||requested&&"not_requested".equals(status))throw error(HttpStatus.BAD_GATEWAY,"Incorrect area person-count status");
            validateCount(area);
            if(!Set.of("success","not_requested").contains(status)&&(!(area.get("message") instanceof String)||((String)area.get("message")).isBlank()))throw error(HttpStatus.BAD_GATEWAY,"Non-success area requires explanation");
            result.add(area);
        }
        if(!seen.equals(expected))throw error(HttpStatus.BAD_GATEWAY,"Missing area result");return result;
    }
    @SuppressWarnings("unchecked") private boolean consistentPersonAreas(Map<String,Object> b) {
        for(Map<String,Object> area:(List<Map<String,Object>>)b.get("areas")) {
            if(!"success".equals(area.get("status")))continue;
            for(Map<String,Object> p:photos(b)) {
                if(!Objects.equals(area.get("area_id"),p.get("areaId"))||!((List<?>)p.get("checks")).contains("person_count"))continue;
                Object raw=p.get("result");if(!(raw instanceof Map)||!(((Map<?,?>)raw).get("checks") instanceof List))return false;
                long count=((List<?>)((Map<?,?>)raw).get("checks")).stream().filter(c->c instanceof Map&&"person_count".equals(((Map<?,?>)c).get("code"))&&"success".equals(((Map<?,?>)c).get("status"))).count();
                if(count!=1)return false;
            }
        }
        return true;
    }
    private static boolean integer(Object v){return v instanceof Number&&Double.isFinite(((Number)v).doubleValue())&&((Number)v).doubleValue()>=0&&((Number)v).doubleValue()<=Integer.MAX_VALUE&&((Number)v).doubleValue()==((Number)v).intValue();}
    private static void validateCount(Map<String,Object> value) {
        if("success".equals(value.get("status"))?!integer(value.get("count")):value.get("count")!=null)throw error(HttpStatus.BAD_GATEWAY,"Invalid person count");
    }
    @SuppressWarnings("unchecked") private boolean fetch(String w,String t,Map<String,Object> b,Map<String,Object> p)throws IOException {
        Map<String,Object> raw=client.image((String)b.get("remoteId"),(String)p.get("id"));Object image=raw.get("image");
        if(image instanceof String)raw.remove("image");
        p.put("result",raw);Path dir=store.batch(w,t,(String)b.get("batchId"));Path resultPath=store.file(dir,"result.json");
        Map<String,Object> results=store.read(resultPath);results.put((String)p.get("id"),raw);store.write(resultPath,results);
        boolean good=true;Set<Object> expected=new HashSet<>((List<?>)p.get("checks")),seen=new HashSet<>();
        if(!(raw.get("checks") instanceof List))throw error(HttpStatus.BAD_GATEWAY,"Missing check results");
        for(Object item:(List<?>)raw.get("checks")) {
            if(!(item instanceof Map))throw error(HttpStatus.BAD_GATEWAY,"Invalid check result");Map<String,Object> c=(Map<String,Object>)item;
            if(!expected.contains(c.get("code"))||!seen.add(c.get("code"))||!CHECK_STATUS.contains(c.get("status")))throw error(HttpStatus.BAD_GATEWAY,"Invalid check correspondence");
            if("person_count".equals(c.get("code")))validateCount(c);
            if(!"success".equals(c.get("status"))){good=false;if(!(c.get("message") instanceof String)||((String)c.get("message")).isBlank())throw error(HttpStatus.BAD_GATEWAY,"Non-success check requires explanation");}
            if("success".equals(c.get("status"))&&!Set.of("person_count","adverse_weather").contains(c.get("code"))&&!(c.get("boxes") instanceof List))throw error(HttpStatus.BAD_GATEWAY,"Missing detection boxes");
            if(c.containsKey("score")&&!score(c.get("score")))throw error(HttpStatus.BAD_GATEWAY,"Invalid image-level score");
            if(c.containsKey("boxes")) {
                if(!(c.get("boxes") instanceof List))throw error(HttpStatus.BAD_GATEWAY,"Invalid detection boxes");
                for(Object box:(List<?>)c.get("boxes")) {
                    if(!(box instanceof Map))throw error(HttpStatus.BAD_GATEWAY,"Invalid detection box");Map<?,?> value=(Map<?,?>)box;
                    if(!(value.get("rect") instanceof List)||((List<?>)value.get("rect")).size()!=4||!score(value.get("score")))throw error(HttpStatus.BAD_GATEWAY,"Invalid detection geometry/score");
                    List<?> rect=(List<?>)value.get("rect");for(Object n:rect)if(!(n instanceof Number)||!Double.isFinite(((Number)n).doubleValue())||((Number)n).doubleValue()<0)throw error(HttpStatus.BAD_GATEWAY,"Invalid detection coordinates");
                    if(((Number)rect.get(2)).doubleValue()<=0||((Number)rect.get(3)).doubleValue()<=0)throw error(HttpStatus.BAD_GATEWAY,"Invalid detection box dimensions");
                }
            }
        }
        if(!expected.equals(seen))throw error(HttpStatus.BAD_GATEWAY,"Missing requested checks");
        if(image==null&&raw.containsKey("image")&&raw.get("image_error") instanceof String&&!((String)raw.get("image_error")).isBlank()) {
            p.put("status","PARTIAL");p.put("message","Result image unavailable: algorithm supplied image_error; structured checks retained");return true;
        }
        if(!(image instanceof String)||((String)image).length()>4*((maxFile+2)/3))throw error(HttpStatus.BAD_GATEWAY,"Result image missing without image_error or exceeds limit");
        byte[] jpg;try{jpg=Base64.getDecoder().decode((String)image);}catch(IllegalArgumentException e){throw error(HttpStatus.BAD_GATEWAY,"Invalid result Base64");}
        validateJpeg(jpg);Path images=store.file(dir,"images");store.writeBytes(store.file(images,p.get("id")+".jpg"),jpg);
        p.put("imageUrl",base(t)+"/batches/"+b.get("batchId")+"/photos/"+p.get("id")+"/result");p.put("status",good?"SUCCEEDED":"PARTIAL");p.put("message",good?null:"One or more requested checks did not succeed; not an all-clear");return true;
    }
    private static boolean score(Object value){return value instanceof Number&&Double.isFinite(((Number)value).doubleValue())&&((Number)value).doubleValue()>=0&&((Number)value).doubleValue()<=1;}
    void validateJpeg(byte[] bytes)throws IOException {
        if(bytes.length>maxFile||bytes.length<4||(bytes[0]&255)!=255||(bytes[1]&255)!=216)throw error(HttpStatus.BAD_GATEWAY,"Result is not JPEG");
        try(ImageInputStream in=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(in);if(!readers.hasNext())throw error(HttpStatus.BAD_GATEWAY,"Result JPEG is undecodable");
            ImageReader r=readers.next();try{r.setInput(in);long pixels=(long)r.getWidth(0)*r.getHeight(0);if(pixels>25_000_000||pixels<1||!r.getFormatName().equalsIgnoreCase("JPEG")||r.read(0)==null)throw error(HttpStatus.BAD_GATEWAY,"Result image dimensions/format invalid");}finally{r.dispose();}
        }
    }
    private Path originalLocked(String w,String t,String image)throws IOException {
        authorize(w,t);Path p=store.scan(t).get(AnalysisStore.id(image));if(p==null)throw error(HttpStatus.NOT_FOUND,"Photo not found");return p;
    }
    private Path resultLocked(String w,String t,String batch,String image)throws IOException {
        authorize(w,t);AnalysisStore.id(image);Map<String,Object> b=load(w,t,batch);
        if(photos(b).stream().noneMatch(p->image.equals(p.get("id"))&&p.get("imageUrl")!=null))throw error(HttpStatus.NOT_FOUND,"Result image not saved");
        Path p=store.file(store.file(store.batch(w,t,batch),"images"),image+".jpg");store.bytes(p,maxFile);return p;
    }
    public Map<String,Object> health(){if(!client.configured())return Map.of("configured",false,"connected",false,"message","Algorithm service is not configured");try{return Map.of("configured",true,"connected","ok".equals(client.health().get("status")));}catch(Exception e){return Map.of("configured",true,"connected",false,"message","Algorithm health check failed");}}
    private Map<String,Object> generateReportLocked(String w,String t,String batch)throws Exception {
        authorize(w,t);Map<String,Object> b=load(w,t,batch);if(!TERMINAL.contains(b.get("status")))throw error(HttpStatus.CONFLICT,"Batch is not terminal");
        byte[] bytes=AnalysisReport.render(this,w,t,b);store.writeBytes(store.file(store.batch(w,t,batch),"report.docx"),bytes);b.put("reportReady",true);persist(w,t,b);return b;
    }
    private Path reportLocked(String w,String t,String batch)throws IOException {
        authorize(w,t);Map<String,Object> b=load(w,t,batch);Path p=store.file(store.batch(w,t,batch),"report.docx");
        if(!Boolean.TRUE.equals(b.get("reportReady")))throw error(HttpStatus.NOT_FOUND,"Generate report first");store.bytes(p,maxTotal+16L*1024*1024);return p;
    }
    static String base(String t){return "/api/construction-analysis/tasks/"+t;}
    static String digest(byte[] bytes) {try{return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
    @PreDestroy public void close(){executor.shutdown();try{if(!executor.awaitTermination(5,TimeUnit.SECONDS))executor.shutdownNow();}catch(InterruptedException e){executor.shutdownNow();Thread.currentThread().interrupt();}}
}
