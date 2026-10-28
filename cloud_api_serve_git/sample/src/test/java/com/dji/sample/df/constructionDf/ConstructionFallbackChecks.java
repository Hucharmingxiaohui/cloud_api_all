package com.dji.sample.df.constructionDf;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.*;
import org.springframework.beans.factory.ObjectProvider;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.service.IWaylineRedisService;

public class ConstructionFallbackChecks {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    interface Work{void run()throws Exception;}
    static void reject(Work action)throws Exception{try{action.run();}catch(ConstructionAreaGeometry.RuleException|org.springframework.web.server.ResponseStatusException e){return;}throw new AssertionError("Expected rejection");}
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler);}
    static byte[] fixture(String mode)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(ConstructionOverallChecks.fixture("reachPoint")));ZipOutputStream zip=new ZipOutputStream(out)){
            for(ZipEntry e;(e=in.getNextEntry())!=null;){String xml=new String(in.readAllBytes(),StandardCharsets.UTF_8);
                if(mode.equals("missing"))xml=xml.replaceAll("<wpml:fileSuffix>.*?</wpml:fileSuffix>","");
                if(mode.equals("duplicate"))xml=xml.replaceAll("航点[123]","same");
                if(mode.equals("oriented"))xml=xml.replace("takePhoto","orientedShoot").replaceAll("<wpml:fileSuffix>.*?</wpml:fileSuffix>","");
                if(mode.equals("mismatch")&&e.getName().endsWith("template.kml"))xml=xml.replace("航点1","other");
                zip.putNextEntry(new ZipEntry(e.getName()));zip.write(xml.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("wpmz/res/image/reference.jpg"));zip.write(new byte[]{1,7,9,3});zip.closeEntry();
        }return out.toByteArray();
    }
    static Map<String,byte[]> unzip(byte[] data)throws Exception{Map<String,byte[]> entries=new HashMap<>();try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(data))){for(ZipEntry e;(e=in.getNextEntry())!=null;)entries.put(e.getName(),in.readAllBytes());}return entries;}
    static void naming()throws Exception{
        byte[] unchanged=fixture("valid");var same=ConstructionPhotoNaming.prepare(unchanged);check(!same.changed()&&Arrays.equals(unchanged,same.bytes()),"valid route remains byte-identical");
        for(String type:List.of("missing","duplicate","oriented")){
            byte[] before=fixture(type);var after=ConstructionPhotoNaming.prepare(before);
            check(after.changedPoints().equals(List.of(1,2,3)),"all affected points reported");
            var parsed=ConstructionKmz.parse(after.bytes());Set<String> suffixes=new HashSet<>();for(var names:parsed.photoSuffixes.values()){check(names.size()==1,"one identifier per action");suffixes.addAll(names);}check(suffixes.size()==3,"unique naming");
            check(ConstructionKmz.parse(before).preview.equals(parsed.preview),"flight points unchanged");
            var input=unzip(before);var output=unzip(after.bytes());check(input.keySet().equals(output.keySet()),"all KMZ resources retained");
            check(Arrays.equals(input.get("wpmz/res/image/reference.jpg"),output.get("wpmz/res/image/reference.jpg")),"reference image bytes unchanged");
            for(String xml:List.of("wpmz/template.kml","wpmz/waylines.wpml")){
                String text=new String(output.get(xml),StandardCharsets.UTF_8);for(String suffix:suffixes)check(text.contains(suffix),"template/execution naming aligned");
                check(text.contains("<wpml:waypointSpeed>5</wpml:waypointSpeed>"),"speed retained");
            }
            check(!ConstructionPhotoNaming.prepare(after.bytes()).changed(),"preparation is idempotent");
        }
        reject(()->ConstructionPhotoNaming.prepare(fixture("mismatch")));
        reject(()->ConstructionPhotoNaming.prepare(ConstructionOverallChecks.fixture("multipleTiming")));
    }
    @SuppressWarnings("unchecked") static void associations()throws Exception{
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(),"fallback"),MediaFileEntity.class);
        var points=List.of(ConstructionOverallChecks.point(1,"WP1"),ConstructionOverallChecks.point(2,"WP2"));
        Map<String,Object> snapshot=Map.of("points",points,"bindings",List.of(Map.of("area_id","a","area_name","area A","waypoints",List.of(1)),Map.of("area_id","b","area_name","area B","waypoints",List.of(2))));
        var overall=new ConstructionOverallService(null,null,null,null,null){@Override public Map<String,Object> jobSnapshot(String w,String j){return snapshot;}};
        Path root=Files.createTempDirectory("construction-fallback-check-");Path image=root.resolve("source.jpg");Files.write(image,new byte[]{1,2,3});
        List<MediaFileEntity> photos=List.of(MediaFileEntity.builder().fileId("one").fileName("DJI_WP1.JPG").build(),MediaFileEntity.builder().fileId("two").fileName("unknown.JPG").build());
        IFileMapperDf media=proxy(IFileMapperDf.class,(p,m,a)->{if(m.getName().equals("selectList"))return photos;throw new UnsupportedOperationException(m.getName());});
        IFileService files=proxy(IFileService.class,(p,m,a)->{if(m.getName().equals("getObjectUrl"))return image.toUri().toURL();throw new UnsupportedOperationException(m.getName());});
        IWaylineRedisService redis=proxy(IWaylineRedisService.class,(p,m,a)->Optional.empty());
        int[] submitted={0,0};ConstructionAnalysisGateway gateway=new ConstructionAnalysisGateway(){public boolean checkConnection(){return true;}public String submit(String batch,String job,List<Path> paths){submitted[0]++;submitted[1]=paths.size();return "remote";}public Map<String,Object> query(String batch,String remote){return Map.of("status","SUCCEEDED","result",Map.of());}};
        ObjectProvider<ConstructionAnalysisGateway> provider=proxy(ObjectProvider.class,(p,m,a)->gateway);
        ConstructionTaskService service=new ConstructionTaskService(null,null,media,files,redis,provider,root.resolve("data").toString());
        Field f=ConstructionTaskService.class.getDeclaredField("overall");f.setAccessible(true);f.set(service,overall);
        WaylineJobEntity job=new WaylineJobEntity();job.setJobId("job");job.setWorkspaceId("w");job.setName("test");job.setStatus(3);job.setMediaCount(2);
        service.archive(job);
        Path archived = service.photo(job, "one");
        check(archived.getFileName().toString().endsWith(".jpg"), "original image extension preserved");
        check(Arrays.equals(Files.readAllBytes(image), Files.readAllBytes(archived)), "archive preserves original bytes");
        Path legacy = archived.resolveSibling(archived.getFileName().toString().replace(".jpg", ".original"));
        Files.move(archived, legacy);
        check(service.photo(job, "one").equals(legacy), "old archive remains readable");
        Path offline = image.resolveSibling("source-offline.jpg");
        Files.move(image, offline);
        service.archive(job);
        check(service.photo(job, "one").equals(archived), "legacy archive restored with image extension offline");
        check(Arrays.equals(Files.readAllBytes(legacy), Files.readAllBytes(archived)), "legacy recovery preserves bytes");
        check(Files.exists(legacy), "legacy paths retained for existing analysis batches");
        service.archive(job);
        Files.move(offline, image);
        check(((Number)service.detail(job).get("unresolved_count")).intValue()==1,"unknown photo blocks submission");
        reject(()->service.submit(job,"request-0001"));check(submitted[0]==0,"no algorithm call for unresolved media");
        reject(()->service.assignPhotos(job,Map.of("file_ids",List.of("other-job-photo"),"expected_revision",0,"area_id","a"),"user"));
        reject(()->service.assignPhotos(job,Map.of("file_ids",List.of("two"),"expected_revision",0,"area_id","outside-snapshot"),"user"));
        reject(()->service.assignPhotos(job,Map.of("file_ids",List.of("two"),"expected_revision",0,"excluded",true,"reason",""),"user"));
        service.assignPhotos(job,Map.of("file_ids",List.of("two"),"expected_revision",0,"excluded",true,"reason","transit"),"user");
        check(((Number)service.detail(job).get("unresolved_count")).intValue()==0,"explicit exclusion resolves pending decision");
        reject(()->service.assignPhotos(job,Map.of("file_ids",List.of("two"),"expected_revision",0,"area_id","b"),"user"));
        var batch=service.submit(job,"request-0002");check(submitted[1]==1,"excluded image not sent");check(((Map<?,?>)batch.get("photo_associations")).size()==2,"all decisions frozen including excluded image");
        reject(()->service.assignPhotos(job,Map.of("file_ids",List.of("two"),"expected_revision",1,"area_id","b"),"user"));
        service.refresh(job);
        service.assignPhotos(job,Map.of("file_ids",List.of("two"),"expected_revision",1,"area_id","b"),"user");
        check(Boolean.TRUE.equals(service.detail(job).get("analysis_stale")),"changed ownership invalidates display of old analysis");
        service.submit(job,"request-0003");check(submitted[1]==2,"manual region restores image to analysis");
        check(!Boolean.TRUE.equals(service.detail(job).get("analysis_stale")),"new batch uses latest decisions");
        try(var walk=Files.walk(root)){for(Path p:walk.sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList()))Files.deleteIfExists(p);}
    }
    static void preparation()throws Exception{
        Path root=Files.createTempDirectory("construction-preparation-check-");Path source=root.resolve("source.kmz"),execution=root.resolve("execution.kmz");
        byte[] original=fixture("missing");Files.write(source,original);String sign=org.springframework.util.DigestUtils.md5DigestAsHex(original);
        Map<String,com.dji.sdk.cloudapi.wayline.GetWaylineListResponse> library=new HashMap<>();
        library.put("source",new com.dji.sdk.cloudapi.wayline.GetWaylineListResponse().setSign(sign).setName("source"));
        int[] imports={0};Map<String,Object> saved=new HashMap<>();
        var db=new org.springframework.jdbc.core.JdbcTemplate(){
            @Override public List<Map<String,Object>> queryForList(String sql,Object...args){
                if(!"w".equals(args[0]))return List.of();
                if(sql.contains("FROM wayline_file"))return "source".equals(args[1])?List.of(Map.of("wayline_id","source")):List.of();
                return saved.isEmpty()?List.of():List.of(saved);
            }
            @Override public int update(String sql,Object...args){saved.put("prepared_wayline_id",args[3]);saved.put("prepared_sign",args[4]);saved.put("source_wayline_id",args[1]);saved.put("source_sign",args[2]);return 1;}
        };
        var files=proxy(com.dji.sample.wayline.service.IWaylineFileService.class,(p,m,a)->{
            if(m.getName().equals("getWaylineByWaylineId"))return Optional.ofNullable(library.get(a[1]));
            if(m.getName().equals("getObjectUrl"))return (a[1].equals("source")?source:execution).toUri().toURL();
            if(m.getName().equals("importKmzFileWithId")){imports[0]++;byte[] bytes=((org.springframework.web.multipart.MultipartFile)a[0]).getBytes();Files.write(execution,bytes);library.put("exec",new com.dji.sdk.cloudapi.wayline.GetWaylineListResponse().setSign(org.springframework.util.DigestUtils.md5DigestAsHex(bytes)).setName("exec"));return "exec";}
            throw new UnsupportedOperationException(m.getName());
        });
        var service=new ConstructionPreparationService(db,files);
        reject(()->service.prepare("foreign","source",sign,"user"));
        reject(()->service.prepare("w","source","stale","user"));
        check("exec".equals(service.prepare("w","source",sign,"user")),"execution route registered");
        check("exec".equals(service.prepare("w","source",sign,"user"))&&imports[0]==1,"retry reuses generated file");
        check(Arrays.equals(Files.readAllBytes(source),original),"original file untouched");
        check("source".equals(service.lineage("w","exec").get("source_wayline_id")),"source-to-execution provenance");
        Files.write(execution,new byte[]{1});reject(()->service.prepare("w","source",sign,"user"));
        try(var walk=Files.walk(root)){for(Path path:walk.sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList()))Files.deleteIfExists(path);}
    }
    public static void main(String[] args)throws Exception{naming();preparation();associations();System.out.println("Fallback checks passed: identifier preparation/reuse/provenance, original/resource preservation, unsupported triggers, manual ownership, revision locking, exclusion, submission guard and frozen batches.");}
}
