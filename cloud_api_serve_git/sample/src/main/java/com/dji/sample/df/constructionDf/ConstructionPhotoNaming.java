package com.dji.sample.df.constructionDf;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/** Changes supported photo naming fields only; never reconstructs a flight from a reduced model. */
public final class ConstructionPhotoNaming {
    public static final class Result {
        private final byte[] bytes;
        private final List<Integer> changedPoints;

        public Result(byte[] bytes, List<Integer> changedPoints) {
            this.bytes = bytes;
            this.changedPoints = changedPoints;
        }

        public byte[] bytes() { return bytes; }
        public List<Integer> changedPoints() { return changedPoints; }
        public boolean changed() { return !changedPoints.isEmpty(); }
    }

    private static final class Shot {
        private final int point;
        private final Element params;
        private final String type;
        private final String suffix;

        private Shot(int point, Element params, String type, String suffix) {
            this.point = point;
            this.params = params;
            this.type = type;
            this.suffix = suffix;
        }
    }
    private static RuntimeException bad(String s){return new ConstructionAreaGeometry.RuleException(400,s);}
    private static List<Element> children(Element e,String name){List<Element> out=new ArrayList<>();for(Node n=e.getFirstChild();n!=null;n=n.getNextSibling())if(n instanceof Element && name.equals(n.getLocalName()))out.add((Element)n);return out;}
    private static Element one(Element e,String name){var v=children(e,name);if(v.size()!=1)throw bad("拍照命名字段结构不支持："+name);return v.get(0);}
    private static String text(Element e,String name){return one(e,name).getTextContent().trim();}
    private static Document xml(byte[] bytes)throws Exception{
        var f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        f.setFeature("http://xml.org/sax/features/external-general-entities",false);f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");f.setXIncludeAware(false);f.setExpandEntityReferences(false);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }
    private static List<Shot> shots(Document doc){
        List<Shot> result=new ArrayList<>();
        Element folder=one(one(doc.getDocumentElement(),"Document"),"Folder");
        for(Element point:children(folder,"Placemark")){
            int index=Integer.parseInt(text(point,"index"));
            for(Element group:children(point,"actionGroup"))for(Element action:children(group,"action")){
                String type=text(action,"actionActuatorFunc");if(!Set.of("takePhoto","orientedShoot").contains(type))continue;
                if(!"reachPoint".equals(text(one(group,"actionTrigger"),"actionTriggerType"))||Integer.parseInt(text(group,"actionGroupStartIndex"))!=index||Integer.parseInt(text(group,"actionGroupEndIndex"))!=index)throw bad("自动命名仅支持到点拍照，不支持等时、等距或跨航点拍照");
                Element params=one(action,"actionActuatorFuncParam");
                var nativeName=children(params,"orientedShoot".equals(type)?"orientedFileSuffix":"fileSuffix");
                var legacy=children(params,"fileSuffix");
                if(nativeName.size()>1||legacy.size()>1)throw bad("重复的拍照命名字段，请检查航线");
                if("orientedShoot".equals(type)&&!nativeName.isEmpty()&&!legacy.isEmpty()&&!nativeName.get(0).getTextContent().trim().equals(legacy.get(0).getTextContent().trim()))throw bad("精准复拍的两种命名字段冲突，请检查航线");
                var names=nativeName.isEmpty()?legacy:nativeName;
                result.add(new Shot(index+1,params,type,names.isEmpty()?"":names.get(0).getTextContent().trim()));
            }
        }return result;
    }
    private static void rename(Shot shot,String value){
        var nodes=children(shot.params,"fileSuffix");if("orientedShoot".equals(shot.type))nodes.addAll(children(shot.params,"orientedFileSuffix"));
        if(nodes.isEmpty()){String field="orientedShoot".equals(shot.type)?"orientedFileSuffix":"fileSuffix";Element node=shot.params.getOwnerDocument().createElementNS(shot.params.getNamespaceURI(),"wpml:"+field);shot.params.appendChild(node);nodes.add(node);}
        for(Element node:nodes)node.setTextContent(value);
    }
    private static byte[] serialize(Document doc)throws Exception{
        var f=TransformerFactory.newDefaultInstance();f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");f.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
        var t=f.newTransformer();t.setOutputProperty(OutputKeys.ENCODING,"UTF-8");ByteArrayOutputStream out=new ByteArrayOutputStream();t.transform(new DOMSource(doc),new StreamResult(out));return out.toByteArray();
    }
    public static Result prepare(byte[] original){
        ConstructionKmz before=ConstructionKmz.parse(original);
        if(!before.pointPhotoOnly)throw bad("自动命名仅支持到点拍照");
        try{
            Map<String,byte[]> entries=new LinkedHashMap<>();
            try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(original))){for(ZipEntry e;(e=zip.getNextEntry())!=null;)entries.put(e.getName(),ConstructionKmz.readBounded(zip));}
            var template=xml(entries.get("wpmz/template.kml"));var execution=xml(entries.get("wpmz/waylines.wpml"));
            List<Shot> source=shots(execution),mirror=shots(template);
            if(source.size()!=mirror.size())throw bad("模板与执行文件拍照动作数量不一致，不能自动命名");
            Map<String,Set<Integer>> owners=new HashMap<>();Set<String> used=new HashSet<>();
            for(int i=0;i<source.size();i++){
                Shot a=source.get(i),b=mirror.get(i);
                if(a.point!=b.point||!a.type.equals(b.type)||!a.suffix.equals(b.suffix))throw bad("模板与执行文件拍照动作或后缀不一致，不能自动命名");
                if(!a.suffix.isBlank()){used.add(a.suffix);owners.computeIfAbsent(a.suffix,k->new HashSet<>()).add(a.point);}
            }
            Set<Integer> changed=new LinkedHashSet<>();int serial=1;
            for(int i=0;i<source.size();i++){
                Shot a=source.get(i);
                if(!a.suffix.isBlank()&&owners.get(a.suffix).size()==1)continue;
                String value;do{value=String.format(Locale.ROOT,"GD%04dP%04d",a.point,serial++);}while(used.contains(value));
                used.add(value);rename(a,value);rename(mirror.get(i),value);changed.add(a.point);
            }
            if(changed.isEmpty())return new Result(original,List.of());
            entries.put("wpmz/template.kml",serialize(template));entries.put("wpmz/waylines.wpml",serialize(execution));
            ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){for(var e:entries.entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}}
            byte[] bytes=out.toByteArray();ConstructionKmz after=ConstructionKmz.parse(bytes);
            if(!before.preview.equals(after.preview)||!before.heightMode.equals(after.heightMode))throw bad("执行版航点校验不一致");
            return new Result(bytes,List.copyOf(changed));
        }catch(ConstructionAreaGeometry.RuleException e){throw e;}catch(Exception e){throw bad("无法生成安全的拍照命名执行版："+e.getMessage());}
    }
}
