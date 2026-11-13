package com.dji.sample.df.constructionAnalysisDf;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;

/** Standalone Java11 checks; synthetic matrices and fake algorithm, no real SDK/flight. */
public class ConstructionThermalChecks {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static final Map<String,Object> POLICY=Map.of("max_temperature_c",50,"min_hotspot_pixels",2);
    static LocalThermalAnalyzer.Output sample()throws Exception{return LocalThermalAnalyzer.evaluate(new double[]{60,60,20,20,20,80},3,2,POLICY);}
    public static void main(String[] args)throws Exception {
        var output=sample();check((int)output.result.get("hotspot_count")==1,"isolated hot pixel must not join separate region");check((double)output.result.get("max_temperature_c")==80,"maximum includes isolated pixels");
        var zero=LocalThermalAnalyzer.evaluate(new double[]{-10,0,10,49},2,2,POLICY);check(Boolean.FALSE.equals(zero.result.get("abnormal")),"no matching region is explicit false");
        var equal=LocalThermalAnalyzer.evaluate(new double[]{50,50},2,1,POLICY);check(Boolean.TRUE.equals(equal.result.get("abnormal")),"threshold and area comparisons inclusive");
        try{LocalThermalAnalyzer.evaluate(new double[]{Double.NaN},1,1,POLICY);throw new AssertionError("NaN accepted");}catch(IOException expected){}
        try{new LocalThermalAnalyzer().analyze(new byte[]{1},POLICY);throw new AssertionError("disabled SDK accepted");}catch(IOException expected){}
        runner();scenario(false,false);scenario(true,false);scenario(false,true);
        System.out.println("Thermal checks passed: connectivity, thresholds, isolated pixels, failure states, local-only no remote request, mixed totals/areas, frozen policy, idempotency, evidence and Word report. Synthetic inputs only.");
    }
    static void runner()throws Exception {
        List<Path> dirs=new ArrayList<>();
        LocalThermalAnalyzer analyzer=new LocalThermalAnalyzer(){
            public boolean available(){return true;}
            protected Process start(ProcessBuilder builder)throws IOException{
                Path dir=builder.directory().toPath();dirs.add(dir);check(Arrays.equals(Files.readAllBytes(dir.resolve("original.jpg")),new byte[]{1,2,3}),"temporary input preserves bytes");
                Files.writeString(dir.resolve("sdk.log"),"R-JPEG resolution size\n image width : 3\n image height : 2\n");
                java.nio.ByteBuffer raw=java.nio.ByteBuffer.allocate(12).order(java.nio.ByteOrder.LITTLE_ENDIAN);for(short t:new short[]{600,600,200,200,200,800})raw.putShort(t);Files.write(dir.resolve("measure.raw"),raw.array());
                return new Process(){public OutputStream getOutputStream(){return OutputStream.nullOutputStream();}public InputStream getInputStream(){return InputStream.nullInputStream();}public InputStream getErrorStream(){return InputStream.nullInputStream();}public int waitFor(){return 0;}public int exitValue(){return 0;}public void destroy(){}};
            }
        };
        var executable=LocalThermalAnalyzer.class.getDeclaredField("executable");executable.setAccessible(true);executable.set(analyzer,"fake-sdk");
        for(int i=0;i<2;i++)check((int)analyzer.analyze(new byte[]{1,2,3},POLICY).result.get("width")==3,"SDK resolution is parsed, not hardcoded");
        check(!dirs.get(0).equals(dirs.get(1)),"each run uses unique temporary directory");for(Path dir:dirs)check(!Files.exists(dir),"temporary files cleaned after measurement");
    }
    static void scenario(boolean mixed,boolean fail)throws Exception {
        Path root=Files.createTempDirectory("construction-thermal-check-");Path input=Files.createDirectories(root.resolve("input/task"));
        byte[] original=sample().image;Files.write(input.resolve("thermal.jpg"),original);if(mixed)Files.write(input.resolve("visible.jpg"),original);
        Map<String,Map<String,Object>> metadata=new LinkedHashMap<>();Map<String,Object> ir=new LinkedHashMap<>();ir.put("id","thermal");ir.put("fileName","thermal.jpg");ir.put("areaId","thermal-area");ir.put("ready",true);ir.put("excluded",false);ir.put("type","infrared");ir.put("checks",List.of());ir.put("infrared_temperature_enabled",true);ir.put("infrared_policy",POLICY);metadata.put("thermal",ir);
        if(mixed)metadata.put("visible",Map.of("id","visible","fileName","visible.jpg","areaId","visible-area","ready",true,"excluded",false,"type","visible","checks",List.of("person_count")));
        int[] sent={0},local={0};
        AnalysisClient client=new AnalysisClient("","",100,100,1000000){
            public boolean configured(){return mixed;}
            public Map<String,Object> submit(Map<String,Object> body){sent[0]++;List<?> images=(List<?>)body.get("images");check(images.size()==1&&"visible".equals(((Map<?,?>)images.get(0)).get("id")),"thermal must never be sent to remote algorithm");return Map.of("task_id","remote-test");}
            public Map<String,Object> poll(String id){return Map.of("status","completed","total",1,"finished",1,"areas",List.of(Map.of("area_id","visible-area","status","success","count",0)));}
            public Map<String,Object> image(String task,String id){return new LinkedHashMap<>(Map.of("checks",List.of(Map.of("code","person_count","status","success","count",0)),"image",Base64.getEncoder().encodeToString(original)));}
        };
        try(AnalysisService service=new AnalysisService(new AnalysisStore(root.resolve("input"),root.resolve("output")),(w,t)->metadata,client,10,1000000,10000000)){
            service.setThermalAnalyzer(new LocalThermalAnalyzer(){public synchronized Output analyze(byte[] bytes,Map<String,Object> policy)throws Exception{local[0]++;check(policy.equals(POLICY),"frozen thresholds passed to local analyzer");check(Arrays.equals(bytes,original),"original preserved");if(fail)throw new IOException("simulated SDK failure");return sample();}});
            String batch=(String)service.submit("workspace","task","request-one").get("batchId");Map<String,Object> value=null;
            for(int i=0;i<100;i++){Thread.sleep(20);try{value=service.refresh("workspace","task",batch);if("SUCCEEDED".equals(value.get("status")))break;}catch(org.springframework.web.server.ResponseStatusException busy){if(busy.getStatus()!=org.springframework.http.HttpStatus.CONFLICT)throw busy;}}
            check(value!=null&&"SUCCEEDED".equals(value.get("status")),"batch completed");check(((Number)value.get("finished")).intValue()==(mixed?2:1),"combined progress");
            Map<String,Object> photo=AnalysisService.photos(value).stream().filter(p->"thermal".equals(p.get("id"))).findFirst().get();check((fail?"PARTIAL":"SUCCEEDED").equals(photo.get("status")),"local failure is never normal");check(((List<?>)value.get("areas")).size()==(mixed?2:1),"thermal-only area is not_requested");
            check(batch.equals(service.submit("workspace","task","request-one").get("batchId")),"idempotent submission");check(local[0]==1&&sent[0]==(mixed?1:0),"no duplicate SDK or remote invocation");
            if(!fail)check(Files.size(service.result("workspace","task",batch,"thermal"))>0,"local image served through existing evidence endpoint");
            service.generateReport("workspace","task",batch);Path report=service.report("workspace","task",batch);try(ZipFile zip=new ZipFile(report.toFile())){String xml=new String(zip.getInputStream(zip.getEntry("word/document.xml")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);check(xml.contains("本地红外测温"),"report contains temperature results");}
            try(ZipFile zip=new ZipFile(report.toFile())){
                String xml=new String(zip.getInputStream(zip.getEntry("word/document.xml")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                check(xml.contains("东方电子股份有限公司")&&xml.contains("复核与处理记录")&&xml.contains("thermal.jpg"),"report branding, review and file identity");
                check(xml.contains("tblHeader")&&xml.contains("pageBreakBefore"),"repeated headers and chapter pagination");
                check(zip.getEntry("word/header1.xml")!=null&&zip.getEntry("word/footer1.xml")!=null,"report header and footer");
            }
            String preview=System.getProperty("construction.report.preview");
            if(mixed&&preview!=null)Files.copy(report,Path.of(preview),StandardCopyOption.REPLACE_EXISTING);
            check(Arrays.equals(original,Files.readAllBytes(input.resolve("thermal.jpg"))),"source file unchanged");
        }finally {try(var files=Files.walk(root)){for(Path p:files.sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList()))Files.deleteIfExists(p);}}
    }
}
