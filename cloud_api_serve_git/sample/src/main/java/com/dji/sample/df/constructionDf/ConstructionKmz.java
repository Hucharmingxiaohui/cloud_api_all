package com.dji.sample.df.constructionDf;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.Point;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;

/** Bounded, in-memory parser. No ZIP extraction or XML external resources. */
public final class ConstructionKmz {
    public static final int LIMIT=20*1024*1024;
    public final List<Point> points=new ArrayList<>();
    public final List<Map<String,Object>> preview=new ArrayList<>();
    public String heightMode;
    public int drone,droneSub,payload,payloadSub;
    private static RuleException bad(String text) { return new RuleException(400,text); }
    private static List<Element> children(Element parent,String name) {
        List<Element> out=new ArrayList<>();
        for(Node n=parent.getFirstChild();n!=null;n=n.getNextSibling()) if(n instanceof Element && name.equals(n.getLocalName())) out.add((Element)n);
        return out;
    }
    private static Element one(Element e,String name) {
        List<Element> found=children(e,name);
        if(found.size()!=1) throw bad("KMZ 字段缺失或重复："+name);
        return found.get(0);
    }
    private static String text(Element e,String name) { return one(e,name).getTextContent().trim(); }
    // Match the existing PayloadInfo model: an omitted payload sub-model is zero.
    // Only omission is compatible; duplicate, empty and malformed values still fail.
    private static int payloadSub(Element payload) {
        List<Element> values=children(payload,"payloadSubEnumValue");
        if(values.isEmpty()) return 0;
        if(values.size()!=1) throw bad("KMZ 字段重复：payloadSubEnumValue");
        try {
            int value=Integer.parseInt(values.get(0).getTextContent().trim());
            if(value<0) throw new NumberFormatException();
            return value;
        } catch(NumberFormatException e) { throw bad("KMZ 字段无效：payloadSubEnumValue"); }
    }
    private static double number(String s) { double n=Double.parseDouble(s); if(!Double.isFinite(n)) throw bad("KMZ 含无效数字"); return n; }
    private static Element document(byte[] bytes) throws Exception {
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance(); f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        f.setFeature("http://xml.org/sax/features/external-general-entities",false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,""); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        f.setXIncludeAware(false); f.setExpandEntityReferences(false);
        Element root=f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes)).getDocumentElement();
        return one(root,"Document");
    }
    public static byte[] readBounded(InputStream in) throws IOException {
        byte[] bytes=in.readNBytes(LIMIT+1);
        if(bytes.length>LIMIT) throw bad("KMZ 文件超过 20MB");
        return bytes;
    }
    public static ConstructionKmz parse(byte[] bytes) {
        try {
            if(bytes.length>LIMIT) throw bad("KMZ 文件超过 20MB");
            Map<String,byte[]> files=new HashMap<>(); int count=0,total=0;
            try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
                for(ZipEntry entry; (entry=zip.getNextEntry())!=null;) {
                    if(++count>200) throw bad("KMZ 条目过多");
                    byte[] data=readBounded(zip); total+=data.length;
                    if(total>LIMIT) throw bad("KMZ 解压内容超过 20MB");
                    if(files.put(entry.getName(),data)!=null) throw bad("KMZ 含重复条目");
                }
            }
            if(!files.containsKey("wpmz/template.kml")||!files.containsKey("wpmz/waylines.wpml")) throw bad("需要包含 template.kml 和 waylines.wpml 的 DJI KMZ");
            Element template=document(files.get("wpmz/template.kml")), execution=document(files.get("wpmz/waylines.wpml"));
            Element tf=one(template,"Folder"),ef=one(execution,"Folder");
            if(!"waypoint".equals(text(tf,"templateType"))) throw bad("本期仅支持航点航线，不支持面状模板");
            ConstructionKmz result=new ConstructionKmz();
            result.heightMode=text(ef,"executeHeightMode");
            if(!Set.of("WGS84","relativeToStartPoint").contains(result.heightMode)) throw bad("暂不支持该高度模式："+result.heightMode);
            Element mission=one(template,"missionConfig"),drone=one(mission,"droneInfo"),payload=one(mission,"payloadInfo");
            result.drone=Integer.parseInt(text(drone,"droneEnumValue")); result.droneSub=Integer.parseInt(text(drone,"droneSubEnumValue"));
            result.payload=Integer.parseInt(text(payload,"payloadEnumValue")); result.payloadSub=payloadSub(payload);
            Element em=one(execution,"missionConfig");
            for(String group:List.of("droneInfo","payloadInfo")) for(String field:group.equals("droneInfo")?List.of("droneEnumValue","droneSubEnumValue"):List.of("payloadEnumValue"))
                if(!text(one(mission,group),field).equals(text(one(em,group),field))) throw bad("模板与执行文件机型信息不一致");
            if(result.payloadSub!=payloadSub(one(em,"payloadInfo"))) throw bad("模板与执行文件机型信息不一致");
            List<Element> tps=children(tf,"Placemark"),eps=children(ef,"Placemark");
            if(tps.size()!=eps.size()||eps.size()<2||eps.size()>2000) throw bad("模板和执行航点数量不一致或超出 2–2000 点限制");
            for(int i=0;i<eps.size();i++) {
                Element p=eps.get(i),tp=tps.get(i);
                if(Integer.parseInt(text(p,"index"))!=i||Integer.parseInt(text(tp,"index"))!=i) throw bad("航点序号必须从0连续递增");
                String[] xy=text(one(p,"Point"),"coordinates").split(","),txy=text(one(tp,"Point"),"coordinates").split(",");
                double lng=number(xy[0]),lat=number(xy[1]);
                if(Math.abs(lng)>180||Math.abs(lat)>85||Math.abs(lng-number(txy[0]))>1e-8||Math.abs(lat-number(txy[1]))>1e-8) throw bad("模板与执行航点坐标不一致");
                String turn=text(one(p,"waypointTurnParam"),"waypointTurnMode");
                if(!"toPointAndStopWithDiscontinuityCurvature".equals(turn)) throw bad("区域约束航线仅支持直线到点停转弯，航点 "+(i+1));
                List<Element> straight=children(p,"useStraightLine");
                if(!straight.isEmpty()&&!"1".equals(straight.get(0).getTextContent().trim())) throw bad("航点必须采用直线飞行");
                double height=number(text(p,"executeHeight")),speed=number(text(p,"waypointSpeed"));
                if(speed<=0||speed>15) throw bad("航点速度应大于0且不超过15m/s");
                result.points.add(new Point(lng,lat));
                result.preview.add(Map.of("lng",lng,"lat",lat,"height",height));
            }
            return result;
        } catch(RuleException e) { throw e; } catch(Exception e) { throw bad("KMZ 结构或参数无效："+e.getClass().getSimpleName()); }
    }
}
