package com.dji.sample.df.constructionDf;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Proxy;
import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.df.frogJumpDf.service.FrogJumpLandingRecommendationService;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.cloudapi.device.OsdDock;
import com.dji.sdk.cloudapi.wayline.GetWaylineListResponse;
import org.springframework.jdbc.core.JdbcTemplate;

public class ConstructionOverallChecks {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void reject(Runnable call){try{call.run();}catch(RuleException e){return;}throw new AssertionError("Expected rejection");}
    static Map<String,Object> point(int i,String suffix){return new LinkedHashMap<>(Map.of("index",i,"lng",120.0+i*.001,"lat",36.0,"height",90.0,"photo_suffixes",suffix.isEmpty()?List.of():List.of(suffix)));}
    static Map<String,Object> binding(String id,int... indices){return Map.of("area_id",id,"area_version",1,"waypoints",Arrays.stream(indices).boxed().toList());}
    static byte[] fixture(String trigger) throws Exception {
        String mission="<wpml:missionConfig><wpml:droneInfo><wpml:droneEnumValue>77</wpml:droneEnumValue><wpml:droneSubEnumValue>0</wpml:droneSubEnumValue></wpml:droneInfo><wpml:payloadInfo><wpml:payloadEnumValue>66</wpml:payloadEnumValue></wpml:payloadInfo></wpml:missionConfig>";
        String points="";
        for(int i=0;i<3;i++) points+="<Placemark><Point><coordinates>"+(120+i*.001)+",36</coordinates></Point><wpml:index>"+i+"</wpml:index><wpml:executeHeight>90</wpml:executeHeight><wpml:waypointSpeed>5</wpml:waypointSpeed><wpml:waypointTurnParam><wpml:waypointTurnMode>toPointAndStopWithDiscontinuityCurvature</wpml:waypointTurnMode></wpml:waypointTurnParam><wpml:actionGroup><wpml:actionGroupStartIndex>"+i+"</wpml:actionGroupStartIndex><wpml:actionGroupEndIndex>"+i+"</wpml:actionGroupEndIndex><wpml:actionTrigger><wpml:actionTriggerType>"+trigger+"</wpml:actionTriggerType></wpml:actionTrigger><wpml:action><wpml:actionActuatorFunc>takePhoto</wpml:actionActuatorFunc><wpml:actionActuatorFuncParam><wpml:fileSuffix>航点"+(i+1)+"</wpml:fileSuffix></wpml:actionActuatorFuncParam></wpml:action></wpml:actionGroup></Placemark>";
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){
            for(String file:List.of("template.kml","waylines.wpml")) {
                zip.putNextEntry(new ZipEntry("wpmz/"+file));String xml="<kml xmlns='http://www.opengis.net/kml/2.2' xmlns:wpml='http://www.dji.com/wpmz/1.0.6'><Document>"+mission+"<Folder><wpml:templateType>waypoint</wpml:templateType><wpml:executeHeightMode>WGS84</wpml:executeHeightMode>"+points+"</Folder></Document></kml>";zip.write(xml.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            }
        }return out.toByteArray();
    }
    public static void main(String[] args) throws Exception {
        generatedSuffixes();
        ConstructionKmz kmz=ConstructionKmz.parse(fixture("reachPoint"));check(kmz.photoSuffixes.get(2).equals(List.of("航点2")),"actual WPML suffix");
        check(!ConstructionKmz.parse(fixture("multipleTiming")).pointPhotoOnly,"interval cannot be mapped as one point");
        List<Map<String,Object>> points=List.of(point(1,"航点1"),point(2,"航点2"),point(3,""));
        List<Map<String,Object>> b=ConstructionOverallService.bindings(List.of(binding("a",1,2)),points);
        check(b.size()==1&&((List<?>)b.get(0).get("waypoints")).size()==2,"one region many points");
        reject(()->ConstructionOverallService.bindings(List.of(binding("a",1),binding("b",1)),points));
        reject(()->ConstructionOverallService.bindings(List.of(binding("a",3)),points));
        reject(()->ConstructionOverallService.bindings(List.of(binding("a",4)),points));
        reject(()->ConstructionOverallService.bindings(List.of(binding("a",1)),List.of(point(1,"same"),point(2,"same"))));
        reject(()->ConstructionOverallService.bindings(List.of(),points));
        Map<String,Object> center=Map.of("lng",120.0,"lat",36.0,"radius_m",2000);
        check(ConstructionOverallService.inside(center,points.get(0)),"near point");check(!ConstructionOverallService.inside(center,Map.of("lng",121.0,"lat",36.0)),"far point");
        Map<String,Map<String,Object>> stored=new HashMap<>();Map<String,Object> dispatch=new HashMap<>();
        JdbcTemplate db=new JdbcTemplate(){
            @Override public List<Map<String,Object>> queryForList(String sql,Object... a){
                if(sql.contains("construction_dock_coverage")) return List.of(Map.of("radius_m",2000));
                if(sql.contains("FROM construction_area ")) return List.of(Map.of("id",a[1]));
                if(sql.contains("construction_plan_dispatch"))return dispatch.isEmpty()?List.of():List.of(dispatch);
                if(sql.contains("construction_overall_route"))return stored.values().stream().filter(r->Objects.equals(a[0],r.get("workspace_id"))&&(sql.contains("dock_sn=?")?Objects.equals(a[1],r.get("dock_sn")):Objects.equals(a[1],r.get("id")))).toList();
                throw new AssertionError(sql);
            }
            @Override public int update(String sql,Object... a){
                if(sql.startsWith("INSERT INTO construction_overall_route")){stored.put((String)a[0],new HashMap<>(Map.of("id",a[0],"workspace_id",a[1],"dock_sn",a[2],"wayline_id",a[3],"version",a[4],"snapshot_json",a[5])));return 1;}
                if(sql.startsWith("UPDATE construction_overall_route")){stored.get(a[3]).put("version",a[0]);stored.get(a[3]).put("snapshot_json",a[1]);return 1;}throw new AssertionError(sql);
            }
        };
        Map<String,Object> area=new HashMap<>(Map.of("name","一区","version",1,"enabled",true,"archived",false,"boundary",List.of(Map.of("lng",120.0,"lat",36.0),Map.of("lng",120.001,"lat",36.0),Map.of("lng",120.0,"lat",36.001))));
        ConstructionAreaService areas=new ConstructionAreaService(null,null){@Override public Map<String,Object> detail(String w,String id){if(!"w".equals(w)||!List.of("a","b").contains(id))throw new RuleException(404,"scope");return area;}};
        FrogJumpLandingRecommendationService docks=new FrogJumpLandingRecommendationService(null,null,null){@Override public boolean isWorkspaceDock(String w,String sn){return "w".equals(w)&&"dock".equals(sn);}};
        OsdDock osd=new OsdDock();osd.setLongitude(120.0f);osd.setLatitude(36.0f);
        IDeviceRedisService redis=(IDeviceRedisService)Proxy.newProxyInstance(ConstructionOverallChecks.class.getClassLoader(),new Class[]{IDeviceRedisService.class},(p,m,a)->Optional.of(osd));
        GetWaylineListResponse file=new GetWaylineListResponse();file.setSign("sign");
        IWaylineFileService files=(IWaylineFileService)Proxy.newProxyInstance(ConstructionOverallChecks.class.getClassLoader(),new Class[]{IWaylineFileService.class},(p,m,a)->Optional.of(file));
        ConstructionOverallService service=new ConstructionOverallService(db,areas,files,docks,redis){@Override public Map<String,Object> inspect(String w,String sn,String id){if(!"w".equals(w)||!"dock".equals(sn))throw new RuleException(404,"scope");return new LinkedHashMap<>(Map.of("wayline_id",id,"name","Route","dock_sn",sn,"file_sign","sign","height_mode","WGS84","points",points,"coverage",center));}};
        Map<String,Object> body=new HashMap<>(Map.of("dock_sn","dock","wayline_id","file","file_sign","sign","expected_version",0,"bindings",List.of(binding("a",1),binding("b",2))));
        Map<String,Object> saved=service.save("w",body);String id=(String)saved.get("overall_id");check(stored.size()==1,"one shared route");
        Map<String,Object> snapshot=service.stored("w",id);service.validate("w",snapshot,true);
        reject(()->service.stored("foreign",id));reject(()->service.save("w",body));
        area.put("version",2);reject(()->service.validate("w",snapshot,false));area.put("version",1);
        area.put("enabled",false);reject(()->service.validate("w",snapshot,false));area.put("enabled",true);
        file.setSign("changed");reject(()->service.validate("w",snapshot,false));file.setSign("sign");
        osd.setLongitude(121.0f);reject(()->service.validate("w",snapshot,false));osd.setLongitude(120.0f);
        Map<String,Object> originalPhoto=ConstructionOverallService.matchPhoto(snapshot,"DJI_test_V_航点1.jpg");check("a".equals(originalPhoto.get("area_id")),"visible photo maps region");
        check("b".equals(ConstructionOverallService.matchPhoto(snapshot,"DJI_test_T_航点2.jpg").get("area_id")),"infrared maps same waypoint");
        check("UNMATCHED".equals(ConstructionOverallService.matchPhoto(snapshot,"DJI_test_V_航点10.jpg").get("association")),"no prefix collision 1 versus 10");
        check("UNMATCHED".equals(ConstructionOverallService.matchPhoto(snapshot,"DJI_test.jpg").get("association")),"unknown stays unmatched");
        check("UNMATCHED".equals(ConstructionOverallService.matchPhoto(snapshot,"DJI_航点1_航点2.jpg").get("association")),"ambiguous stays unmatched");
        dispatch.put("snapshot_json",service.encode(Map.of("snapshot_json",service.encode(Map.of("overall_route",snapshot)))));
        body.put("expected_version",1);body.put("bindings",List.of(binding("b",1,2)));service.save("w",body);
        reject(()->service.validate("w",snapshot,false));
        check("a".equals(ConstructionOverallService.matchPhoto(service.jobSnapshot("w","job"),"DJI_test_V_航点1.jpg").get("area_id")),"historical job frozen despite reassignment");
        System.out.println("Overall route checks passed: actual suffix parsing, multiple points per region, duplicate/invalid binding rejection, scope, version/sign, dock movement, photo ambiguity and frozen execution snapshot. In-memory collaborators.");
    }
    static void generatedSuffixes() throws Exception {
        Map<String,Object> mission=Map.of("fileName","suffix-test","finishAction","goHome","takeOffSecurityHeight",20,"globalTransitionalSpeed",4,"droneInfo",Map.of("droneEnumValue",77,"droneSubEnumValue",0),"payloadInfo",Map.of("payloadEnumValue",66,"payloadSubEnumValue",0,"payloadPositionIndex",0));
        List<Map<String,Object>> points=new ArrayList<>();
        for(int i=0;i<12;i++) {
            String action=i%2==0?"takePhoto":"orientedShoot";
            Map<String,Object> params=Map.of(action,Map.of(action.equals("takePhoto")?"fileSuffix":"orientedFileSuffix","WP"));
            points.add(Map.of("point",Map.of("coordinates",(120+i*.0001)+",36"),"useGlobalHeight",1,"useGlobalSpeed",1,"useGlobalHeadingParam",1,"useGlobalTurnParam",1,"actionGroup",Map.of("actionTrigger",Map.of("actionTriggerType","reachPoint"),"actionList",List.of(Map.of("actionActuatorFunc",action,"actionActuatorFuncParam",params)))));
        }
        Map<String,Object> folder=Map.of("autoFlightSpeed",5,"globalHeight",90,"waylineCoordinateSysParam",Map.of(),"globalWaypointHeadingParam",Map.of("waypointHeadingMode","followWayline"),"payloadParam",Map.of("payloadPositionIndex",0,"imageFormat","wide,zoom"),"placeMarks",points);
        var route=new com.fasterxml.jackson.databind.ObjectMapper().convertValue(Map.of("missionConfig",mission,"folder",folder),com.dji.sample.df.kmzDf.entity.wayline.Wayline.class);
        java.nio.file.Path temp=java.nio.file.Files.createTempDirectory("overall-suffix-test-");
        try {
            java.nio.file.Files.createDirectories(temp.resolve("wpmz"));
            byte[] bytes=java.nio.file.Files.readAllBytes(java.nio.file.Path.of(com.dji.sample.df.kmzDf.utils.CreateKmz.craeteKmz(route,temp.toString())));
            ConstructionKmz parsed=ConstructionKmz.parse(bytes);
            for(int i=1;i<=12;i++)check(parsed.photoSuffixes.get(i).equals(List.of("WP"+i)),"generated suffix at point "+i);
            try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
                for(ZipEntry e;(e=zip.getNextEntry())!=null;) if(e.getName().endsWith("template.kml")) {
                    String xml=new String(zip.readAllBytes(),StandardCharsets.UTF_8);
                    check(xml.contains("<wpml:fileSuffix>WP2</wpml:fileSuffix>")&&xml.contains("<wpml:fileSuffix>WP12</wpml:fileSuffix>"),"template and execution naming agree");
                }
            }
        } finally {try(var walk=java.nio.file.Files.walk(temp)){for(var p:walk.sorted(Comparator.reverseOrder()).toList())java.nio.file.Files.deleteIfExists(p);}}
        System.out.println("Generated takePhoto/orientedShoot suffixes passed: points 1-12, template and execution agree.");
    }
}
