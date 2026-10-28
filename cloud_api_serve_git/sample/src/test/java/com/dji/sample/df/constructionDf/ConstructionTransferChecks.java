package com.dji.sample.df.constructionDf;

import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.service.IWaylineRedisService;
import org.springframework.beans.factory.ObjectProvider;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unchecked")
public class ConstructionTransferChecks {
    static void check(boolean b,String text) {if(!b)throw new AssertionError(text);}
    interface Work {void run() throws Exception;}
    static void reject(Work run) throws Exception {try{run.run();}catch(Exception expected){return;}throw new AssertionError("expected rejection");}
    static <T> T proxy(Class<T> type,InvocationHandler h){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},h);}
    static final ObjectMapper JSON=new ObjectMapper();
    static void alter(Path source,Path target,String mode) throws Exception {
        try(ZipFile in=new ZipFile(source.toFile());ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(target))) {
            for(var entry:Collections.list(in.entries())) {
                if(mode.equals("missing")&&entry.getName().startsWith("originals/"))continue;
                byte[] bytes;try(var stream=in.getInputStream(entry)){bytes=stream.readAllBytes();}
                if(mode.equals("corrupt")&&entry.getName().startsWith("originals/"))bytes[0]^=1;
                if(mode.equals("conflict")&&entry.getName().equals("manifest.json")){Map<String,Object> doc=JSON.readValue(bytes,Map.class);((Map<String,Object>)doc.get("task")).put("name","changed");bytes=JSON.writeValueAsBytes(doc);}
                out.putNextEntry(new ZipEntry(entry.getName()));out.write(bytes);out.closeEntry();
            }
            if(mode.equals("traversal")){out.putNextEntry(new ZipEntry("../escape.jpg"));out.write(1);out.closeEntry();}
        }
    }
    public static void main(String[] args) throws Exception {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),"transfer"),MediaFileEntity.class);
        Path root=Files.createTempDirectory("construction-transfer-check-");
        try {
            Path original=root.resolve("source.jpg");Files.write(original,new byte[]{(byte)255,(byte)216,1,2,(byte)255,(byte)217});
            var photos=List.of(MediaFileEntity.builder().fileId("image-1").fileName("DJI_WP1.JPG").build(),MediaFileEntity.builder().fileId("image-2").fileName("DJI_WP2.JPG").build());
            IFileMapperDf media=proxy(IFileMapperDf.class,(p,m,a)->photos);
            IFileService files=proxy(IFileService.class,(p,m,a)->original.toUri().toURL());
            IWaylineRedisService redis=proxy(IWaylineRedisService.class,(p,m,a)->Optional.empty());
            int[] submissions={0};
            ConstructionAnalysisGateway gateway=new ConstructionAnalysisGateway(){
                public boolean checkConnection(){return true;}
                public String submit(String b,String j,List<Path> paths) throws Exception {check(paths.size()==2,"both imported photos analyzed");for(Path path:paths)check(Arrays.equals(Files.readAllBytes(path),Files.readAllBytes(original)),"analysis reads imported originals");submissions[0]++;return "remote";}
                public Map<String,Object> query(String b,String r){return Map.of("status","SUCCEEDED","result",Map.of());}
            };
            ObjectProvider<ConstructionAnalysisGateway> provider=proxy(ObjectProvider.class,(p,m,a)->gateway);
            ConstructionTaskService source=new ConstructionTaskService(null,null,media,files,redis,provider,root.resolve("source-data").toString());
            Map<String,Object> snapshot=Map.of("overall_id","route","points",List.of(ConstructionOverallChecks.point(1,"WP1"),ConstructionOverallChecks.point(2,"WP2")),"bindings",List.of(Map.of("area_id","a","area_name","工作面A","waypoints",List.of(1)),Map.of("area_id","b","area_name","工作面B","waypoints",List.of(2))));
            var overall=new ConstructionOverallService(null,null,null,null,null){@Override public Map<String,Object> jobSnapshot(String w,String j){return snapshot;}};
            Field field=ConstructionTaskService.class.getDeclaredField("overall");field.setAccessible(true);field.set(source,overall);
            WaylineJobEntity nativeJob=new WaylineJobEntity();nativeJob.setWorkspaceId("source-workspace");nativeJob.setJobId("source-job");nativeJob.setName("固定机巢巡检");nativeJob.setDockSn("dock-fixed");nativeJob.setStatus(3);nativeJob.setMediaCount(2);nativeJob.setExecuteTime(1000L);
            ConstructionTransferService sender=new ConstructionTransferService(source,root.resolve("sender").toString(),1000000);
            nativeJob.setStatus(1);reject(()->source.exportTransfer(nativeJob,sender));nativeJob.setStatus(3);
            Path exported=source.exportTransfer(nativeJob,sender);
            check(Files.exists(exported),"export writes a finished ZIP");
            ConstructionTaskService receiverTasks=new ConstructionTaskService(null,null,null,null,null,provider,root.resolve("receiver-data").toString());
            ConstructionTransferService receiver=new ConstructionTransferService(receiverTasks,root.resolve("receiver").toString(),1000000);
            String w="different-workspace";Path inbox=Paths.get((String)receiver.status(w).get("inbox"));
            Files.copy(exported,inbox.resolve("copy.part"));check(((List<?>)receiver.scan(w).get("tasks")).isEmpty(),"unfinished copy ignored");
            Files.move(inbox.resolve("copy.part"),inbox.resolve("copy.zip"));Map<String,Object> scanned=receiver.scan(w);
            List<Map<String,Object>> rows=(List<Map<String,Object>>)scanned.get("tasks");check(rows.size()==1,"one imported task published");String id=(String)rows.get(0).get("job_id");
            WaylineJobEntity job=receiverTasks.requireJob(w,id);check(job.getPlanId()==null,"no executable plan created");
            reject(()->receiverTasks.requireJob("other-workspace",id));
            Map<String,Object> detail=receiverTasks.detail(job);check(Boolean.TRUE.equals(detail.get("media_complete")),"imported task complete without Redis");
            check(((List<Map<String,Object>>)detail.get("photos")).stream().allMatch(p->Boolean.TRUE.equals(p.get("archived"))&&p.containsKey("area_id")),"photos grouped from frozen snapshot");
            check(Arrays.equals(Files.readAllBytes(receiverTasks.photo(job,"image-1")),Files.readAllBytes(original)),"original bytes transferred");
            receiverTasks.submit(job,"offline-analysis-1");check(submissions[0]==1,"imported analysis uses normal gateway");receiverTasks.refresh(job);
            receiverTasks.assignPhotos(job,Map.of("file_ids",List.of("image-1"),"expected_revision",0,"area_id","b","excluded",false),"operator");
            Files.copy(exported,inbox.resolve("repeat.zip"));Map<String,Object> repeated=receiver.scan(w);check(((List<?>)repeated.get("tasks")).size()==1,"repeat does not duplicate task");
            check(((Number)receiverTasks.detail(job).get("association_revision")).intValue()==1,"repeat preserves local edits");
            for(String mode:List.of("conflict","missing","corrupt","traversal")) {
                Path modified=root.resolve(mode+".zip");alter(exported,modified,mode);
                // Use a fresh recipient so invalid ZIPs are fully validated, not merely deduplicated.
                ConstructionTaskService freshTasks=new ConstructionTaskService(null,null,null,null,null,provider,root.resolve(mode+"-data").toString());
                ConstructionTransferService fresh=new ConstructionTransferService(freshTasks,root.resolve(mode+"-transfer").toString(),1000000);
                if(mode.equals("conflict")){fresh.receive(w,exported);}
                reject(()->fresh.receive(w,modified));check(((List<?>)fresh.status(w).get("tasks")).size()==(mode.equals("conflict")?1:0),"invalid package never partially published");
            }
            Path outbound=receiverTasks.exportTransfer(job,receiver);
            ConstructionTaskService thirdTasks=new ConstructionTaskService(null,null,null,null,null,provider,root.resolve("third-data").toString());
            ConstructionTransferService third=new ConstructionTransferService(thirdTasks,root.resolve("third").toString(),1000000);
            check(third.receive("third-w",outbound).get("job_id").equals(id),"re-export preserves original provenance");
            check(((Number)thirdTasks.detail(thirdTasks.requireJob("third-w",id)).get("association_revision")).intValue()==1,"re-export carries manual decisions");
            check(sender.receive("source-workspace",exported).get("status").equals("SKIPPED"),"own source skipped");
            Files.write(inbox.resolve("invalid.zip"),new byte[]{1,2,3});
            Map<String,Object> failed=receiver.scan(w);
            check("FAILED".equals(((List<Map<String,Object>>)failed.get("receipts")).get(0).get("status")),"scanner reports failed packages");
            check(!Files.exists(inbox.resolve("invalid.zip")),"failed package moved out of inbox");
            Map<String,Object> single=Map.of("single_area",true,"points",List.of(),"bindings",List.of(Map.of("area_id","single","area_name","单工作面","waypoints",List.of())));
            field.set(source,new ConstructionOverallService(null,null,null,null,null){@Override public Map<String,Object> jobSnapshot(String workspace,String jobId){return single;}});
            nativeJob.setJobId("single-job");nativeJob.setName("单工作面巡检");
            Path singlePackage=source.exportTransfer(nativeJob,sender);
            String singleId=(String)receiver.receive(w,singlePackage).get("job_id");
            var singleDetail=receiverTasks.detail(receiverTasks.requireJob(w,singleId));
            check(((List<Map<String,Object>>)singleDetail.get("photos")).stream().allMatch(p->"single".equals(p.get("area_id"))),"single-area task retains its own workface without waypoint suffix inference");
            check(((List<?>)receiver.status(w).get("tasks")).size()==2,"different source tasks stay separate");
            check(!Files.exists(root.resolve("escape.jpg")),"ZIP paths cannot escape archive");
            System.out.println("Transfer checks passed: complete-task export, cross-workspace import, original bytes, frozen areas, analysis, repeat/conflict isolation, partial-copy/missing/corrupt/traversal rejection and re-export provenance.");
        } finally {try(var walk=Files.walk(root)){for(Path path:walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList()))Files.deleteIfExists(path);}}
    }
}
