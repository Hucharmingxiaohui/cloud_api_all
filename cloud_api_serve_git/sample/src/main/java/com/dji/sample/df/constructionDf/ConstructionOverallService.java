package com.dji.sample.df.constructionDf;

import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.df.frogJumpDf.service.FrogJumpLandingRecommendationService;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.cloudapi.device.OsdDock;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.regex.Pattern;

/** A shared library route, with versioned point ownership. Never copies a KMZ. */
@Service
public class ConstructionOverallService {
    private final JdbcTemplate db;
    private final ConstructionAreaService areas;
    private final IWaylineFileService files;
    private final FrogJumpLandingRecommendationService docks;
    private final IDeviceRedisService redis;
    private final ObjectMapper json=new ObjectMapper();
    public ConstructionOverallService(JdbcTemplate db,ConstructionAreaService areas,IWaylineFileService files,FrogJumpLandingRecommendationService docks,IDeviceRedisService redis) {
        this.db=db;this.areas=areas;this.files=files;this.docks=docks;this.redis=redis;
    }
    public String encode(Object value) {try{return json.writeValueAsString(value);}catch(Exception e){throw bad("绑定数据无效");}}
    public Map<String,Object> decode(Object value) {try{return json.readValue(String.valueOf(value),new TypeReference<Map<String,Object>>(){});}catch(Exception e){throw bad("绑定快照无法读取");}}
    static RuleException bad(String message) {return new RuleException(400,message);}
    public static int integer(Object value,int min,int max) {
        if(!(value instanceof Number)) throw bad("请填写有效整数");
        double n=((Number)value).doubleValue();if(!Double.isFinite(n)||n!=Math.rint(n)||n<min||n>max) throw bad("数字超出范围");return (int)n;
    }
    private Map<String,Object> coverage(String w,String sn) {
        if(!docks.isWorkspaceDock(w,sn)) throw bad("机巢不属于当前工作空间");
        List<Map<String,Object>> rows=db.queryForList("SELECT radius_m FROM construction_dock_coverage WHERE workspace_id=? AND dock_sn=?",w,sn);
        if(rows.isEmpty()) throw bad("请先保存机巢覆盖范围");
        OsdDock osd=redis.getDeviceOsd(sn,OsdDock.class).orElseThrow(()->bad("机巢暂无定位，请刷新遥测"));
        if(osd.getLongitude()==null||osd.getLatitude()==null) throw bad("机巢定位无效");
        double lng=osd.getLongitude().doubleValue(),lat=osd.getLatitude().doubleValue();
        if(!Double.isFinite(lng)||!Double.isFinite(lat)||Math.abs(lng)>180||Math.abs(lat)>85||(lng==0&&lat==0)) throw bad("机巢定位无效");
        return Map.of("lng",lng,"lat",lat,"radius_m",ConstructionDockCoverageController.radius(rows.get(0).get("radius_m")));
    }
    private static double value(Map<String,Object> p,String key) {return ((Number)p.get(key)).doubleValue();}
    public static boolean inside(Map<String,Object> center,Map<String,Object> point) {
        double lat=Math.toRadians(value(center,"lat")),y=Math.toRadians(value(point,"lat"));
        double dx=Math.toRadians(value(point,"lng")-value(center,"lng"));
        double h=Math.pow(Math.sin((y-lat)/2),2)+Math.cos(lat)*Math.cos(y)*Math.pow(Math.sin(dx/2),2);
        return 2*6371008.8*Math.asin(Math.sqrt(Math.min(1,Math.max(0,h))))<=value(center,"radius_m")+0.01;
    }
    @SuppressWarnings("unchecked")
    private void checkArea(String w,Map<String,Object> binding,Map<String,Object> center) {
        Map<String,Object> area=areas.detail(w,(String)binding.get("area_id"));
        if(!Boolean.TRUE.equals(area.get("enabled"))||Boolean.TRUE.equals(area.get("archived"))) throw bad("绑定区域已暂停或归档："+area.get("name"));
        if(integer(binding.get("area_version"),1,Integer.MAX_VALUE)!=((Number)area.get("version")).intValue()) throw new RuleException(409,"区域版本变化，请重新确认航点绑定");
        List<Map<String,Object>> polygon=(List<Map<String,Object>>)area.get("boundary");
        if(polygon==null||polygon.size()<3||polygon.stream().anyMatch(p->!inside(center,p))) throw bad("机巢未完全覆盖区域："+area.get("name"));
        binding.put("area_name",area.get("name"));
    }
    public Map<String,Object> inspect(String w,String sn,String file) {
        var stored=files.getWaylineByWaylineId(w,file).orElseThrow(()->bad("当前工作空间未找到航线"));
        ConstructionKmz kmz;
        try {
            URLConnection c=files.getObjectUrl(w,file).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(15000);
            try(InputStream in=c.getInputStream()){kmz=ConstructionKmz.parse(ConstructionKmz.readBounded(in));}
        } catch(RuleException e){throw e;}catch(Exception e){throw bad("航线文件读取失败，请检查本地存储");}
        if(!kmz.pointPhotoOnly) throw bad("总体航点绑定暂不支持等时、等距或跨航点拍照，请改为到点拍照后再关联");
        Map<String,Object> center=coverage(w,sn);
        if(kmz.preview.stream().anyMatch(p->!inside(center,p))) throw bad("总体航线存在超出当前机巢覆盖范围的航点");
        List<Map<String,Object>> points=new ArrayList<>();
        for(int i=0;i<kmz.preview.size();i++) {
            Map<String,Object> p=new LinkedHashMap<>(kmz.preview.get(i));p.put("index",i+1);p.put("photo_suffixes",kmz.photoSuffixes.get(i+1));points.add(p);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("wayline_id",file);result.put("name",stored.getName());result.put("file_sign",stored.getSign());
        result.put("points",points);result.put("height_mode",kmz.heightMode);result.put("dock_sn",sn);result.put("coverage",center);return result;
    }
    @SuppressWarnings("unchecked")
    public static List<Map<String,Object>> bindings(Object raw,List<Map<String,Object>> points) {
        if(!(raw instanceof List)||((List<?>)raw).isEmpty()||((List<?>)raw).size()>2000) throw bad("请至少绑定一个区域和拍照航点");
        Set<String> areaIds=new HashSet<>();Set<Integer> used=new HashSet<>();List<Map<String,Object>> out=new ArrayList<>();
        Map<String,Set<Integer>> suffixOwners=new HashMap<>();
        for(Map<String,Object> p:points) for(String suffix:(List<String>)p.get("photo_suffixes")) suffixOwners.computeIfAbsent(suffix,k->new HashSet<>()).add(((Number)p.get("index")).intValue());
        for(Object item:(List<?>)raw) {
            if(!(item instanceof Map)) throw bad("区域绑定格式无效");Map<String,Object> b=(Map<String,Object>)item;
            if(!(b.get("area_id") instanceof String)||!areaIds.add((String)b.get("area_id"))) throw bad("区域重复或缺少区域ID");
            int version=integer(b.get("area_version"),1,Integer.MAX_VALUE);
            if(!(b.get("waypoints") instanceof List)||((List<?>)b.get("waypoints")).isEmpty()) throw bad("每个绑定区域至少选择一个拍照航点");
            List<Integer> indices=new ArrayList<>();
            for(Object rawIndex:(List<?>)b.get("waypoints")) {
                int i=integer(rawIndex,1,points.size());if(!used.add(i)) throw bad("一个航点只能绑定一个区域，航点重复："+i);
                List<String> names=(List<String>)points.get(i-1).get("photo_suffixes");
                if(names.isEmpty()||names.stream().anyMatch(s->suffixOwners.get(s).size()!=1)) throw bad("航点 "+i+" 缺少可唯一识别的到点拍照后缀，请先修改航线");
                indices.add(i);
            }
            Collections.sort(indices);Map<String,Object> copy=new LinkedHashMap<>();copy.put("area_id",b.get("area_id"));copy.put("area_version",version);copy.put("waypoints",indices);out.add(copy);
        }
        out.sort(Comparator.comparing(b->(String)b.get("area_id")));return out;
    }
    @Transactional(rollbackFor=Exception.class)
    @SuppressWarnings("unchecked")
    public Map<String,Object> save(String w,Map<String,Object> body) {
        String sn=String.valueOf(body.get("dock_sn")),file=String.valueOf(body.get("wayline_id"));
        int expected=integer(body.get("expected_version"),0,Integer.MAX_VALUE);
        Map<String,Object> snapshot=inspect(w,sn,file);
        if(!Objects.equals(body.get("file_sign"),snapshot.get("file_sign"))) throw new RuleException(409,"航线文件已变化，请重新加载后绑定");
        List<Map<String,Object>> links=bindings(body.get("bindings"),(List<Map<String,Object>>)snapshot.get("points"));
        for(Map<String,Object> b:links) {
            db.queryForList("SELECT id FROM construction_area WHERE workspace_id=? AND id=? FOR UPDATE",w,b.get("area_id"));
            checkArea(w,b,(Map<String,Object>)snapshot.get("coverage"));
        }
        List<Map<String,Object>> old=db.queryForList("SELECT * FROM construction_overall_route WHERE workspace_id=? AND dock_sn=? AND wayline_id=? FOR UPDATE",w,sn,file);
        int version=old.isEmpty()?0:((Number)old.get(0).get("version")).intValue();
        if(version!=expected) throw new RuleException(409,"总体航线绑定已更新，请重新加载");
        String id=old.isEmpty()?UUID.randomUUID().toString():(String)old.get(0).get("id");
        snapshot.put("overall_id",id);snapshot.put("version",version+1);snapshot.put("bindings",links);
        if(old.isEmpty()) db.update("INSERT INTO construction_overall_route (id,workspace_id,dock_sn,wayline_id,version,snapshot_json) VALUES (?,?,?,?,?,?)",id,w,sn,file,version+1,encode(snapshot));
        else db.update("UPDATE construction_overall_route SET version=?,snapshot_json=? WHERE workspace_id=? AND id=?",version+1,encode(snapshot),w,id);
        snapshot.put("status","VALID");snapshot.put("message","绑定已保存");return snapshot;
    }
    public Map<String,Object> stored(String w,String id) {
        List<Map<String,Object>> rows=db.queryForList("SELECT * FROM construction_overall_route WHERE workspace_id=? AND id=?",w,id);
        if(rows.isEmpty()) throw new RuleException(404,"总体航线不存在");return decode(rows.get(0).get("snapshot_json"));
    }
    @SuppressWarnings("unchecked")
    public void validate(String w,Map<String,Object> snapshot,boolean readFile) {
        Map<String,Object> current=stored(w,(String)snapshot.get("overall_id"));
        if(!Objects.equals(current.get("version"),snapshot.get("version"))) throw new RuleException(409,"航点绑定已变化，请重新创建总体计划");
        String file=(String)snapshot.get("wayline_id"),sn=(String)snapshot.get("dock_sn");
        var route=files.getWaylineByWaylineId(w,file).orElseThrow(()->bad("总体航线文件已删除"));
        if(!Objects.equals(route.getSign(),snapshot.get("file_sign"))) throw new RuleException(409,"航线文件已变化，请重新绑定航点");
        Map<String,Object> center=coverage(w,sn);
        for(Map<String,Object> b:(List<Map<String,Object>>)snapshot.get("bindings")) checkArea(w,new LinkedHashMap<>(b),center);
        for(Map<String,Object> p:(List<Map<String,Object>>)snapshot.get("points")) if(!inside(center,p)) throw bad("总体航线超出机巢当前覆盖范围");
        if(readFile) {
            Map<String,Object> actual=inspect(w,sn,file);
            if(!json.valueToTree(actual.get("points")).equals(json.valueToTree(snapshot.get("points")))) throw new RuleException(409,"执行航点或拍照后缀已改变，请重新绑定");
        }
    }
    public List<Map<String,Object>> list(String w,String sn) {
        if(!docks.isWorkspaceDock(w,sn)) throw bad("机巢不属于当前工作空间");
        List<Map<String,Object>> out=new ArrayList<>();
        for(Map<String,Object> row:db.queryForList("SELECT snapshot_json FROM construction_overall_route WHERE workspace_id=? AND dock_sn=? ORDER BY updated_at DESC",w,sn)) {
            Map<String,Object> s=decode(row.get("snapshot_json"));s.put("status","VALID");s.put("message","可创建总体计划");
            try{validate(w,s,false);}catch(RuleException e){s.put("status","STALE");s.put("message",e.getMessage());}out.add(s);
        }return out;
    }
    public Map<String,Object> jobSnapshot(String w,String job) {
        List<Map<String,Object>> rows=db.queryForList("SELECT snapshot_json FROM construction_plan_dispatch WHERE workspace_id=? AND job_id=? ORDER BY dispatch_order DESC LIMIT 1",w,job);
        if(rows.isEmpty()) return Map.of();Map<String,Object> dispatch=decode(rows.get(0).get("snapshot_json"));
        Map<String,Object> plan=decode(dispatch.get("snapshot_json"));Object overall=plan.get("overall_route");
        return overall instanceof Map?(Map<String,Object>)overall:Map.of();
    }
    @SuppressWarnings("unchecked")
    public static Map<String,Object> matchPhoto(Map<String,Object> snapshot,String name) {
        if(snapshot.isEmpty()) return Map.of();
        Set<Integer> matches=new HashSet<>();
        for(Map<String,Object> p:(List<Map<String,Object>>)snapshot.get("points")) for(String suffix:(List<String>)p.get("photo_suffixes")) {
            if(name!=null&&Pattern.compile("(?:^|_)"+Pattern.quote(suffix)+"(?=_|\\.|$)").matcher(name).find()) matches.add(((Number)p.get("index")).intValue());
        }
        if(matches.size()!=1) return Map.of("association","UNMATCHED","association_message",matches.isEmpty()?"照片名称未匹配拍照航点":"照片名称匹配多个航点");
        int index=matches.iterator().next();
        for(Map<String,Object> b:(List<Map<String,Object>>)snapshot.get("bindings")) if(((List<Number>)b.get("waypoints")).stream().anyMatch(n->n.intValue()==index)) return Map.of("association","MATCHED","waypoint_index",index,"area_id",b.get("area_id"),"area_name",b.get("area_name"));
        return Map.of("association","UNBOUND","waypoint_index",index,"association_message","该航点未绑定巡检区域");
    }
}
