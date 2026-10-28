package com.dji.sample.df.constructionDf;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Native DJI precise re-shoot naming and compatibility with existing platform KMZs. */
public class ConstructionOrientedSuffixChecks {
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static byte[] oriented(boolean nativeSuffix, boolean repeatedAction) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ZipInputStream input=new ZipInputStream(new ByteArrayInputStream(ConstructionOverallChecks.fixture("reachPoint")));
            ZipOutputStream output=new ZipOutputStream(bytes)) {
            ZipEntry entry;
            while((entry=input.getNextEntry())!=null) {
                String xml=new String(input.readAllBytes(),StandardCharsets.UTF_8).replace("takePhoto","orientedShoot");
                if(nativeSuffix) xml=xml.replace("fileSuffix","orientedFileSuffix");
                if(repeatedAction) xml=xml.replaceAll("(<wpml:action>.*?</wpml:action>)","$1$1");
                output.putNextEntry(new ZipEntry(entry.getName()));output.write(xml.getBytes(StandardCharsets.UTF_8));output.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
    static void verify(ConstructionKmz kmz,int size) {
        check(kmz.pointPhotoOnly,"point-triggered photos supported");
        check(kmz.points.size()==size,"point count preserved");
        List<Map<String,Object>> points=new ArrayList<>();
        for(int i=1;i<=size;i++) {
            check(new HashSet<>(kmz.photoSuffixes.get(i)).equals(Set.of("航点"+i)),"suffix parsed at point "+i);
            points.add(Map.of("index",i,"photo_suffixes",kmz.photoSuffixes.get(i)));
        }
        var bindings=ConstructionOverallService.bindings(List.of(Map.of("area_id","area","area_version",1,"waypoints",java.util.stream.IntStream.rangeClosed(1,size).boxed().collect(java.util.stream.Collectors.toList()))),points);
        bindings.get(0).put("area_name","workface");
        Map<String,Object> snapshot=Map.of("points",points,"bindings",bindings);
        for(int i=1;i<=size;i++) for(String lens:List.of("V","T")) {
            var match=ConstructionOverallService.matchPhoto(snapshot,"DJI_20261028_0001_"+lens+"_航点"+i+".JPG");
            check("area".equals(match.get("area_id"))&&Integer.valueOf(i).equals(match.get("waypoint_index")),"photo-to-waypoint-to-area "+i);
        }
    }
    public static void main(String[] args) throws Exception {
        verify(ConstructionKmz.parse(ConstructionOverallChecks.fixture("reachPoint")),3);
        verify(ConstructionKmz.parse(oriented(true,false)),3);
        verify(ConstructionKmz.parse(oriented(false,false)),3);
        verify(ConstructionKmz.parse(oriented(true,true)),3);
        if(args.length>0) verify(ConstructionKmz.parse(Files.readAllBytes(Path.of(args[0]))),7);
        System.out.println("Oriented suffix checks passed: native/legacy/takePhoto, same-point repeat actions, photo association"+(args.length>0?", supplied 7-point KMZ":""));
    }
}
