package com.dji.sample.df.constructionDf;

import com.dji.sample.df.constructionDf.ConstructionAreaGeometry.RuleException;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@SuppressWarnings("unchecked")
public class ConstructionAnalysisConfigService {
    private final JdbcTemplate db;
    private final ConstructionAreaService areas;
    private final ConstructionWaylineService waylines;
    private final ConstructionOverallService overall;
    private final IWaylineFileService files;
    private final ObjectMapper json=new ObjectMapper();
    public ConstructionAnalysisConfigService(JdbcTemplate db,ConstructionAreaService areas,ConstructionWaylineService waylines,ConstructionOverallService overall,IWaylineFileService files){this.db=db;this.areas=areas;this.waylines=waylines;this.overall=overall;this.files=files;}
    static Map<String,Object> initial(){Map<String,Object> state=new LinkedHashMap<>();state.put("default",ConstructionAnalysisPolicy.defaults());state.put("areas",new LinkedHashMap<>());state.put("routes",new LinkedHashMap<>());return state;}
    private Map<String,Object> decode(Object raw){try{return json.readValue(String.valueOf(raw),Map.class);}catch(Exception e){throw new RuleException(500,"检测配置记录损坏，未使用默认值覆盖");}}
    private String encode(Object raw){try{return json.writeValueAsString(raw);}catch(Exception e){throw new RuleException(400,"检测配置无法序列化");}}
    private Map<String,Object> read(String w,boolean lock){
        List<Map<String,Object>> rows=db.queryForList("SELECT revision,config_json FROM construction_analysis_config WHERE workspace_id=?"+(lock?" FOR UPDATE":""),w);
        if(rows.isEmpty())return new LinkedHashMap<>(Map.of("revision",0,"state",initial()));
        return new LinkedHashMap<>(Map.of("revision",rows.get(0).get("revision"),"state",decode(rows.get(0).get("config_json"))));
    }
    private static Map<String,Object> object(Map<String,Object> map,String key){return (Map<String,Object>)map.get(key);}
    private static String key(String scope,String sign){return UUID.nameUUIDFromBytes((scope+"\n"+sign).getBytes(StandardCharsets.UTF_8)).toString();}
    private Map<String,Object> context(String w,String area,String file,String overallId){
        if(overallId!=null&&!overallId.isBlank()) {
            if(area!=null||file!=null)throw new RuleException(400,"总体航线与区域航线不能混用");
            Map<String,Object> route=overall.stored(w,overallId);
            // Validate the physical file revision, without depending on dock online state.
            String actual=files.getWaylineByWaylineId(w,(String)route.get("wayline_id")).orElseThrow(()->new RuleException(404,"航线文件不存在")).getSign();
            if(!Objects.equals(actual,route.get("file_sign")))throw new RuleException(409,"航线文件已变化，请重新校验总体绑定");
            Map<String,Object> ctx=new LinkedHashMap<>();ctx.put("route_key",key("overall:"+overallId,actual));ctx.put("file_sign",actual);ctx.put("route_version",route.get("version"));ctx.put("name",route.get("name"));
            Map<Integer,String> owners=new HashMap<>();Map<String,String> names=new HashMap<>();
            for(Map<String,Object> b:(List<Map<String,Object>>)route.get("bindings")){areas.detail(w,(String)b.get("area_id"));names.put((String)b.get("area_id"),(String)b.get("area_name"));for(Number n:(List<Number>)b.get("waypoints"))owners.put(n.intValue(),(String)b.get("area_id"));}
            List<Map<String,Object>> points=new ArrayList<>();for(Map<String,Object> p:(List<Map<String,Object>>)route.get("points")){Map<String,Object> point=new LinkedHashMap<>(p);String a=owners.get(((Number)p.get("index")).intValue());point.put("area_id",a);point.put("area_name",names.get(a));points.add(point);}ctx.put("points",points);return ctx;
        }
        Map<String,Object> ctx=new LinkedHashMap<>();
        if(area!=null&&!area.isBlank()) {Map<String,Object> a=areas.detail(w,area);ctx.put("area_id",area);ctx.put("area_name",a.get("name"));}
        if(file==null||file.isBlank())return ctx;
        if(!ctx.containsKey("area_id"))throw new RuleException(400,"区域航线须指定所属工作面");
        Map<String,Object> route=waylines.detail(w,area,file);
        var stored=files.getWaylineByWaylineId(w,file).orElseThrow(()->new RuleException(404,"航线文件不存在"));
        ConstructionKmz kmz;
        try {URLConnection connection=files.getObjectUrl(w,file).openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(30000);byte[] bytes;try(InputStream in=connection.getInputStream()){bytes=ConstructionKmz.readBounded(in);}if(!stored.getSign().equals(DigestUtils.md5DigestAsHex(bytes)))throw new RuleException(409,"航线文件内容已变化");kmz=ConstructionKmz.parse(bytes);}
        catch(RuleException e){throw e;}catch(Exception e){throw new RuleException(400,"无法解析航线，请检查航线文件");}
        ctx.put("route_key",key("area:"+area+":file:"+file,stored.getSign()));ctx.put("file_sign",stored.getSign());ctx.put("route_version",route.get("area_version"));ctx.put("name",route.get("name"));
        List<Map<String,Object>> points=new ArrayList<>();for(int i=0;i<kmz.preview.size();i++){Map<String,Object> p=new LinkedHashMap<>(kmz.preview.get(i));p.put("index",i+1);p.put("photo_suffixes",kmz.photoSuffixes.getOrDefault(i+1,List.of()));p.put("area_id",area);p.put("area_name",ctx.get("area_name"));points.add(p);}ctx.put("points",points);return ctx;
    }
    static Map<String,Object> resolved(Map<String,Object> state,String area,Object override){
        Object regional=area==null?null:object(state,"areas").get(area);
        Map<String,Object> out=new LinkedHashMap<>();out.put("override",override);
        out.put("inherited",regional!=null?regional:state.get("default"));out.put("inherited_source",regional!=null?"area":"default");
        out.put("effective",override!=null?override:regional!=null?regional:state.get("default"));out.put("source",override!=null?"waypoint":regional!=null?"area":"default");return out;
    }
    private Map<String,Object> view(Map<String,Object> stored,Map<String,Object> context){
        Map<String,Object> state=object(stored,"state");Map<String,Object> out=new LinkedHashMap<>(context);
        out.put("revision",stored.get("revision"));out.put("catalog",ConstructionAnalysisPolicy.catalog());out.put("default_policy",state.get("default"));
        if(context.containsKey("area_id")){String area=(String)context.get("area_id");Map<String,Object> a=resolved(state,area,null);a.put("override",object(state,"areas").get(area));a.put("inherited",state.get("default"));a.put("inherited_source","default");out.put("area",a);}
        if(context.containsKey("route_key")) {
            Map<String,Object> overrides=(Map<String,Object>)object(state,"routes").getOrDefault(context.get("route_key"),Map.of());
            List<Map<String,Object>> points=new ArrayList<>();for(Map<String,Object> p:(List<Map<String,Object>>)context.get("points")){Map<String,Object> item=new LinkedHashMap<>(p);item.putAll(resolved(state,(String)p.get("area_id"),overrides.get(String.valueOf(p.get("index")))));points.add(item);}out.put("points",points);
        }
        return out;
    }
    public Map<String,Object> get(String w,String area,String file,String overallId){Map<String,Object> ctx=context(w,area,file,overallId);return view(read(w,false),ctx);}
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> save(String w,String area,String file,String overallId,Map<String,Object> body){
        Map<String,Object> ctx=context(w,area,file,overallId);
        db.update("INSERT IGNORE INTO construction_analysis_config (workspace_id,revision,config_json) VALUES (?,0,?)",w,encode(initial()));
        Map<String,Object> row=read(w,true);Object expected=body.get("expected_revision");
        if(!(expected instanceof Number)||((Number)expected).doubleValue()!=((Number)row.get("revision")).doubleValue())throw new RuleException(409,"检测配置已被修改，请重新加载后保存");
        if(!body.containsKey("policy"))throw new RuleException(400,"缺少检测配置；恢复继承请传null");
        Object policy=body.get("policy")==null?null:ConstructionAnalysisPolicy.validate(body.get("policy"));
        Map<String,Object> state=object(row,"state");String target=String.valueOf(body.get("target"));
        if("default".equals(target)){if(!ctx.isEmpty()||policy==null)throw new RuleException(400,"默认配置不能继承其他层级");state.put("default",policy);}
        else if("area".equals(target)){if(!ctx.containsKey("area_id")||ctx.containsKey("route_key"))throw new RuleException(400,"请选择工作面配置入口");if(policy==null)object(state,"areas").remove(area);else object(state,"areas").put(area,policy);}
        else if("waypoint".equals(target)){
            if(!ctx.containsKey("route_key")||!Objects.equals(ctx.get("file_sign"),body.get("file_sign"))||!Objects.equals(String.valueOf(ctx.get("route_version")),String.valueOf(body.get("route_version"))))throw new RuleException(409,"航线或绑定版本已变化，请重新加载");
            Object index=body.get("waypoint_index");Map<String,Object> point=null;
            if(index instanceof Number&&((Number)index).doubleValue()==((Number)index).intValue())for(Map<String,Object> p:(List<Map<String,Object>>)ctx.get("points"))if(((Number)p.get("index")).intValue()==((Number)index).intValue())point=p;
            if(point==null||point.get("area_id")==null)throw new RuleException(400,"只能配置已绑定工作面的实际航点");
            Map<String,Object> routes=object(state,"routes");String routeKey=(String)ctx.get("route_key");Map<String,Object> overrides=(Map<String,Object>)routes.computeIfAbsent(routeKey,k->new LinkedHashMap<>());
            String pointKey=Integer.toString(((Number)index).intValue());
            if(policy==null)overrides.remove(pointKey);else overrides.put(pointKey,policy);
        }else throw new RuleException(400,"检测配置层级无效");
        String encoded=encode(state);if(encoded.length()>4000000)throw new RuleException(400,"检测配置数量超过当前容量");
        int revision=((Number)row.get("revision")).intValue()+1;
        db.update("UPDATE construction_analysis_config SET revision=?,config_json=?,updated_at=CURRENT_TIMESTAMP WHERE workspace_id=?",revision,encoded,w);
        row.put("revision",revision);return view(row,ctx);
    }
    /** Called only when creating a new plan. Dispatch/history never re-read live settings. */
    public Map<String,Object> freeze(String w,String area,String file,Map<String,Object> overallRoute){
        Map<String,Object> ctx=context(w,overallRoute==null?area:null,overallRoute==null?file:null,overallRoute==null?null:(String)overallRoute.get("overall_id"));
        Map<String,Object> snapshot=view(read(w,false),ctx);snapshot.remove("catalog");snapshot.put("schema_version",1);return snapshot;
    }
}
