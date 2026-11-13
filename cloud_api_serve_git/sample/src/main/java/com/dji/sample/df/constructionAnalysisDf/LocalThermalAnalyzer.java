package com.dji.sample.df.constructionAnalysisDf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Dedicated local SDK runner. Never uses the legacy temTest/measure.raw directory. */
@Component
public class LocalThermalAnalyzer {
    @Value("${construction.thermal.enabled:false}") private boolean enabled;
    @Value("${construction.thermal.executable:}") private String executable;
    @Value("${construction.thermal.library-path:}") private String libraryPath;
    @Value("${construction.thermal.timeout-seconds:60}") private int timeout;
    public boolean available(){return enabled && executable!=null && !executable.isBlank() && Files.isRegularFile(Paths.get(executable)) && Files.isExecutable(Paths.get(executable));}
    public static boolean requested(Map<String,Object> photo){return "infrared".equals(photo.get("type")) && Boolean.TRUE.equals(photo.get("infrared_temperature_enabled"));}
    public static final class Output {
        public final Map<String,Object> result; public final byte[] image;
        Output(Map<String,Object> result,byte[] image){this.result=result;this.image=image;}
    }
    protected Process start(ProcessBuilder builder)throws IOException{return builder.start();}
    // Shared lock serializes only this new runner, not the existing point/ROI endpoints.
    public synchronized Output analyze(byte[] original,Map<String,Object> policy)throws Exception {
        if(!available())throw new IOException("本地红外SDK未启用或可执行文件不可用");
        Path temp=Files.createTempDirectory("construction-thermal-");Process process=null;
        try {
            Path input=temp.resolve("original.jpg"),raw=temp.resolve("measure.raw"),log=temp.resolve("sdk.log");Files.write(input,original);
            ProcessBuilder builder=new ProcessBuilder(Paths.get(executable).toAbsolutePath().toString(),"-s",input.toString(),"-a","measure","-o",raw.toString());
            builder.directory(temp.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
            if(libraryPath!=null&&!libraryPath.isBlank())builder.environment().put("LD_LIBRARY_PATH",libraryPath);
            builder.environment().put("LC_ALL","C");
            process=start(builder);long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(Math.max(1,Math.min(timeout,600)));
            while(!process.waitFor(200,TimeUnit.MILLISECONDS)){
                if(System.nanoTime()>end)throw new IOException("红外SDK测温超时");
                if(Files.size(log)>1024*1024 || (Files.exists(raw)&&Files.size(raw)>8_000_000))throw new IOException("红外SDK输出超过限制");
            }
            if(process.exitValue()!=0)throw new IOException("红外SDK解析失败（退出码"+process.exitValue()+"），请核对机型支持与原始R-JPEG");
            if(Files.size(log)>1024*1024)throw new IOException("SDK输出日志过大");
            String info=Files.readString(log,StandardCharsets.UTF_8);
            int width=dimension(info,"width"),height=dimension(info,"height");
            long pixels=(long)width*height;
            if(pixels<1||pixels>4_000_000||!Files.isRegularFile(raw)||Files.size(raw)!=pixels*2)throw new IOException("SDK温度矩阵尺寸或格式不匹配，未按固定分辨率猜测");
            ByteBuffer buffer=ByteBuffer.wrap(Files.readAllBytes(raw)).order(ByteOrder.LITTLE_ENDIAN);double[] values=new double[(int)pixels];
            for(int i=0;i<values.length;i++)values[i]=buffer.getShort()/10.0;
            return evaluate(values,width,height,policy);
        }finally {
            if(process!=null&&process.isAlive()){process.destroyForcibly();process.waitFor(5,TimeUnit.SECONDS);}
            // Delete only the known files in the unique directory; never traverse user paths.
            for(String name:java.util.List.of("original.jpg","measure.raw","sdk.log"))Files.deleteIfExists(temp.resolve(name));
            Files.deleteIfExists(temp);
        }
    }
    private static int dimension(String text,String axis)throws IOException {
        Matcher m=Pattern.compile("(?im)image\\s+"+axis+"\\s*:\\s*(\\d+)").matcher(text);
        if(!m.find())throw new IOException("SDK未返回可识别的红外矩阵尺寸");
        try{return Integer.parseInt(m.group(1));}catch(NumberFormatException e){throw new IOException("红外矩阵尺寸无效");}
    }
    public static Output evaluate(double[] values,int width,int height,Map<String,Object> policy)throws IOException {
        Object t=policy.get("max_temperature_c"),m=policy.get("min_hotspot_pixels");
        if(!(t instanceof Number)||!(m instanceof Number))throw new IOException("冻结红外阈值配置缺失");
        double threshold=((Number)t).doubleValue(),minimum=((Number)m).doubleValue();
        if(!Double.isFinite(threshold)||threshold< -50||threshold>1000||!Double.isFinite(minimum)||minimum<1||minimum>10_000_000||minimum!=Math.rint(minimum))throw new IOException("冻结红外阈值配置无效");
        if(width<1||height<1||(long)width*height!=values.length||values.length>4_000_000)throw new IOException("温度矩阵无效");
        double low=Double.POSITIVE_INFINITY,high=Double.NEGATIVE_INFINITY,sum=0;int hottest=0;
        for(int i=0;i<values.length;i++){double v=values[i];if(!Double.isFinite(v)||v< -273.15)throw new IOException("SDK返回无效温度");if(v>high){high=v;hottest=i;}low=Math.min(low,v);sum+=v;}
        boolean[] seen=new boolean[values.length];int[] queue=new int[values.length];java.util.List<Map<String,Object>> regions=new ArrayList<>();
        for(int start=0;start<values.length;start++){
            if(seen[start]||values[start]<threshold)continue;
            int head=0,tail=1,minX=start%width,maxX=minX,minY=start/width,maxY=minY;double peak=values[start];queue[0]=start;seen[start]=true;
            while(head<tail){int at=queue[head++],x=at%width,y=at/width;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);peak=Math.max(peak,values[at]);
                int[] next={x>0?at-1:-1,x+1<width?at+1:-1,y>0?at-width:-1,y+1<height?at+width:-1};
                for(int n:next)if(n>=0&&!seen[n]&&values[n]>=threshold){seen[n]=true;queue[tail++]=n;}
            }
            if(tail>=minimum){if(regions.size()>=1000)throw new IOException("热点区域超过1000个，请调整阈值后重新生成任务配置");regions.add(Map.of("rect",java.util.List.of(minX,minY,maxX-minX+1,maxY-minY+1),"pixels",tail,"max_temperature_c",peak));}
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","success");result.put("max_temperature_c",high);result.put("min_temperature_c",low);result.put("average_temperature_c",sum/values.length);result.put("threshold_c",threshold);result.put("min_hotspot_pixels",(int)minimum);result.put("width",width);result.put("height",height);result.put("hotspot_count",regions.size());result.put("hotspots",regions);result.put("max_point",java.util.List.of(hottest%width,hottest/width));result.put("abnormal",!regions.isEmpty());result.put("message",regions.isEmpty()?"未发现同时满足温度阈值与最小连通像素数的热点":"发现温度异常区域，请结合现场复核；不直接判定失火");
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        for(int i=0;i<values.length;i++){float f=(float)((values[i]-low)/Math.max(.1,high-low));image.setRGB(i%width,i/width,Color.HSBtoRGB((1-f)*.66f,1,1));}
        Graphics2D g=image.createGraphics();try{g.setColor(Color.WHITE);g.setStroke(new BasicStroke(Math.max(1,width/320f)));for(Map<String,Object> region:regions){java.util.List<Integer> r=(java.util.List<Integer>)region.get("rect");g.drawRect(r.get(0),r.get(1),Math.max(0,r.get(2)-1),Math.max(0,r.get(3)-1));}}finally{g.dispose();}
        ByteArrayOutputStream out=new ByteArrayOutputStream();if(!ImageIO.write(image,"jpg",out))throw new IOException("测温结果图生成失败");return new Output(result,out.toByteArray());
    }
}
