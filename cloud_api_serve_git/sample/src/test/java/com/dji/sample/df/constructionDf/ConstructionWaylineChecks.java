package com.dji.sample.df.constructionDf;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.Point;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.df.kmzDf.entity.wayline.Wayline;
import com.dji.sample.df.kmzDf.utils.CreateKmz;

public class ConstructionWaylineChecks {
    private static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    private static void rejects(Runnable call,String message) { try { call.run(); } catch(RuleException e) { check(e.getMessage().contains(message),e.getMessage());return; } throw new AssertionError("Expected rejection: "+message); }
    private static byte[] replace(byte[] bytes,String name,String from,String to) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes));ZipOutputStream result=new ZipOutputStream(out)) {
            for(ZipEntry e;(e=zip.getNextEntry())!=null;) {
                byte[] content=zip.readAllBytes();
                if(e.getName().equals(name)) content=new String(content,StandardCharsets.UTF_8).replace(from,to).getBytes(StandardCharsets.UTF_8);
                result.putNextEntry(new ZipEntry(e.getName()));result.write(content);result.closeEntry();
            }
        }
        return out.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        List<Point> polygon=List.of(new Point(120,36),new Point(120.01,36),new Point(120.01,36.01),new Point(120.006,36.01),new Point(120.006,36.004),new Point(120.004,36.004),new Point(120.004,36.01),new Point(120,36.01));
        ConstructionWaylineGeometry.validate(List.of(new Point(120.002,36.002),new Point(120.008,36.002)),polygon);
        ConstructionWaylineGeometry.validate(List.of(polygon.get(0),polygon.get(1)),polygon);
        rejects(()->ConstructionWaylineGeometry.validate(List.of(new Point(120.002,36.008),new Point(120.008,36.008)),polygon),"航段");
        rejects(()->ConstructionWaylineGeometry.validate(List.of(new Point(120.002,36.002),new Point(121,37)),polygon),"航点 2");
        rejects(()->ConstructionWaylineGeometry.validate(List.of(new Point(Double.NaN,36),new Point(120,36)),polygon),"航点 1");
        String fixture="{\"missionConfig\":{\"fileName\":\"fixture\",\"finishAction\":\"goHome\",\"takeOffSecurityHeight\":20,\"globalTransitionalSpeed\":4,\"droneInfo\":{\"droneEnumValue\":77,\"droneSubEnumValue\":0},\"payloadInfo\":{\"payloadEnumValue\":66,\"payloadSubEnumValue\":0,\"payloadPositionIndex\":0}},\"folder\":{\"autoFlightSpeed\":5,\"globalHeight\":90,\"waylineCoordinateSysParam\":{},\"globalWaypointHeadingParam\":{\"waypointHeadingMode\":\"followWayline\"},\"payloadParam\":{\"payloadPositionIndex\":0,\"imageFormat\":\"wide,zoom\"},\"placeMarks\":[{\"point\":{\"coordinates\":\"120.002,36.002\"},\"useGlobalHeight\":1,\"useGlobalSpeed\":1,\"useGlobalHeadingParam\":1,\"useGlobalTurnParam\":1},{\"point\":{\"coordinates\":\"120.008,36.002\"},\"useGlobalHeight\":1,\"useGlobalSpeed\":1,\"useGlobalHeadingParam\":1,\"useGlobalTurnParam\":1}]}}";
        Path temp=Files.createTempDirectory("construction-route-fixture-");
        try {
            Files.createDirectories(temp.resolve("wpmz"));
            Wayline wayline=new ObjectMapper().readValue(fixture,Wayline.class);
            byte[] valid=Files.readAllBytes(Path.of(CreateKmz.craeteKmz(wayline,temp.toString())));
            ConstructionKmz parsed=ConstructionKmz.parse(valid);
            check(parsed.points.size()==2,"execution points"); check(parsed.heightMode.equals("relativeToStartPoint"),"height mode preserved");
            ConstructionWaylineGeometry.validate(parsed.points,polygon);
            ConstructionWaylineServiceChecks.run(valid);
            String sub="<wpml:payloadSubEnumValue>0</wpml:payloadSubEnumValue>";
            byte[] missingTemplate=replace(valid,"wpmz/template.kml",sub,"");
            byte[] missingExecution=replace(valid,"wpmz/waylines.wpml",sub,"");
            byte[] missingBoth=replace(missingTemplate,"wpmz/waylines.wpml",sub,"");
            check(!Arrays.equals(valid,missingTemplate),"fixture must contain payload sub-model");
            check(ConstructionKmz.parse(missingTemplate).payloadSub==0,"omitted template defaults to zero");
            check(ConstructionKmz.parse(missingExecution).payloadSub==0,"omitted execution defaults to zero");
            check(ConstructionKmz.parse(missingBoth).payloadSub==0,"both omitted default to zero");
            ConstructionWaylineServiceChecks.run(missingBoth);
            byte[] duplicateSub=replace(valid,"wpmz/template.kml",sub,sub+sub);
            rejects(()->ConstructionKmz.parse(duplicateSub),"字段重复");
            byte[] emptySub=replace(valid,"wpmz/template.kml",sub,"<wpml:payloadSubEnumValue/>");
            rejects(()->ConstructionKmz.parse(emptySub),"字段无效");
            byte[] invalidSub=replace(valid,"wpmz/template.kml",sub,"<wpml:payloadSubEnumValue>abc</wpml:payloadSubEnumValue>");
            rejects(()->ConstructionKmz.parse(invalidSub),"字段无效");
            byte[] conflictingSub=replace(missingTemplate,"wpmz/waylines.wpml",sub,"<wpml:payloadSubEnumValue>1</wpml:payloadSubEnumValue>");
            rejects(()->ConstructionKmz.parse(conflictingSub),"机型信息不一致");
            System.out.println("Payload sub-model compatibility checks passed: missing template/execution/both, import service, duplicates, empty/invalid values and conflicting explicit sub-model.");
            byte[] mismatched=replace(valid,"wpmz/waylines.wpml","120.008,36.002","120.009,36.002");
            rejects(()->ConstructionKmz.parse(mismatched),"坐标不一致");
            byte[] curved=replace(valid,"wpmz/waylines.wpml","toPointAndStopWithDiscontinuityCurvature","coordinateTurn");
            rejects(()->ConstructionKmz.parse(curved),"到点停");
            byte[] extraFolder=replace(valid,"wpmz/waylines.wpml","</Document>","<Folder/></Document>");
            rejects(()->ConstructionKmz.parse(extraFolder),"Folder");
            byte[] xxe=replace(valid,"wpmz/template.kml","<kml ","<!DOCTYPE kml [<!ENTITY xxe SYSTEM 'file:///not-allowed'>]><kml ");
            rejects(()->ConstructionKmz.parse(xxe),"结构");
            byte[] mode=replace(valid,"wpmz/waylines.wpml","relativeToStartPoint","realTimeFollowSurface");
            rejects(()->ConstructionKmz.parse(mode),"高度模式");
            rejects(()->ConstructionKmz.parse(new byte[ConstructionKmz.LIMIT+1]),"20MB");
            Files.write(Path.of(System.getProperty("java.io.tmpdir"),"construction-wayline-fixture.kmz"),valid);
            System.out.println("Construction wayline checks passed: concave crossing, boundary edge, invalid coordinates, generated KMZ roundtrip, height, template/execution mismatch, curves, multiple folders, XXE and size limits.");
        } finally {
            try(java.util.stream.Stream<Path> paths=Files.walk(temp)) { for(Path p:paths.sorted(Comparator.reverseOrder()).toArray(Path[]::new)) Files.deleteIfExists(p); }
        }
    }
}
