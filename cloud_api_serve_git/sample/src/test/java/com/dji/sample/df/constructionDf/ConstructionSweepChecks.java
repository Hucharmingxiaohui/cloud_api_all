package com.dji.sample.df.constructionDf;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dji.sample.df.kmzDf.entity.wayline.Wayline;
import com.dji.sample.df.kmzDf.utils.CreateKmz;
public class ConstructionSweepChecks {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        Map<String,Object> mission=Map.of("fileName","sweep","finishAction","goHome","takeOffSecurityHeight",20,"globalTransitionalSpeed",5,"droneInfo",Map.of("droneEnumValue",77,"droneSubEnumValue",0),"payloadInfo",Map.of("payloadEnumValue",66,"payloadSubEnumValue",0,"payloadPositionIndex",0));
        List<Map<String,Object>> points=new ArrayList<>();
        for(int i=0;i<3;i++){
            List<Object> actions=new ArrayList<>();
            actions.add(Map.of("actionActuatorFunc","gimbalRotate","actionActuatorFuncParam",Map.of("gimbalRotate",Map.of("gimbalPitchRotateEnable",1,"gimbalPitchRotateAngle",-90,"payloadPositionIndex",0))));
            if(i!=1)actions.add(Map.of("actionActuatorFunc","takePhoto","actionActuatorFuncParam",Map.of("takePhoto",Map.of("fileSuffix","航点","payloadPositionIndex",0,"useGlobalPayloadLensIndex",0,"payloadLensIndex","wide,ir"))));
            points.add(Map.of("point",Map.of("coordinates",(120+i*.001)+",36"),"useGlobalHeight",1,"useGlobalSpeed",1,"useGlobalHeadingParam",1,"useGlobalTurnParam",1,"gimbalPitchAngle",-90,"actionGroup",Map.of("actionGroupMode","sequence","actionTrigger",Map.of("actionTriggerType","reachPoint","actionTriggerParam",0),"actionList",actions)));
        }
        Map<String,Object> folder=Map.of("executeHeightMode","WGS84","globalHeight",130,"autoFlightSpeed",5,"gimbalPitchMode","usePointSetting","globalWaypointTurnMode","toPointAndStopWithDiscontinuityCurvature","globalWaypointHeadingParam",Map.of("waypointHeadingMode","followWayline"),"waylineCoordinateSysParam",Map.of("coordinateMode","WGS84","heightMode","WGS84","positioningType","GPS"),"payloadParam",Map.of("payloadPositionIndex",0,"imageFormat","wide,ir"),"placeMarks",points);
        ObjectMapper json=new ObjectMapper();Wayline wayline=json.convertValue(Map.of("missionConfig",mission,"folder",folder),Wayline.class);
        if(args.length>0)wayline=json.convertValue(json.readValue(Files.readAllBytes(Path.of(args[0])),Map.class).get("wayline"),Wayline.class);
        Path temp=Files.createTempDirectory("sweep-kmz-check-");
        try {
            Files.createDirectories(temp.resolve("wpmz"));byte[] bytes=Files.readAllBytes(Path.of(CreateKmz.craeteKmz(wayline,temp.toString())));
            ConstructionKmz kmz=ConstructionKmz.parse(bytes);check(kmz.pointPhotoOnly,"point-triggered photos");
            if(args.length==0){check("WGS84".equals(kmz.heightMode),"absolute WGS84 height mode");check(kmz.points.size()==3,"three points");check(kmz.photoSuffixes.getOrDefault(2,List.of()).isEmpty(),"transfer must not take photos");check(kmz.photoSuffixes.get(1).contains("航点1")&&kmz.photoSuffixes.get(3).contains("航点3"),"photo naming");}
            try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(bytes))){for(ZipEntry e;(e=in.getNextEntry())!=null;){String xml=new String(in.readAllBytes(),StandardCharsets.UTF_8);if(e.getName().endsWith("waylines.wpml")){check(!xml.contains("<wpml:gimbalPitchRotateAngle>0</wpml:gimbalPitchRotateAngle>"),"explicit pitch must not be reset to zero");check(xml.indexOf("<wpml:actionActuatorFunc>gimbalRotate")<xml.indexOf("<wpml:actionActuatorFunc>takePhoto"),"pitch set before capture");check(xml.contains("<wpml:gimbalPitchRotateAngle>-90.0</wpml:gimbalPitchRotateAngle>"),"nadir pitch preserved");check(xml.contains("followWayline"),"heading follows route");}}}
            System.out.println("Sweep KMZ checks passed: explicit pitch before capture, no zero reset, photo suffixes, transit without capture, heading and parser compatibility.");
        } finally {try(java.util.stream.Stream<Path> files=Files.walk(temp)){for(Path p:files.sorted(Comparator.reverseOrder()).toArray(Path[]::new))Files.deleteIfExists(p);}}
    }
}
