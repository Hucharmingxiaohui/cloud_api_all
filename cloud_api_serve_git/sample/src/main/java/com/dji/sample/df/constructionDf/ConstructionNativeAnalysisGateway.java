package com.dji.sample.df.constructionDf;

import com.dji.sample.df.constructionAnalysisDf.*;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Archive-backed adapter; no external-folder ownership or manual metadata journal. */
@Component
public class ConstructionNativeAnalysisGateway implements ConstructionAnalysisGateway, AutoCloseable {
    private final ObjectProvider<ConstructionTaskService> tasks;
    private final AnalysisClient client;
    private final AnalysisService engine;

    public ConstructionNativeAnalysisGateway(ObjectProvider<ConstructionTaskService> tasks,
            @Value("${construction-analysis.algorithm-url:}") String url,
            @Value("${construction-analysis.algorithm-token:}") String token,
            @Value("${construction-analysis.connect-timeout-ms:5000}") int connect,
            @Value("${construction-analysis.read-timeout-ms:30000}") int read,
            @Value("${construction-analysis.max-photos:100}") int photos,
            @Value("${construction-analysis.max-file-bytes:20971520}") long file,
            @Value("${construction-analysis.max-total-bytes:104857600}") long total) {
        this.tasks=tasks;client=new AnalysisClient(url,token,connect,read,4*((file+2)/3)+1024*1024);
        engine=new AnalysisService(new NativeStore(),(w,t)-> {
            try{return service().analysisMetadata(service().requireJob(w,t));}
            catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("任务冻结配置无法读取",e);}
        },client,photos,file,total);
    }
    private ConstructionTaskService service(){return tasks.getObject();}
    private final class NativeStore extends AnalysisStore {
        NativeStore(){super(Paths.get(".native-unused-input"),Paths.get(".native-unused-output"));}
        @Override public Path task(String w,String t)throws IOException {
            try{return service().dir(service().requireJob(w,t)).resolve("analysis-batches");}
            catch(RuntimeException e){throw e;}catch(Exception e){throw new IOException(e);}
        }
        @Override public void claim(String w,String t){service().requireJob(w,t);}
        @Override public void authorizeMetadata(AnalysisMetadata ignored,String w,String t){service().requireJob(w,t);}
        @Override public void authorizeInputs(String w,String t){service().requireJob(w,t);}
        @Override public Map<String,Object> manualMetadata(Path journal){return Map.of();}
        @Override public Map<String,Object> frozenBatchMetadata(String w,String t)throws IOException {
            try{return service().analysisSnapshot(service().requireJob(w,t));}
            catch(RuntimeException e){throw e;}catch(Exception e){throw new IOException(e);}
        }
        @Override public String base(String t){return "/api/construction/tasks/"+t+"/analysis";}
        @Override public String originalName(String w,String t,String id,Path path){
            try{return (String)service().analysisMetadata(service().requireJob(w,t)).get(id).get("fileName");}
            catch(Exception e){throw new IllegalStateException("照片元数据无法读取",e);}
        }
        @Override public Map<String,Path> scan(String w,String t)throws IOException {
            try{return service().analysisOriginals(service().requireJob(w,t));}
            catch(RuntimeException e){throw e;}catch(Exception e){throw new IOException(e);}
        }
        @Override public Path original(String w,String t,String id)throws IOException {
            try{return service().analysisOriginal(service().requireJob(w,t),id);}
            catch(RuntimeException e){throw e;}catch(Exception e){throw new IOException(e);}
        }
    }
    public boolean configured(){return client.configured();}
    @Override public boolean checkConnection(){return client.configured()&&"ok".equals(client.health().get("status"));}
    @Override public String submit(String b,String j,List<Path> p){throw AnalysisStore.error(org.springframework.http.HttpStatus.CONFLICT,"原生算法提交必须携带工作空间任务及冻结配置，请使用任务分析入口");}
    @Override public Map<String,Object> query(String b,String r){throw AnalysisStore.error(org.springframework.http.HttpStatus.CONFLICT,"原生算法查询必须携带工作空间任务，请使用任务批次入口");}
    private interface Operation<T>{T run()throws Exception;}
    private <T>T context(WaylineJobEntity job,Operation<T> action)throws Exception {
        service().requireJob(job.getWorkspaceId(),job.getJobId());return action.run();
    }
    public Map<String,Object> submit(WaylineJobEntity job,String request)throws Exception {
        return context(job,()->nativeBatch(engine.submit(job.getWorkspaceId(),job.getJobId(),request)));
    }
    public List<Map<String,Object>> batches(WaylineJobEntity job)throws Exception {
        return context(job,()->{List<Map<String,Object>> out=new ArrayList<>();for(var b:engine.batches(job.getWorkspaceId(),job.getJobId()))out.add(nativeBatch(b));return out;});
    }
    public Map<String,Object> batch(WaylineJobEntity job,String id)throws Exception {
        return context(job,()->nativeBatch(engine.refresh(job.getWorkspaceId(),job.getJobId(),id)));
    }
    public Map<String,Object> generate(WaylineJobEntity job,String id)throws Exception {
        return context(job,()->nativeBatch(engine.generateReport(job.getWorkspaceId(),job.getJobId(),id)));
    }
    public Path evidence(WaylineJobEntity job,String id,String photo)throws Exception {
        return context(job,()->engine.result(job.getWorkspaceId(),job.getJobId(),id,photo));
    }
    public Path report(WaylineJobEntity job,String id)throws Exception {
        return context(job,()->engine.report(job.getWorkspaceId(),job.getJobId(),id));
    }
    private Map<String,Object> nativeBatch(Map<String,Object> b) {
        Map<String,Object> out=new LinkedHashMap<>();
        String[][] names={{"batchId","batch_id"},{"requestId","request_id"},{"taskId","job_id"},{"remoteId","remote_id"},{"createdAt","created_at"},{"reportReady","report_ready"}};
        out.putAll(b);for(String[] n:names){out.remove(n[0]);out.put(n[1],b.get(n[0]));}
        if(b.get("createdAt") instanceof String)out.put("created_at",java.time.Instant.parse((String)b.get("createdAt")).toEpochMilli());
        List<Map<String,Object>> photos=new ArrayList<>();
        for(Map<String,Object> p:(List<Map<String,Object>>)b.getOrDefault("photos",List.of())){
            Map<String,Object> photo=new LinkedHashMap<>(p);photo.put("file_id",p.get("id"));photo.put("name",p.get("fileName"));photo.put("area_id",p.get("areaId"));
            photo.put("image_url",p.get("imageUrl")==null?null:"/api/construction/tasks/"+b.get("taskId")+"/analysis/batches/"+b.get("batchId")+"/photos/"+p.get("id")+"/result");
            for(String k:List.of("id","fileName","areaId","imageUrl","originalUrl","sha256"))photo.remove(k);photos.add(photo);
        }
        out.put("photos",photos);return out;
    }
    @javax.annotation.PreDestroy public void close(){engine.close();}
}
